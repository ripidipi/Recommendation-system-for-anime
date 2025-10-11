package scripts;

import data.UserStat;
import data.Users;
import mapper.UserStatMapper;
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
                        processUser(user.getMalId());
                    } catch (Exception ex) {
                        System.out.println("Error processing user " + user.getMalId() +
                                " (" + user.getUsername() + "): " + ex);
                        ex.printStackTrace();
                    }
                }

                offset += users.size();
            }
            System.out.println("Offset processed: " + offset);
        } finally {
            em.close();
        }
    }

    public void processUser(Integer malId) {
        if (malId == null) return;

        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();

            Users user = em.find(Users.class, malId);
            if (user == null) {
                System.out.println("User with malId=" + malId + " not found, skipping");
                em.getTransaction().commit();
                return;
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

            em.getTransaction().commit();

            System.out.printf("User %s (malId=%d): nativeCount=%d, reported=%d%n",
                    user.getUsername(), malId, countNative, reported);

            long dbCount = countNative;
            double diffPercent;
            if (reported == 0) {
                diffPercent = dbCount > 0 ? 1.0 : 0.0;
            } else {
                diffPercent = Math.abs(dbCount - reported) / (double) reported;
            }

            if (diffPercent > thresholdPercent) {
                System.out.println("MISMATCH (diff=" + (diffPercent * 100) +
                        "%). Starting resync for " + user.getUsername());
                boolean resyncOk = resyncService.resyncUserUpsertFetchWithRetries(user.getUsername(),
                        malId, 3, 500);
                if (!resyncOk) {
                    System.out.println("Resync failed for " + user.getUsername() + ".");
                    if (deleteOnFailure) {
                        System.out.println("deleteOnFailure is enabled — deleting user data for malId=" + malId);
                        try {
                            resyncService.deleteUserData(malId);
                        } catch (Exception e) {
                            System.out.println("Failed to delete data after resync failure for " + malId + ": " + e.getMessage());
                            e.printStackTrace();
                        }
                    } else {
                        System.out.println("deleteOnFailure is disabled — keeping existing data for " + user.getUsername());
                    }
                    return;
                }
                System.out.println("Resync succeeded for " + user.getUsername() +
                        "Now updating UserStat.totalEntries from remote stats");

                try {
                    StatsData freshStats = FetchUsers.fetchUserStats(user.getUsername());
                    if (freshStats != null) {
                        EntityManager em2 = emf.createEntityManager();
                        EntityTransaction tx2 = em2.getTransaction();
                        try {
                            tx2.begin();
                            Users mergedUser = em2.find(Users.class, malId);
                            var userStat = UserStatMapper.mapOrCreate(freshStats, mergedUser, em2);
                            em2.merge(userStat);
                            tx2.commit();
                            System.out.println("UserStat updated for " + user.getUsername());
                        } catch (Exception e) {
                            if (tx2.isActive()) tx2.rollback();
                            System.out.println("Failed to update UserStat for " +
                                    user.getUsername() + ": " + e.getMessage());
                            e.printStackTrace();
                            if (deleteOnFailure) {
                                System.out.println("deleteOnFailure is enabled — deleting user data due to failed UserStat update for malId=" + malId);
                                try { resyncService.deleteUserData(malId); } catch (Exception ex) {
                                    System.out.println("Failed to delete data after UserStat update failure for " + malId + ": " + ex.getMessage());
                                    ex.printStackTrace();
                                }
                            }
                        } finally {
                            em2.close();
                        }
                    } else {
                        System.out.println("Could not fetch remote stats to update user_stat for " +
                                user.getUsername());
                        if (deleteOnFailure) {
                            System.out.println("deleteOnFailure is enabled — deleting user data because fresh stats couldn't be fetched for malId=" + malId);
                            try { resyncService.deleteUserData(malId); } catch (Exception ex) {
                                System.out.println("Failed to delete data after missing fresh stats for " + malId + ": " + ex.getMessage());
                                ex.printStackTrace();
                            }
                        }
                    }
                } catch (Exception e) {
                    System.out.println("Error fetching remote stats after resync for " +
                            user.getUsername() + ": " + e.getMessage());
                    e.printStackTrace();
                    if (deleteOnFailure) {
                        System.out.println("deleteOnFailure is enabled — deleting user data due to exception when fetching fresh stats for malId=" + malId);
                        try { resyncService.deleteUserData(malId); } catch (Exception ex) {
                            System.out.println("Failed to delete data after exception fetching fresh stats for " + malId + ": " + ex.getMessage());
                            ex.printStackTrace();
                        }
                    }
                }
                return;
            }

            boolean missing = user.getUrl() == null;
            if (missing) {
                System.out.println("Missing profile fields for user " +
                        user.getUsername() + ", updating profile only.");
                refreshProfileOnly(user.getUsername());
            }

        } finally {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            em.close();
        }
    }

    private void refreshProfileOnly(String username) {
        try {
            user_parsing.UserLite dto = FetchUsers.fetchUserByUsername(username);
            user_parsing.StatsData stats = FetchUsers.fetchUserStats(dto.username);
            EntityManager em = emf.createEntityManager();
            EntityTransaction tx = em.getTransaction();
            try {
                tx.begin();
                var userEntity = mapper.UserMapper.mapOrCreate(dto, em);
                var userStat = mapper.UserStatMapper.mapOrCreate(stats, userEntity, em);
                em.merge(userStat);
                tx.commit();
                System.out.println("Profile-only update committed for " + username);
            } catch (Exception e) {
                if (tx.isActive()) tx.rollback();
                throw e;
            } finally {
                em.close();
            }
        } catch (Exception e) {
            System.out.println("Failed profile-only refresh for " + username + ": " + e);
            e.printStackTrace();
        }
    }

}
