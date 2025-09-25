package AnimeParsing;

import Mapper.AnimeMapper;
import jakarta.persistence.*;

import java.util.HashMap;
import java.util.Map;

public class Parser {

    private static volatile EntityManagerFactory emf;

    public static EntityManagerFactory getEmf() {
        if (emf == null) {
            synchronized (Parser.class) {
                if (emf == null) {
                    try {
                        Map<String, Object> props = new HashMap<>();
                        props.put("hibernate.bytecode.provider", "javassist");
                        emf = Persistence.createEntityManagerFactory("animePU", props);
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to initialize EntityManagerFactory", e);
                    }
                }
            }
        }
        return emf;
    }

    public static void saveAnimeToDB(Anime dto) {
        EntityManager em = getEmf().createEntityManager();
        EntityTransaction tx = em.getTransaction();

        if (dto.title == null || dto.title.isBlank()) {
            System.out.println("Skipping anime with empty title, malId=" + dto.malId);
            return;
        }

        try {
            tx.begin();

            var existingAnime = em.find(Data.Anime.class, dto.malId);
            if (existingAnime != null) {
                AnimeMapper.update(existingAnime, dto, em);
                em.merge(existingAnime);
            } else {
                var newAnime = AnimeMapper.map(dto, em);
                em.persist(newAnime);
            }
            tx.commit();
        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            throw new RuntimeException("Error saving anime: " + dto.title, e);
        } finally {
            em.close();
        }
    }
}
