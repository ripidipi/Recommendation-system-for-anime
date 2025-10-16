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

    private static String BASE = "https://api.jikan.moe/v4";
    private static final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .version(HttpClient.Version.HTTP_1_1)
            .build();
    private static final ObjectMapper mapper = new ObjectMapper()
            .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
            .findAndRegisterModules();

    public static void fetchAndPersistAnime(int numberOfPages) {

        final int POOL_SIZE = 7;
        final int MAX_RETRIES = 5;
        final long RETRY_BASE_SLEEP_MS = 1500L;
        final long SUBMIT_INTERVAL_MS = 1000L;
        final long TASK_GET_TIMEOUT_SEC = 5L;
        final long AWAIT_TERMINATION_SEC = 30L;

        ExecutorService executor = Executors.newFixedThreadPool(POOL_SIZE);
        CompletionService<Integer> cs = new ExecutorCompletionService<>(executor);

        int submitted = 0;
        try {
            submitted = submitPages(cs, numberOfPages, SUBMIT_INTERVAL_MS, MAX_RETRIES, RETRY_BASE_SLEEP_MS);

            int completed = collectResults(cs, submitted, TASK_GET_TIMEOUT_SEC);

            System.out.println("Total submitted pages: " + submitted + ", completed (successful tasks observed): " + completed);
        } finally {
            shutdownExecutor(executor, AWAIT_TERMINATION_SEC);
        }
    }

    private static int submitPages(CompletionService<Integer> cs,
                                   int numberOfPages,
                                   long submitIntervalMs,
                                   int maxRetries,
                                   long retryBaseSleepMs) {
        int submitted = 0;
        for (int page = 1; page <= numberOfPages; page++) {
            final int currentPage = page;
            cs.submit(() -> fetchAndSavePageWithRetries(currentPage, maxRetries, retryBaseSleepMs));
            submitted++;
            try {
                Thread.sleep(submitIntervalMs);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                System.out.println("Submit loop interrupted, stopping submissions at page " + currentPage);
                break;
            }
        }
        return submitted;
    }

    private static Integer fetchAndSavePageWithRetries(int page, int maxRetries, long retryBaseSleepMs) throws Exception {
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                AnimeTopResult res = fetchTopAnimePage(page);
                if (res != null && res.data != null) {
                    for (var anime : res.data) {
                        try {
                            Parser.saveAnimeToDB(anime);
                        } catch (Exception e) {
                            System.out.println("Error saving anime (page " + page + "): " + e.getMessage());
                            e.printStackTrace();
                        }
                    }
                }
                return page;
            } catch (Exception e) {
                System.out.println("Fetch page " + page + " failed (attempt " + attempt + "): " + e.getMessage());
                if (attempt == maxRetries) {
                    throw e;
                } else {
                    try {
                        long sleepMs = retryBaseSleepMs * attempt;
                        TimeUnit.MILLISECONDS.sleep(sleepMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw ie;
                    }
                }
            }
        }
        return page;
    }

    private static int collectResults(CompletionService<Integer> cs, int submitted, long taskGetTimeoutSec) {
        int completed = 0;
        for (int i = 0; i < submitted; i++) {
            try {
                Future<Integer> f = cs.take();
                try {
                    Integer page = f.get(taskGetTimeoutSec, TimeUnit.SECONDS);
                    completed++;
                    if (completed % 50 == 0 || (page != null && page % 100 == 0)) {
                        System.out.println("Completed pages: " + completed + "/" + submitted + " (last page: " + page + ")");
                    }
                } catch (ExecutionException ee) {
                    System.out.println("Task failed with exception: " + ee.getCause());
                } catch (CancellationException ce) {
                    System.out.println("Task cancelled.");
                } catch (TimeoutException te) {
                    System.out.println("Timed out waiting for task result.");
                }
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                System.out.println("Collector interrupted, stopping collection.");
                break;
            }
        }
        return completed;
    }

    private static void shutdownExecutor(ExecutorService executor, long awaitTerminationSec) {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(awaitTerminationSec, TimeUnit.SECONDS)) {
                System.out.println("Executor didn't stop in " + awaitTerminationSec + "s, forcing shutdownNow()");
                executor.shutdownNow();
            }
        } catch (InterruptedException ie) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
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

    public static void setBASE(String base) {
        FetchTop.BASE = base;
    }
}
