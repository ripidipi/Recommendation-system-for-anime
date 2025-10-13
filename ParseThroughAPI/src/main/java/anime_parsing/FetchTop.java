package anime_parsing;

import exeptions.HttpRequestException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.*;


public class FetchTop {

    private static final String BASE = "https://api.jikan.moe/v4";
    private static final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .version(HttpClient.Version.HTTP_1_1)
            .build();
    private static final ObjectMapper mapper = new ObjectMapper()
            .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
            .findAndRegisterModules();

    // TODO reduce complexity
    public static void fetchAndPersistAnime(int numberOfPages)  {
        final int POOL_SIZE = 7;
        final int MAX_RETRIES = 5;
        final long BETWEEN_TASK_SLEEP_MS = 1500;
        final long INTERVAL_MS = 1000;
        final long MAX_TASK_MS = 500;

        ExecutorService executor = Executors.newFixedThreadPool(POOL_SIZE);
        CompletionService<Integer> cs = new ExecutorCompletionService<>(executor);

        int submitted = 0;

        try {
            for (int page = 1; page <= numberOfPages; page++) {
                final int currentPage = page;
                cs.submit(() -> {
                    for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
                        try {
                            AnimeTopResult res = fetchTopAnimePage(currentPage);
                            if (res != null && res.data != null) {
                                for (var anime : res.data) {
                                    try {
                                        Parser.saveAnimeToDB(anime);
                                    } catch (Exception e) {
                                        System.out.println("Error saving anime (page " + currentPage + "): "
                                                + e.getMessage());
                                        e.printStackTrace();
                                    }
                                }
                            }
                            return currentPage;
                        } catch (Exception e) {
                            System.out.println("Fetch page " + currentPage + " failed (attempt " + attempt + "): "
                                    + e.getMessage());
                            if (attempt == MAX_RETRIES) {
                                throw e;
                            } else {
                                try {
                                    Thread.sleep(BETWEEN_TASK_SLEEP_MS * attempt);
                                } catch (InterruptedException ie) {
                                    Thread.currentThread().interrupt();
                                    throw ie;
                                }
                            }
                        }
                    }
                    return currentPage;
                });
                submitted++;
                try { Thread.sleep(INTERVAL_MS); } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt(); break;
                }
            }

            int completed = 0;
            for (int i = 0; i < submitted; i++) {
                try {
                    Future<Integer> f = cs.poll(MAX_TASK_MS, TimeUnit.SECONDS);
                    if (f == null) {
                        System.out.println("No completed futures in " + MAX_TASK_MS + "s");
                        f = cs.poll(10, TimeUnit.SECONDS);
                        if (f == null) {
                            continue;
                        }
                    }
                    try {
                        Integer page = f.get(5, TimeUnit.SECONDS);
                        completed++;
                        if (completed % 50 == 0 || page % 100 == 0) {
                            System.out.println("Completed pages: " + completed + "/" + submitted + " (last page: " + page + ")");
                        }
                    } catch (ExecutionException ee) {
                        System.out.println("Task failed: " + ee.getCause());
                    } catch (CancellationException ce) {
                        System.out.println("Task cancelled");
                    } catch (TimeoutException te) {
                        System.out.println("Unexpected timeout getting future result");
                    }
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    System.out.println("Collector interrupted");
                    break;
                }
            }

            System.out.println("Total submitted pages: " + submitted + ", completed (successful tasks observed): " + completed);
        } finally {
            executor.shutdownNow();
            try {
                if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                    System.out.println("Executor didn't stop in time, forcing shutdown");
                    executor.shutdownNow();
                }
            } catch (InterruptedException ie) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    public static int countPages() {
        if (!pageExists(1)) return 0;

        int lo = 1;
        int hi = 1;
        while (pageExists(hi)) {
            lo = hi;
            hi = hi * 2;
            if (hi > 1_000_000) {
                hi = 1_000_000;
                break;
            }
            try {
                Thread.sleep(1000);
            } catch (Exception e) {
                System.out.println("Error sleeping: " + e.getMessage());
            }
        }
        int l = lo;
        int r = Math.max(hi, lo + 1) - 1;
        while (l < r) {
            int mid = l + (r - l + 1) / 2;
            if (pageExists(mid)) {
                l = mid;
            } else {
                r = mid - 1;
            }
            try {
                Thread.sleep(1000);
            } catch (Exception e) {
                System.out.println("Error sleeping: " + e.getMessage());
            }
        }
        return l;
    }

    private static boolean pageExists(int page) {
        try {
            AnimeTopResult res = fetchTopAnimePage(page);
            return res != null && res.data != null && !res.data.isEmpty();
        } catch (IOException | InterruptedException e) {
            System.err.println("pageExists(" + page + ") failed: " + e.getMessage());
            return false;
        } catch (RuntimeException e) {
            System.err.println("pageExists(" + page + ") runtime error: " + e.getMessage());
            return false;
        }
    }


    private static AnimeTopResult fetchTopAnimePage(int page) throws IOException, InterruptedException {
        String url = String.format("%s/top/anime?page=%d", BASE, page);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new HttpRequestException("HTTP " + response.statusCode());
        }
        return mapper.readValue(response.body(), AnimeTopResult.class);
    }
}
