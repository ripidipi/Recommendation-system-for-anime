package AnimeParsing;

import Mapper.AnimeMapper;
import jakarta.persistence.*;

public class Parser {

    private static final EntityManagerFactory emf = Persistence.createEntityManagerFactory("animePU");

    public static void saveAnimeToDB(Anime dto) {
        EntityManager em = emf.createEntityManager();
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
