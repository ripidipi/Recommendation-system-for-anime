package Scripts;

import Data.Users;
import Data.UserStat;
import UserParsing.FetchUsers;
import UserParsing.Parser;
import UserParsing.StatsData;
import UserParsing.UserLite;
import jakarta.persistence.*;

import java.util.List;

public class DataIntegrityRestorer {

    private final EntityManagerFactory emf;
    private final double thresholdPercent;
    private final int batchSize;

    public DataIntegrityRestorer(double thresholdPercent, int batchSize) {
        this.emf = Parser.getEmf();
        this.thresholdPercent = thresholdPercent;
        this.batchSize = batchSize;
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
                        System.out.println("Error processing user " + user.getMalId() + " (" + user.getUsername() + "): " + ex);
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
        EntityTransaction tx = em.getTransaction();

        try {
            tx.begin();

            Users user = em.find(Users.class, malId);
            if (user == null) {
                System.out.println("User with malId=" + malId + " not found, skipping.");
                tx.commit();
                return;
            }

            Long countByField = em.createQuery(
                            "SELECT COUNT(uas) FROM UserAnimeStat uas WHERE uas.userId = :uid", Long.class)
                    .setParameter("uid", malId)
                    .getSingleResult();

            Long countByAssoc = em.createQuery(
                            "SELECT COUNT(uas) FROM UserAnimeStat uas WHERE uas.user = :user", Long.class)
                    .setParameter("user", user)
                    .getSingleResult();

            Number nativeCountNum = (Number) em.createNativeQuery(
                            "SELECT COUNT(*) FROM user_anime_stat WHERE user_id = ?1")
                    .setParameter(1, malId)
                    .getSingleResult();
            long countNative = nativeCountNum == null ? 0L : nativeCountNum.longValue();

            UserStat stat = em.createQuery("SELECT s FROM UserStat s WHERE s.user = :u", UserStat.class)
                    .setParameter("u", user)
                    .getResultStream()
                    .findFirst()
                    .orElse(null);
            int reported = stat != null ? stat.getTotalEntries() : 0;

            tx.commit();

            System.out.printf("User %s (malId=%d): dbFieldCount=%d, assocCount=%d, nativeCount=%d, reported=%d%n",
                    user.getUsername(), malId,
                    countByField == null ? 0L : countByField,
                    countByAssoc == null ? 0L : countByAssoc,
                    countNative,
                    reported);

            long dbCount = countNative;

            double diffPercent;
            if (reported == 0) {
                diffPercent = dbCount > 0 ? 1.0 : 0.0;
            } else {
                diffPercent = Math.abs(dbCount - reported) / (double) reported;
            }

            if (diffPercent > thresholdPercent) {
                System.out.println("MISMATCH (diff=" + (diffPercent * 100) + "%). Refreshing full user data: " + user.getUsername());
                refreshFull(user.getUsername());
                return;
            }

            boolean missing = user.getUrl() == null;
            if (missing) {
                System.out.println("Missing profile fields for user " + user.getUsername() + ", updating profile only.");
                refreshProfileOnly(user.getUsername());
            } else {}

        } catch (Exception ex) {
            if (tx.isActive()) tx.rollback();
            throw ex;
        } finally {
            if (tx.isActive()) tx.rollback();
            em.close();
        }
    }

    private void refreshFull(String username) {
        try {
            UserLite dto = FetchUsers.fetchUserByUsername(username);
            StatsData stats = FetchUsers.fetchUserStats(dto.username);
            UserParsing.Parser.saveUserAndStats(dto, stats);
            System.out.println("Refreshed full data for " + username);
        } catch (Exception e) {
            System.out.println("Failed full refresh for " + username + ": " + e);
            e.printStackTrace();
        }
    }

    private void refreshProfileOnly(String username) {
        try {
            UserLite dto = FetchUsers.fetchUserByUsername(username);
            StatsData stats = FetchUsers.fetchUserStats(dto.username);
            EntityManager em = emf.createEntityManager();
            EntityTransaction tx = em.getTransaction();
            try {
                tx.begin();
                var userEntity = Mapper.UserMapper.mapOrCreate(dto, em);
                var userStat = Mapper.UserStatMapper.mapOrCreate(stats, userEntity, em);
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