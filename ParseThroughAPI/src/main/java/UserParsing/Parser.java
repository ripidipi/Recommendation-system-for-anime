package UserParsing;

import Data.UserAnimeStat;
import Data.Users;
import jakarta.persistence.*;
import Mapper.UserMapper;
import Mapper.UserAnimeStatMapper;

import java.util.List;


public class Parser {

    private static final EntityManagerFactory emf = Persistence.createEntityManagerFactory("animePU");

    public static void saveUserAndStats(UserLite dto) {
        EntityManager em = emf.createEntityManager();
        EntityTransaction tx = em.getTransaction();

        try {
            tx.begin();

            Users user = UserMapper.mapOrUpdate(dto, em);

            StatsData stats = FetchUsers.fetchUserStats(dto.username);

            UserAnimeStat userStats = UserAnimeStatMapper.mapOrCreate(stats, user, em);
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
        EntityManager em = emf.createEntityManager();
        EntityTransaction tx = em.getTransaction();

        try {
            tx.begin();

            Users user = UserMapper.mapOrUpdate(dto, em);

            UserAnimeStat userStats = UserAnimeStatMapper.mapOrCreate(stats, user, em);
            em.merge(userStats);

            tx.commit();
        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            throw new RuntimeException("Error saving user and stats: " + dto.username, e);
        } finally {
            em.close();
        }
    }

}
