package scripts;

import data.UserStat;
import data.Users;
import mapper.UserStatMapper;
import mapper.UserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import user_parsing.FetchUsers;
import user_parsing.Parser;
import user_parsing.StatsData;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;

import java.util.List;

public class DataIntegrityRestorer {

    private final EntityManagerFactory emf;
    private final double thresholdPercent;
    private final int batchSize;
    private final UserResyncService resyncService;
    private final boolean deleteOnFailure;
    private static final Logger LOGGER = LoggerFactory.getLogger(DataIntegrityRestorer.class);

    public DataIntegrityRestorer(double thresholdPercent, int batchSize, int resyncPersistBatchSize) {
        this(thresholdPercent, batchSize, resyncPersistBatchSize, true);
    }

    public DataIntegrityRestorer(double thresholdPercent, int batchSize, int resyncPersistBatchSize, boolean deleteOnFailure) {
        this.emf = Parser.getEmf();
        this.thresholdPercent = thresholdPercent;
        this.batchSize = batchSize;
        this.resyncService = new UserResyncService(resyncPersistBatchSize);
        this.deleteOnFailure = deleteOnFailure;
    }

    public void run() {
        EntityManager em = emf.createEntityManager();
        try {
            int offset = 0;
            while (true) {
                List<Users> users = em.createQuery("SELECT u FROM Users u ORDER BY u.malId", Users.class)
                        .setFirstResult(offset)
                        .setMaxResults(batchSize)
                        .getResultList();
                if (users == null || users.isEmpty()) break;

                for (Users user : users) {
                    try {
                        processUserById(user.getMalId());
                    } catch (Exception ex) {
                        LOGGER.error("Error processing user {} ({})",user.getMalId(), user.getUsername(), ex);
                    }
                }

                offset += users.size();
            }
            LOGGER.info("Offset processed: {}", offset);
        } finally {
            em.close();
        }
    }

    public void processUserById(Integer malId) {
        if (malId == null) return;

        UserStatsResult r = readUserAndStats(malId);
        if (r.user == null) {
            LOGGER.warn("User with malId={} not found, skipping", malId);
            return;
        }

        LOGGER.info("User {} (malId={}): nativeCount={}, reported={}",
                r.user.getUsername(), malId, r.nativeCount, r.reported);

        double diffPercent = computeDiffPercent(r.nativeCount, r.reported);

        if (diffPercent > thresholdPercent) {
            handleResyncAndUpdate(r.user, malId, diffPercent);
            return;
        }

        if (r.user.getUrl() == null) {
            LOGGER.warn("Missing profile fields for user {}, updating profile only.",  r.user.getUsername());
            refreshProfileOnly(r.user.getUsername());
        }
    }

    private UserStatsResult readUserAndStats(int malId) {
        EntityManager em = emf.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();

            Users user = em.find(Users.class, malId);
            if (user == null) {
                tx.commit();
                return new UserStatsResult(null, 0L, 0);
            }

            Number nativeCountNum = (Number) em.createNativeQuery(
                            "SELECT COUNT(*) FROM user_anime_stat WHERE user_id = ?1")
                    .setParameter(1, malId)
                    .getSingleResult();
            long countNative = nativeCountNum == null ? 0L : nativeCountNum.longValue();

            UserStat stat = em.createQuery(
                            "SELECT s FROM UserStat s WHERE s.user = :u", UserStat.class)
                    .setParameter("u", user)
                    .getResultStream()
                    .findFirst()
                    .orElse(null);
            int reported = stat != null ? stat.getTotalEntries() : 0;

            tx.commit();
            return new UserStatsResult(user, countNative, reported);
        } catch (RuntimeException e) {
            if (tx.isActive()) tx.rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    private double computeDiffPercent(long dbCount, int reported) {
        if (reported == 0) {
            return dbCount > 0 ? 1.0 : 0.0;
        }
        return Math.abs(dbCount - reported) / (double) reported;
    }

    private void handleResyncAndUpdate(Users user, int malId, double diffPercent) {
        System.out.println("MISMATCH (diff=" + (diffPercent * 100) + "%). Starting resync for " + user.getUsername());
        boolean resyncOk = false;
        try {
            resyncOk = resyncService.resyncUserUpsertFetchWithRetries(user.getUsername(), malId, 3, 500);
        } catch (Exception e) {
            System.out.println("Resync threw an exception for " + user.getUsername() + ": " + e.getMessage());
            e.printStackTrace();
        }

        if (!resyncOk) {
            System.out.println("Resync failed for " + user.getUsername() + ".");
            if (deleteOnFailure) {
                System.out.println("deleteOnFailure is enabled — deleting user data for malId=" + malId);
                safeDeleteUserData(malId);
            } else {
                System.out.println("deleteOnFailure is disabled — keeping existing data for " + user.getUsername());
            }
            return;
        }

        System.out.println("Resync succeeded for " + user.getUsername() + ". Now updating UserStat.totalEntries from remote stats");
        updateUserStatFromRemote(user, malId);
    }

    private void updateUserStatFromRemote(Users user, int malId) {
        try {
            StatsData freshStats = FetchUsers.fetchUserStats(user.getUsername());
            if (freshStats != null) {
                updateUserStatInTx(freshStats, malId);
                System.out.println("UserStat updated for " + user.getUsername());
            } else {
                System.out.println("Could not fetch remote stats to update user_stat for " + user.getUsername());
                if (deleteOnFailure) {
                    System.out.println("deleteOnFailure is enabled — deleting user data because fresh stats couldn't be fetched for malId=" + malId);
                    safeDeleteUserData(malId);
                }
            }
        } catch (Exception e) {
            System.out.println("Error fetching remote stats after resync for " + user.getUsername() + ": " + e.getMessage());
            e.printStackTrace();
            if (deleteOnFailure) {
                System.out.println("deleteOnFailure is enabled — deleting user data due to exception when fetching fresh stats for malId=" + malId);
                safeDeleteUserData(malId);
            }
        }
    }

    private void updateUserStatInTx(StatsData freshStats, int malId) {
        EntityManager em2 = emf.createEntityManager();
        EntityTransaction tx2 = em2.getTransaction();
        try {
            tx2.begin();
            Users mergedUser = em2.find(Users.class, malId);
            var userStat = UserStatMapper.mapOrCreate(freshStats, mergedUser, em2);
            em2.merge(userStat);
            tx2.commit();
        } catch (Exception e) {
            if (tx2.isActive()) tx2.rollback();
            System.out.println("Failed to update UserStat for malId=" + malId + ": " + e.getMessage());
            e.printStackTrace();
            if (deleteOnFailure) {
                System.out.println("deleteOnFailure is enabled — deleting user data due to failed UserStat update for malId=" + malId);
                safeDeleteUserData(malId);
            }
        } finally {
            em2.close();
        }
    }

    private void safeDeleteUserData(int malId) {
        try {
            resyncService.deleteUserData(malId);
        } catch (Exception ex) {
            LOGGER.error("Failed to delete data for {}: {}", malId, ex.getMessage(), ex);
        }
    }

    private void refreshProfileOnly(String username) {
        try {
            user_parsing.UserLite dto = FetchUsers.fetchUserByUsername(username);
            user_parsing.StatsData stats = FetchUsers.fetchUserStats(dto.username);
            persistProfileOnly(dto, stats);
        } catch (Exception e) {
            LOGGER.error("Failed profile-only refresh for {}: ", username, e);
        }
    }

    private void persistProfileOnly(user_parsing.UserLite dto, user_parsing.StatsData stats) {
        EntityManager em = emf.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            var userEntity = UserMapper.map(dto, em);
            var userStat = UserStatMapper.mapOrCreate(stats, userEntity, em);
            em.merge(userStat);
            tx.commit();
            LOGGER.info("Profile-only update committed for {}", dto.username);
        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    private record UserStatsResult(Users user, long nativeCount, int reported) {}
}
