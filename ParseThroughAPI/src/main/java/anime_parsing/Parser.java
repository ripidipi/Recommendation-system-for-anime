package anime_parsing;

import exeptions.ParserException;
import mapper.AnimeMapper;
import jakarta.persistence.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

public final class Parser {

    private static final Logger LOGGER = LoggerFactory.getLogger(Parser.class);

    private Parser() {}

    public static EntityManagerFactory getEmf() {
        return EmfHolder.getEmf();
    }

    public static void saveAnimeToDB(Anime dto) {
        Objects.requireNonNull(dto, "dto must not be null");
        if (dto.title == null || dto.title.isBlank()) {
            LOGGER.info("Skipping anime with empty title, malId={}", dto.malId);
            return;
        }

        try (EntityManager em = getEmf().createEntityManager()) {
            EntityTransaction tx = em.getTransaction();
            try {
                tx.begin();
                data.Anime existingAnime = em.find(data.Anime.class, dto.malId);
                if (existingAnime != null) {
                    AnimeMapper.update(existingAnime, dto, em);
                    em.merge(existingAnime);
                } else {
                    data.Anime newAnime = AnimeMapper.map(dto, em);
                    em.persist(newAnime);
                }
                tx.commit();
            } catch (RuntimeException e) {
                if (tx.isActive()) {
                    try {
                        tx.rollback();
                    } catch (RuntimeException re) {
                        LOGGER.warn("Rollback failed for anime malId={}", dto.malId, re);
                    }
                }
                LOGGER.error("Error saving anime: {}", dto.title, e);
                throw new ParserException("Error saving anime: " + dto.title, e);
            }
        } catch (PersistenceException e) {
            LOGGER.error("Persistence error when saving anime: {}", dto.title, e);
            throw new ParserException("Persistence error saving anime: " + dto.title, e);
        }
    }
}
