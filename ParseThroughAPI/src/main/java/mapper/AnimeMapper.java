package mapper;

import anime_parsing.Anime;
import data.Demographic;
import jakarta.persistence.EntityManager;

import java.util.*;
import java.util.stream.Collectors;

public class AnimeMapper {

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
                        dto.producers.stream().map(p -> findOrCreateProducer(p, em)).collect(Collectors.toList()),
                entity::setProducers);

        updateCollection(entity.getLicensors(),
                dto.licensors == null ? List.of() :
                        dto.licensors.stream().map(p -> findOrCreateProducer(p, em)).collect(Collectors.toList()),
                entity::setLicensors);

        updateCollection(entity.getStudios(),
                dto.studios == null ? List.of() :
                        dto.studios.stream().map(p -> findOrCreateProducer(p, em)).collect(Collectors.toList()),
                entity::setStudios);

        updateCollection(entity.getGenres(),
                dto.genres == null ? List.of() :
                        dto.genres.stream().map(g -> findOrCreateGenre(g, em)).collect(Collectors.toList()),
                entity::setGenres);

        updateCollection(entity.getThemes(),
                dto.themes == null ? List.of() :
                        dto.themes.stream().map(g -> findOrCreateGenre(g, em)).collect(Collectors.toList()),
                entity::setThemes);

        updateCollection(entity.getDemographics(),
                dto.demographics == null ? List.of() :
                        dto.demographics.stream().map(d -> findOrCreateDemographic(d, em)).collect(Collectors.toList()),
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

    private static data.Producer findOrCreateProducer(anime_parsing.Producer dto, EntityManager em) {
        data.Producer existing = em.find(data.Producer.class, dto.malId);
        if (existing != null) return existing;
        data.Producer p = new data.Producer();
        p.setMalId(dto.malId);
        p.setName(dto.name);
        p.setType(dto.type);
        p.setUrl(dto.url);
        em.persist(p);
        return p;
    }

    private static data.Genre findOrCreateGenre(anime_parsing.Genre dto, EntityManager em) {
        data.Genre existing = em.find(data.Genre.class, dto.malId);
        if (existing != null) return existing;
        data.Genre g = new data.Genre();
        g.setMalId(dto.malId);
        g.setName(dto.name);
        g.setType(dto.type);
        g.setUrl(dto.url);
        em.persist(g);
        return g;
    }

    private static data.Demographic findOrCreateDemographic(anime_parsing.Demographic dto, EntityManager em) {
        data.Demographic existing = em.find(data.Demographic.class, dto.malId);
        if (existing != null) return existing;
        data.Demographic d = new data.Demographic();
        d.setMalId(dto.malId);
        d.setName(dto.name);
        d.setUrl(dto.url);
        em.persist(d);
        return d;
    }
}
