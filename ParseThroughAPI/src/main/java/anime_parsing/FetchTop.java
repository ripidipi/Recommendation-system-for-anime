package anime_parsing;

import exeptions.HttpRequestException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.*;

public class FetchTop {

    public static final int DEFAULT_POOL_SIZE = 7;
    public static final long DEFAULT_SUBMIT_INTERVAL_MS = 1_500L;
    public static final long DEFAULT_COUNT_SUBMIT_INTERVAL_MS = 2_000L;
    public static final int DEFAULT_MAX_ATTEMPTS = 5;
    public static final long DEFAULT_TASK_GET_TIMEOUT_SEC = 5L;
    public static final long DEFAULT_AWAIT_TERMINATION_SEC = 30L;
    public static final String DEFAULT_BASE_URL = "https://api.jikan.moe/v4";

    private static final Logger LOGGER = LoggerFactory.getLogger(FetchTop.class);

    private final long submitIntervalMs;
    private final long countSubmitIntervalMs;
    private final int maxAttempts;
    private final long taskGetTimeoutSec;
    private final long awaitTerminationSec;
    private final String baseUrl;

    private final HttpClient client;
    final ObjectMapper mapper;
    private final AnimePersister persister;
    private final ExecutorService executor;
    private final boolean executorOwned;

    public FetchTop(HttpClient client,
                    ObjectMapper mapper,
                    AnimePersister persister,
                    ExecutorService executor,
                    int poolSize,
                    long submitIntervalMs,
                    long countSubmitIntervalMs,
                    int maxAttempts,
                    long taskGetTimeoutSec,
                    long awaitTerminationSec,
                    String baseUrl) {
        this.client = Objects.requireNonNull(client);
        this.mapper = Objects.requireNonNull(mapper);
        this.persister = Objects.requireNonNull(persister);
        this.submitIntervalMs = submitIntervalMs;
        this.countSubmitIntervalMs = countSubmitIntervalMs;
        this.maxAttempts = maxAttempts;
        this.taskGetTimeoutSec = taskGetTimeoutSec;
        this.awaitTerminationSec = awaitTerminationSec;
        this.baseUrl = Objects.requireNonNull(baseUrl);

        if (executor != null) {
            this.executor = executor;
            this.executorOwned = false;
        } else {
            this.executor = Executors.newFixedThreadPool(poolSize);
            this.executorOwned = true;
        }
    }

    public static FetchTop createDefault(AnimePersister persister) {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .version(HttpClient.Version.HTTP_1_1)
                .build();
        ObjectMapper mapper = new ObjectMapper()
                .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
                .findAndRegisterModules();
        ExecutorService executor = Executors.newFixedThreadPool(DEFAULT_POOL_SIZE);
        return new FetchTop(client, mapper, persister, executor,
                DEFAULT_POOL_SIZE,
                DEFAULT_SUBMIT_INTERVAL_MS,
                DEFAULT_COUNT_SUBMIT_INTERVAL_MS,
                DEFAULT_MAX_ATTEMPTS,
                DEFAULT_TASK_GET_TIMEOUT_SEC,
                DEFAULT_AWAIT_TERMINATION_SEC,
                DEFAULT_BASE_URL);
    }

    public void fetchAndPersistAnime(int numberOfPages) {
        CompletionService<Integer> cs = new ExecutorCompletionService<>(executor);
        int submitted = 0;
        try {
            submitted = submitPages(cs, numberOfPages);
            int completed = collectResults(cs, submitted);
            LOGGER.info("Total submitted pages: {}, completed (successful tasks observed) {}", submitted, completed);
        } finally {
            if (executorOwned) {
                shutdownExecutor();
            }
        }
    }

    private int submitPages(CompletionService<Integer> cs, int numberOfPages) {
        int submitted = 0;
        for (int page = 1; page <= numberOfPages; page++) {
            final int currentPage = page;
            cs.submit(() -> fetchAndSavePageWithRetries(currentPage));
            submitted++;
            if (submitted % 25 == 0 )
                LOGGER.info("Submitted page {} of {} pages", currentPage, numberOfPages);
            try {
                Thread.sleep(submitIntervalMs);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                LOGGER.warn("Submit loop interrupted, stopping submissions at page {}", currentPage, ie);
                break;
            }
        }
        return submitted;
    }

    private Integer fetchAndSavePageWithRetries(int page) throws Exception {
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                AnimeTopResult res = fetchTopAnimePage(page);
                if (res != null && res.data != null) {
                    for (Anime anime : res.data) {
                        try {
                            persister.save(anime);
                        } catch (Exception e) {
                            LOGGER.error("Error saving anime (page {}): {}", page, e.getMessage(), e);
                        }
                    }
                }
                return page;
            } catch (Exception e) {
                LOGGER.warn("Fetch page {} failed (attempt {})", page, attempt);
                if (attempt == maxAttempts) {
                    throw e;
                }
                try {
                    long sleepMs = 1_500L * attempt;
                    TimeUnit.MILLISECONDS.sleep(sleepMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw ie;
                }
            }
        }
        return page;
    }

    private int collectResults(CompletionService<Integer> cs, int submitted) {
        int completed = 0;
        for (int i = 0; i < submitted; i++) {
            try {
                Future<Integer> f = cs.take();
                try {
                    Integer page = f.get(taskGetTimeoutSec, TimeUnit.SECONDS);
                    completed++;
                    if (completed % 50 == 0 || (page != null && page % 100 == 0)) {
                        LOGGER.info("Completed pages: {}/{} (last page: {})", completed, submitted, page);
                    }
                } catch (ExecutionException ee) {
                    LOGGER.error("Task failed with exception: {}", ee.getCause(), ee);
                } catch (CancellationException ce) {
                    LOGGER.warn("Task cancelled.", ce);
                } catch (TimeoutException te) {
                    LOGGER.error("Timed out waiting for task result.", te);
                }
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                LOGGER.error("Collector interrupted, stopping collection.", ie);
                break;
            }
        }
        return completed;
    }

    private void shutdownExecutor() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(awaitTerminationSec, TimeUnit.SECONDS)) {
                LOGGER.warn("Executor didn't stop in {}s, forcing shutdownNow()", awaitTerminationSec);
                executor.shutdownNow();
            }
        } catch (InterruptedException ie) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    public int countPages() {
        if (!pageExists(1)) {
            return 0;
        }

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
                Thread.sleep(countSubmitIntervalMs);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                LOGGER.error("Interrupted while sleeping in countPages", ie);
                break;
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
                Thread.sleep(submitIntervalMs / 2);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                LOGGER.error("Interrupted while sleeping in countPages", ie);
                break;
            }
        }
        return l;
    }

    public boolean pageExists(int page) {
        try {
            AnimeTopResult res = fetchTopAnimePage(page);
            return res != null && res.data != null && !res.data.isEmpty();
        } catch (IOException | InterruptedException e) {
            LOGGER.error("pageExists({}) failed: {}", page, e.getMessage(), e);
            return false;
        } catch (RuntimeException e) {
            LOGGER.error("pageExists({}) runtime error: {}", page, e.getMessage(), e);
            return false;
        }
    }

    public AnimeTopResult fetchTopAnimePage(int page) throws IOException, InterruptedException {
        String url = String.format("%s/top/anime?page=%d", baseUrl, page);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new HttpRequestException("HTTP " + response.statusCode());
        }
        return mapper.readValue(response.body(), AnimeTopResult.class);
    }

    public void close() {
        if (executorOwned) {
            shutdownExecutor();
        }
    }
}
