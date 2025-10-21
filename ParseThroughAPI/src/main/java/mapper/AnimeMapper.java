package mapper;

import anime_parsing.Anime;
import data.Demographic;
import data.Genre;
import data.Producer;
import jakarta.persistence.EntityManager;

import java.util.*;

public class AnimeMapper {

    private AnimeMapper() {}

    /**
     * @return managed entity
     */
    public static data.Anime map(Anime dto, EntityManager em) {
        data.Anime entity = new data.Anime();
        entity.setMalId(dto.malId);
        apply(entity, dto, em);
        return entity;
    }

    public static void update(data.Anime entity, Anime dto, EntityManager em) {
        apply(entity, dto, em);
    }

    private static void apply(data.Anime entity, Anime dto, EntityManager em) {
        entity.setUrl(dto.url);
        entity.setStatus(dto.status);
        entity.setTitle(dto.title);
        entity.setApproved(dto.approved);
        entity.setTitleEnglish(dto.titleEnglish);
        entity.setTitleJapanese(dto.titleJapanese);
        entity.setType(dto.type);
        entity.setEpisodes(dto.episodes);
        entity.setRating(dto.rating);
        entity.setScore(dto.score);
        entity.setScoredBy(dto.scoredBy);
        entity.setSynopsis(dto.synopsis);
        entity.setBackground(dto.background);
        entity.setSeason(dto.season);
        entity.setYear(dto.year);

        updateCollection(entity.getProducers(),
                dto.producers == null ? List.of() :
                        dto.producers.stream().map(p -> findOrCreateProducer(p, em)).toList(),
                entity::setProducers);

        updateCollection(entity.getLicensors(),
                dto.licensors == null ? List.of() :
                        dto.licensors.stream().map(p -> findOrCreateProducer(p, em)).toList(),
                entity::setLicensors);

        updateCollection(entity.getStudios(),
                dto.studios == null ? List.of() :
                        dto.studios.stream().map(p -> findOrCreateProducer(p, em)).toList(),
                entity::setStudios);

        updateCollection(entity.getGenres(),
                dto.genres == null ? List.of() :
                        dto.genres.stream().map(g -> findOrCreateGenre(g, em)).toList(),
                entity::setGenres);

        updateCollection(entity.getThemes(),
                dto.themes == null ? List.of() :
                        dto.themes.stream().map(g -> findOrCreateGenre(g, em)).toList(),
                entity::setThemes);

        updateCollection(entity.getDemographics(),
                dto.demographics == null ? List.of() :
                        dto.demographics.stream().map(d -> findOrCreateDemographic(d, em)).toList(),
                entity::setDemographics);
    }

    private static <T> void updateCollection(List<T> current, List<T> updated, java.util.function.Consumer<List<T>> setter) {
        if (current == null) {
            setter.accept(new ArrayList<>(updated));
            return;
        }
        try {
            current.clear();
            LinkedHashSet<T> unique = new LinkedHashSet<>(updated);
            current.addAll(unique);
        } catch (UnsupportedOperationException e) {
            List<T> mutable = new ArrayList<>(new LinkedHashSet<>(updated));
            setter.accept(mutable);
        }
    }

    private static <T, D> T findOrCreate(
            Class<T> entityClass,
            D dto,
            EntityManager em,
            java.util.function.ToIntFunction<D> idExtractor,
            java.util.function.Supplier<T> creator,
            java.util.function.BiConsumer<T, D> initializer) {

        Integer id = idExtractor.applyAsInt(dto);
        T existing = em.find(entityClass, id);
        if (existing != null) return existing;

        T entity = creator.get();
        initializer.accept(entity, dto);
        em.persist(entity);
        return entity;
    }

    private static data.Producer findOrCreateProducer(anime_parsing.Producer dto, EntityManager em) {
        return findOrCreate(
                data.Producer.class,
                dto,
                em,
                d -> d.malId,
                Producer::new,
                (p, d) -> {
                    p.setMalId(d.malId);
                    p.setName(d.name);
                    p.setType(d.type);
                    p.setUrl(d.url);
                }
        );
    }

    private static data.Genre findOrCreateGenre(anime_parsing.Genre dto, EntityManager em) {
        return findOrCreate(
                data.Genre.class,
                dto,
                em,
                d -> d.malId,
                Genre::new,
                (g, d) -> {
                    g.setMalId(d.malId);
                    g.setName(d.name);
                    g.setType(d.type);
                    g.setUrl(d.url);
                }
        );
    }

    private static data.Demographic findOrCreateDemographic(anime_parsing.Demographic dto, EntityManager em) {
        return findOrCreate(
                data.Demographic.class,
                dto,
                em,
                d -> d.malId,
                Demographic::new,
                (x, d) -> {
                    x.setMalId(d.malId);
                    x.setName(d.name);
                    x.setUrl(d.url);
                }
        );
    }

}
