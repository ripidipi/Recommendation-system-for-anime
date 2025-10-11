package utils;

import okhttp3.ConnectionPool;
import okhttp3.Dispatcher;
import okhttp3.OkHttpClient;

import java.time.Duration;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Manager for OkHttpClient that keeps a reference to the Dispatcher executor and ConnectionPool
 * so we can reliably evict connections and shutdown executors when recreating the client.
 */
public class OkHttpClientManager {

    private static final int THREAD_POOL_SIZE = 15;
    private static final long AWAIT_TERMINATION_MS = 10_000;
    private static final int MAX_IDLE_CONNECTIONS = 15;
    private static final long KEEP_ALIVE_MINUTES = 5;
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(30);

    private volatile OkHttpClient client;
    private volatile ExecutorService dispatcherExecutor;
    private volatile ConnectionPool connectionPool;

    private final Object lock = new Object();

    public OkHttpClientManager() { }

    public OkHttpClient getClient() {
        if (client == null) {
            synchronized (lock) {
                if (client == null) createNewClient();
            }
        }
        return client;
    }

    private void createNewClient() {
        OkHttpClient oldClient = this.client;
        ExecutorService oldExec = this.dispatcherExecutor;
        ConnectionPool oldPool = this.connectionPool;

        ExecutorService exec = Executors.newFixedThreadPool(THREAD_POOL_SIZE, new NamedThreadFactory("okhttp-dispatcher-"));
        Dispatcher dispatcher = new Dispatcher(exec);
        ConnectionPool pool = new ConnectionPool(MAX_IDLE_CONNECTIONS, KEEP_ALIVE_MINUTES, TimeUnit.MINUTES);

        OkHttpClient newClient = new OkHttpClient.Builder()
                .connectTimeout(CONNECT_TIMEOUT)
                .readTimeout(READ_TIMEOUT)
                .dispatcher(dispatcher)
                .connectionPool(pool)
                .retryOnConnectionFailure(true)
                .build();

        this.dispatcherExecutor = exec;
        this.connectionPool = pool;
        this.client = newClient;

        if (oldClient != null) {
            try {
                if (oldPool != null) oldPool.evictAll();
            } catch (Exception e) {
                System.err.println("OkHttpClientManager: error evicting old pool: " + e);
            }
        }

        if (oldExec != null) {
            try {
                oldExec.shutdownNow();
                if (!oldExec.awaitTermination(AWAIT_TERMINATION_MS, TimeUnit.MILLISECONDS)) {
                    System.err.println("OkHttpClientManager: old dispatcher didn't terminate in time");
                }
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            } catch (Exception ex) {
                System.err.println("OkHttpClientManager: error shutting down old exec: " + ex);
            }
        }
    }

    private static class NamedThreadFactory implements ThreadFactory {
        private final String prefix;
        private final AtomicInteger idx = new AtomicInteger(1);
        NamedThreadFactory(String prefix) { this.prefix = prefix; }
        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, prefix + idx.getAndIncrement());
            t.setDaemon(false);
            return t;
        }
    }
}
