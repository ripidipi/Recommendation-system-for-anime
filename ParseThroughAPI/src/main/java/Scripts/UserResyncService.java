package Scripts;

import UserParsing.FetchUsers;
import UserParsing.Parser;
import UserParsing.UserAnimeEntry;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class UserResyncService {

    private final EntityManagerFactory emf;
    private final int persistBatchSize;

    public UserResyncService(int persistBatchSize) {
        this.emf = Parser.getEmf();
        this.persistBatchSize = persistBatchSize;
    }

    public boolean resyncUserUpsertFetchWithRetries(String username, int malId,
                                                    int maxAttempts, long baseSleepMs) {
        Exception lastException = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                System.out.println("Attempt " + attempt + " for user " + username);
                UserParsing.StatsData stats = null;
                try { stats = FetchUsers.fetchUserStats(username); } catch (Exception e) {
                    System.out.println("Could not fetch stats for " + username + " before download: " + e.getMessage());
                }
                int reported = (stats != null && stats.anime != null) ? stats.anime.totalEntries : 0;
                System.out.println("User " + username + " reported(totalEntries) = " + reported);

                List<UserAnimeEntry> collected = new ArrayList<>();
                boolean ok = FetchUsers.fetchUserAnimeList(username, page -> {
                    if (page != null && !page.isEmpty()) {
                        collected.addAll(page);
                        System.out.println("Downloaded page, total collected = " + collected.size());
                    } else {
                        System.out.println("Downloaded empty page");
                    }
                });

                if (!ok) {
                    lastException = new RuntimeException("fetch returned false (partial/unavailable)");
                    System.out.println("Fetch returned false on attempt " + attempt + " for " + username);
                } else {
                    System.out.println("Finished download: collected = " + collected.size() +
                            " entries for " + username);

                    if (reported > 0 && collected.size() == 0) {
                        String reason = "reported>0 but collected==0 (private/blocked)";
                        logFailed(username, malId, "manual_review", reason);
                        System.out.println("WARN: " + reason + " for " + username + ". Aborting upsert.");
                        return false;
                    }
                    if (reported > 0) {
                        double ratio = collected.size() / (double) reported;
                        if (ratio < 0.25) {
                            String reason = String.format("collected only %d%% of reported (%.2f%%)",
                                    (int)(ratio*100), ratio*100.0);
                            logFailed(username, malId, "manual_review", reason);
                            System.out.println("WARN: " + reason + " for " + username + ". Aborting upsert.");
                            return false;
                        }
                    }

                    EntityManager em = emf.createEntityManager();
                    EntityTransaction tx = em.getTransaction();
                    int processed = 0;
                    try {
                        tx.begin();
                        for (UserAnimeEntry entry : collected) {
                            Mapper.UserAnimeStatMapper.mapAndCreate(entry, malId, em);
                            processed++;
                            if (processed % persistBatchSize == 0) {
                                em.flush();
                                em.clear();
                                System.out.println("Flushed " + processed + " records (upsert)");
                            }
                        }
                        em.flush();
                        tx.commit();
                        System.out.println("Upsert commit done. Processed " + processed +
                                " records for user " + username);
                    } catch (Exception e) {
                        if (tx.isActive()) tx.rollback();
                        System.out.println("Transaction failed on attempt " + attempt +
                                " for " + username + ": " + e.getMessage());
                        e.printStackTrace();
                    } finally {
                        em.close();
                    }

                    return true;
                }
            } catch (IOException | InterruptedException ex) {
                lastException = ex;
                System.out.println("Network error on attempt " + attempt +
                        " for " + username + ": " + ex.getMessage());
                ex.printStackTrace();
            } catch (Exception ex) {
                lastException = ex;
                System.out.println("Unexpected error on attempt " + attempt +
                        " for " + username + ": " + ex.getMessage());
                ex.printStackTrace();
            }

            if (attempt < maxAttempts) {
                long sleep = baseSleepMs * (1L << (attempt - 1));
                System.out.println("Sleeping " + sleep + " ms before next attempt for " + username);
                try { Thread.sleep(sleep); } catch (InterruptedException ie)
                { Thread.currentThread().interrupt(); break; }
            }
        }

        String reason = lastException == null ? "unknown" : lastException.getClass().getSimpleName() +
                ":" + lastException.getMessage();
        logFailed(username, malId, "fetch_failed", reason);
        return false;
    }

    private void logFailed(String username, int malId, String status, String reason) {
        String fn = "resync_failed.csv";
        try (FileWriter fw = new FileWriter(fn, true); PrintWriter pw = new PrintWriter(fw)) {
            pw.printf("%s,%s,%d,%s,%s%n", Instant.now().toString(), username, malId, status,
                    reason.replaceAll("[\\r\\n,]", "_"));
        } catch (Exception e) {
            System.out.println("Failed to write resync_failed.csv: " + e.getMessage());
        }
    }

}
