package UserParsing;

import Exeptions.HttpRequestException;
import Scripts.DataIntegrityRestorer;
import Scripts.UserResyncService;
import Utils.OkHttpClientManager;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import okhttp3.Call;
import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.GZIPInputStream;

import static UserParsing.Parser.saveUserAndStats;

public class FetchUsers {

    private static final String API_HOST;
    private static final String MAL_HOST;

    static {
        String apiHost = System.getProperty("jikan.base");
        if (apiHost == null || apiHost.isBlank()) {
            apiHost = System.getenv().getOrDefault("JIKAN_BASE", "https://api.jikan.moe/v4");
        }
        API_HOST = apiHost;

        String malHost = System.getProperty("mal.base");
        if (malHost == null || malHost.isBlank()) {
            malHost = System.getenv().getOrDefault("MAL_HOST", "https://myanimelist.net");
        }
        MAL_HOST = malHost;
    }


    private static final OkHttpClientManager HTTP_CLIENT_MANAGER = new OkHttpClientManager();

    private static final ObjectMapper mapper = new ObjectMapper()
            .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
            .findAndRegisterModules();

    private static final List<String> USER_AGENTS = List.of(
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                    "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:120.0) Gecko/20100101 Firefox/120.0",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 13_5) AppleWebKit/605.1.15 " +
                    "(KHTML, like Gecko) Version/16.6 Safari/605.1.15",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                    "(KHTML, like Gecko) Edg/120.0.0.0",
            "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15 " +
                    "(KHTML, like Gecko) Version/17.0 Mobile/15E148 Safari/605.1.15",
            "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 " +
                    "(KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36",
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 " +
                    "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
            "Mozilla/5.0 (X11; Linux x86_64; rv:120.0) Gecko/20100101 Firefox/120.0"
    );

    private static final SimpleRateLimiter MAL_RATE_LIMITER = new SimpleRateLimiter(1000, 800);
    private static final ConcurrentHashMap<String, Long> taintedHosts = new ConcurrentHashMap<>();
    private static final long TAINT_MILLIS = Duration.ofMinutes(2).toMillis();

    public static void fetchAndPersistRandomUsers(int numberOfUsers, int numberOfAnimeInLists,
                                                  int numberOfCompletedAnimeInLists) {
        final int MAX_TASK_MS = 120_000;
        final int BETWEEN_TASK_SLEEP_MS = 2_500;
        final int POOL_SIZE = 7;

        ExecutorService workerPool = Executors.newFixedThreadPool(POOL_SIZE);
        ScheduledExecutorService scheduledCanceller = Executors.newSingleThreadScheduledExecutor();
        Semaphore concurrencyLimiter = new Semaphore(POOL_SIZE);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger attempts = new AtomicInteger(0);
        DataIntegrityRestorer dataIntegrityRestorer = new DataIntegrityRestorer(0.05,
                100, 100);

        try {
            while (successCount.get() < numberOfUsers) {
                try {
                    concurrencyLimiter.acquire();
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }

                if (successCount.get() >= numberOfUsers) {
                    concurrencyLimiter.release();
                    break;
                }

                Callable<Boolean> task = () -> {
                    try {
                        UserLite curUser = fetchRandomUser();
                        try { Thread.sleep(BETWEEN_TASK_SLEEP_MS); } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            return false;
                        }
                        StatsData sd = fetchUserStats(curUser.username);

                        if (sd == null || sd.anime == null) {
                            System.out.println("No stats for user " + curUser.username);
                            return false;
                        }
                        if (sd.anime.totalEntries < numberOfAnimeInLists &&
                                sd.anime.completed < numberOfCompletedAnimeInLists) {
                            System.out.println("Too few anime for " + curUser.username + ": " + sd.anime.totalEntries);
                            return false;
                        }

                        saveUserAndStats(curUser, sd);
                        dataIntegrityRestorer.processUser(curUser.malId);

                        try { Thread.sleep(BETWEEN_TASK_SLEEP_MS); } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            return false;
                        }
                        return true;
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        System.out.println("Task interrupted for user fetch");
                        return false;
                    } catch (Exception ex) {
                        System.out.println("Task exception: " + ex.getClass() + " -> " + ex.getMessage());
                        ex.printStackTrace();
                        return false;
                    }
                };

                Future<Boolean> future = workerPool.submit(task);
                attempts.incrementAndGet();

                ScheduledFuture<?> canceller = scheduledCanceller.schedule(() -> {
                    if (!future.isDone()) {
                        System.out.println("Cancelling long task (running > " + MAX_TASK_MS + " ms)");
                        future.cancel(true);
                    }
                }, MAX_TASK_MS, TimeUnit.MILLISECONDS);

                workerPool.submit(() -> {
                    try {
                        Boolean ok;
                        try {
                            ok = future.get(MAX_TASK_MS + 5000, TimeUnit.MILLISECONDS);
                        } catch (TimeoutException te) {
                            future.cancel(true);
                            System.out.println("Future.get timeout -> cancelled");
                            ok = false;
                        } catch (CancellationException ce) {
                            System.out.println("Future was cancelled");
                            ok = false;
                        } catch (ExecutionException ee) {
                            System.out.println("Execution exception in task: " + ee.getCause());
                            ok = false;
                        } catch (InterruptedException ie) {
                            System.out.println("Future thread was interrupted");
                            ok = false;
                        }

                        if (Boolean.TRUE.equals(ok)) {
                            int done = successCount.incrementAndGet();
                            if (done % 25 == 0) System.out.println(
                                    "<<<-------------------------->>>\n" +
                                            "Completed users: " + done + "/" + numberOfUsers +
                                            "\n<<<-------------------------->>>");
                        }
                    } finally {
                        canceller.cancel(false);
                        concurrencyLimiter.release();
                    }
                });
            }

            workerPool.shutdown();
            try {
                if (!workerPool.awaitTermination(3, TimeUnit.MINUTES)) {
                    System.out.println("Worker pool didn't terminate, forcing shutdown");
                    workerPool.shutdownNow();
                }
            } catch (InterruptedException ie) {
                workerPool.shutdownNow();
                Thread.currentThread().interrupt();
            }
        } finally {
            scheduledCanceller.shutdownNow();
        }

        System.out.println("Finished. Successful users: " + successCount.get() + ", attempts: " + attempts.get());
    }

    public static void fetchAndPersistUserByUsername(String username, int tryNumber) {
        try {
            UserLite curUser = fetchUserByUsername(username);
            StatsData sd = fetchUserStats(curUser.username);

            saveUserAndStats(curUser, sd);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            if (tryNumber < 3) fetchAndPersistUserByUsername(username, tryNumber + 1);
        }
    }

    private record DecodedResponse(int status, String body, String contentEncoding, String contentType) { }

    private static DecodedResponse fetchDecoded(String url) throws IOException, InterruptedException {
        String hostKey = url.contains("myanimelist.net") ? "myanimelist.net" : url;

        Long until = taintedHosts.get(hostKey);
        if (until != null && System.currentTimeMillis() < until) {
            throw new IOException("Host " + hostKey + " is tainted until " + until);
        }

        if (url.contains("myanimelist.net")) {
            MAL_RATE_LIMITER.acquire();
        }

        OkHttpClient client = HTTP_CLIENT_MANAGER.getClient();

        Request req = requestBuild(url);
        try (Response resp = client.newCall(req).execute()) {
            int status = resp.code();
            byte[] bodyBytes = resp.body() == null ? new byte[0] : resp.body().bytes();
            String contentEncoding = resp.header("Content-Encoding", "");
            String contentType = resp.header("Content-Type", "");

            System.out.println("URL: " + url + " -> status=" + status + ", bytes=" + (bodyBytes==null?0:bodyBytes.length)
                    + ", enc=" + contentEncoding + ", type=" + contentType);

            byte[] decompressed = bodyBytes;
            boolean isGzip = false;
            try {
                String encLower = contentEncoding == null ? "" : contentEncoding.toLowerCase(Locale.ROOT);
                isGzip = encLower.contains("gzip")
                        || (bodyBytes != null && bodyBytes.length >= 2 && (bodyBytes[0] == (byte)0x1F && bodyBytes[1] == (byte)0x8B));
            } catch (Exception ignored) {}

            if (isGzip && bodyBytes != null && bodyBytes.length > 0) {
                try (ByteArrayInputStream bais = new ByteArrayInputStream(bodyBytes);
                     GZIPInputStream gis = new GZIPInputStream(bais);
                     ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                    byte[] buffer = new byte[4096];
                    int read;
                    while ((read = gis.read(buffer)) != -1) {
                        baos.write(buffer, 0, read);
                    }
                    decompressed = baos.toByteArray();
                } catch (IOException e) {
                    System.err.println("GZIP unpack failed for url: " + url);
                    throw e;
                }
            } else if (contentEncoding != null && contentEncoding.toLowerCase(Locale.ROOT).contains("br")) {
                throw new IOException("Received brotli content for url: " + url + " but no decoder configured");
            }

            String body = decompressed == null ? "" : new String(decompressed, StandardCharsets.UTF_8);

            String low = body.toLowerCase(Locale.ROOT);
            if (low.contains("human verification") || low.contains("gokuprops") || low.contains("awswaf") || low.contains("recaptcha")) {
                taintedHosts.put(hostKey, System.currentTimeMillis() + TAINT_MILLIS);
                System.out.println("capcha " + hostKey + ", tainting for " + (TAINT_MILLIS/1000) + " minutes");
                throw new IOException("Verification detected for host: " + hostKey);
            }

            return new DecodedResponse(status, body, contentEncoding, contentType);
        }
    }

    private static Request requestBuild(String url) {
        String ua = USER_AGENTS.get((int)(Math.random() * USER_AGENTS.size()));
        Request.Builder b = new Request.Builder()
                .url(url)
                .get()
                .header("User-Agent", ua)
                .header("Accept", "application/json, text/javascript, */*; q=0.01")
                .header("Accept-Encoding", "gzip, deflate")
                .header("Accept-Language", "en-US,en;q=0.9")
                .header("Referer", MAL_HOST + "/animelist/")
                .header("X-Requested-With", "XMLHttpRequest");
        return b.build();
    }

    public static UserLite fetchRandomUser() throws IOException, InterruptedException {
        DecodedResponse dr = fetchDecoded(API_HOST + "/random/users");
        if (dr.status != 200) throw new HttpRequestException("HTTP " + dr.status);
        JsonNode root = mapper.readTree(dr.body);
        JsonNode data = root.get("data");
        return mapper.treeToValue(data, UserLite.class);
    }

    public static StatsData fetchUserStats(String username) throws IOException, InterruptedException {
        String url = API_HOST + "/users/" + username + "/statistics";
        DecodedResponse dr = fetchDecoded(url);
        if (dr.status != 200) throw new HttpRequestException("HTTP " + dr.status);
        return mapper.readValue(dr.body, StatsResponse.class).data;
    }

    public static UserLite fetchUserByUsername(String username) throws IOException, InterruptedException {
        DecodedResponse dr = fetchDecoded(API_HOST + "/users/" + username);
        if (dr.status != 200) throw new HttpRequestException("HTTP " + dr.status);
        JsonNode data = mapper.readTree(dr.body).get("data");
        return mapper.treeToValue(data, UserLite.class);
    }

    public static boolean fetchUserAnimeList(String username,
                                             java.util.function.Consumer<List<UserAnimeEntry>> pageHandler)
            throws IOException, InterruptedException {
        int offset = 0;
        final int MAX_RETRIES = 3;
        final long BASE_SLEEP_MS = 2_500;

        while (true) {
            String url = "https://myanimelist.net/animelist/" + username + "/load.json?offset=" + offset;

            DecodedResponse dr = null;
            int attempt = 0;
            IOException lastIo = null;
            while (attempt < MAX_RETRIES) {
                attempt++;
                try {
                    dr = fetchDecoded(url);
                    break;
                } catch (IOException ioe) {
                    lastIo = ioe;
                    long sleep = BASE_SLEEP_MS * attempt;
                    System.out.println("Network/IO error for " + url + " -> " + ioe.getMessage());
                    Thread.sleep(5_000);
                }
            }
            if (dr == null) {
                if (lastIo != null) throw lastIo;
                throw new IOException("Failed to fetch " + url);
            }

            int code = dr.status;
            String body = dr.body == null ? "" : dr.body.trim();

            if (code == 404) return true;

            if (!body.isEmpty() && body.startsWith("<")) {
                System.out.println("Got HTML instead of JSON for user " + username + " url=" + url);
                System.out.println("HTTP " + code + " Body (start): " +
                        (body.length() > 1000 ? body.substring(0, 1000) : body));
                return false;
            }

            if (code == 429) {
                System.out.println("429 Too Many Requests for " + username + " url=" + url);
                Thread.sleep(5_000);
                continue;
            }

            if (code >= 500 && code < 600) {
                System.out.println("Server error " + code + " for " + url);
                Thread.sleep(BASE_SLEEP_MS);
                continue;
            }

            if (code == 400 || code == 403) {
                System.out.println("List unavailable for user " + username + " (HTTP " + code + ")");
                return true;
            }

            if (code != 200) {
                System.out.println("Non-200 for " + username + " url=" + url + " code=" + code);
                System.out.println("Body (start): " + (body == null ? "null" : body.substring(0, Math.min(1000, body.length()))));
                Thread.sleep(7_000);
                continue;
            }

            List<UserAnimeEntry> page;
            try {
                page = mapper.readValue(body, new TypeReference<List<UserAnimeEntry>>() {});
            } catch (Exception e) {
                System.out.println("JSON parse error for " + username + " url=" + url);
                System.out.println("Body (start): " + (body == null ? "null" : body.substring(0, Math.min(1000, body.length()))));
                e.printStackTrace();
                return false;
            }

            if (page == null || page.isEmpty()) return true;

            pageHandler.accept(page);
            offset += page.size();

            Thread.sleep(BASE_SLEEP_MS);
        }
    }

    private static final class SimpleRateLimiter {
        private final long minDelayMs;
        private final long maxJitterMs;
        private long lastCall = 0;

        SimpleRateLimiter(long minDelayMs, long maxJitterMs) {
            this.minDelayMs = minDelayMs;
            this.maxJitterMs = maxJitterMs;
        }

        void acquire() {
            synchronized (this) {
                long now = System.currentTimeMillis();
                long wait = Math.max(0, minDelayMs - (now - lastCall));
                if (wait > 0) {
                    try {
                        long jitter = ThreadLocalRandom.current().nextLong(maxJitterMs + 1);
                        Thread.sleep(wait + jitter);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
                lastCall = System.currentTimeMillis();
            }
        }
    }
}
