package user_parsing;

import data.UserStat;
import data.Users;
import mapper.UserAnimeStatMapper;
import mapper.UserMapper;
import mapper.UserStatMapper;
import jakarta.persistence.*;

import java.io.IOException;

// TODO refactory needs
public class Parser {

    private static volatile EntityManagerFactory emf;

    public static EntityManagerFactory getEmf() {
        if (emf == null) {
            synchronized (Parser.class) {
                if (emf == null) {
                    try {
                        emf = Persistence.createEntityManagerFactory("animePU");
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to initialize EntityManagerFactory", e);
                    }
                }
            }
        }
        return emf;
    }

    public static void saveUserAndStats(UserLite dto) {
        EntityManager em = getEmf().createEntityManager();
        EntityTransaction tx = em.getTransaction();

        try {
            tx.begin();

            Users user = UserMapper.map(dto, em);

            StatsData stats = FetchUsers.fetchUserStats(dto.username);

            animeListPersist(dto, em);

            UserStat userStats = UserStatMapper.mapOrCreate(stats, user, em);
            em.merge(userStats);

            tx.commit();
        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            throw new RuntimeException("Error saving user and stats: " + dto.username, e);
        } finally {
            em.close();
        }
    }

    public static void saveUserAndStats(UserLite dto, StatsData stats) {
        EntityManager em = getEmf().createEntityManager();
        EntityTransaction tx = em.getTransaction();

        try {
            tx.begin();

            Users user = UserMapper.map(dto, em);

            animeListPersist(dto, em);

            UserStat userStats = UserStatMapper.mapOrCreate(stats, user, em);
            em.merge(userStats);

            tx.commit();
        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            throw new RuntimeException("Error saving user and stats: " + dto.username, e);
        } finally {
            em.close();
        }
    }

    private static void animeListPersist(UserLite dto, EntityManager em) throws IOException, InterruptedException {
        final int batchSize = 100;
        final int[] counter = {0};

        boolean ok = FetchUsers.fetchUserAnimeList(dto.username, page -> {
            for (UserAnimeEntry entry : page) {
                UserAnimeStatMapper.map(entry, dto.malId, em);
                if (++counter[0] % batchSize == 0) {
                    em.flush();
                }
            }
            try {
                em.flush();
            } catch (Exception e) {
                System.out.println("Flush error for user " + dto.username + ": " + e.getMessage());
            }
        });
        if (!ok) {
            System.out.println("Warning: partial data for user " + dto.username + " (fetch returned false).");
        }
    }
}
