package Mapper;

import AnimeParsing.Anime;
import jakarta.persistence.EntityManager;

import java.util.stream.Collectors;

public class AnimeMapper {

    public static Data.Anime map(Anime dto, EntityManager em) {
        Data.Anime entity = new Data.Anime();
        entity.setMalId(dto.mal_id);
        seting(entity, dto, em);

        return entity;
    }

    public static void update(Data.Anime entity, Anime dto, EntityManager em) {
        seting(entity, dto, em);
    }

    private static void seting(Data.Anime entity, Anime dto, EntityManager em) {
        entity.setUrl(dto.url);
        entity.setTitle(dto.title);
        entity.setApproved(dto.approved);
        entity.setTitleEnglish(dto.title_english);
        entity.setTitleJapanese(dto.title_japanese);
        entity.setType(dto.type);
        entity.setEpisodes(dto.episodes);
        entity.setRating(dto.rating);
        entity.setScore(dto.score);
        entity.setScoredBy(dto.scored_by);
        entity.setSynopsis(dto.synopsis);
        entity.setBackground(dto.background);
        entity.setSeason(dto.season);
        entity.setYear(dto.year);

        if (dto.producers != null)
            entity.setProducers(dto.producers.stream()
                    .map(p -> findOrCreateProducer(p, em))
                    .collect(Collectors.toList()));

        if (dto.licensors != null)
            entity.setLicensors(dto.licensors.stream()
                    .map(p -> findOrCreateProducer(p, em))
                    .collect(Collectors.toList()));

        if (dto.studios != null)
            entity.setStudios(dto.studios.stream()
                    .map(p -> findOrCreateProducer(p, em))
                    .collect(Collectors.toList()));

        if (dto.genres != null)
            entity.setGenres(dto.genres.stream()
                    .map(g -> findOrCreateGenre(g, em))
                    .collect(Collectors.toList()));

        if (dto.explicit_genres != null)
            entity.setExplicitGenres(dto.explicit_genres.stream()
                    .map(g -> findOrCreateGenre(g, em))
                    .collect(Collectors.toList()));

        if (dto.themes != null)
            entity.setThemes(dto.themes.stream()
                    .map(g -> findOrCreateGenre(g, em))
                    .collect(Collectors.toList()));

        if (dto.demographics != null)
            entity.setDemographics(dto.demographics.stream()
                    .map(d -> findOrCreateDemographic(d, em))
                    .collect(Collectors.toList()));
    }


    private static Data.Producer findOrCreateProducer(AnimeParsing.Producer dto, EntityManager em) {
        Data.Producer existing = em.find(Data.Producer.class, dto.mal_id);
        if (existing != null) return existing;

        Data.Producer p = new Data.Producer();
        p.setMalId(dto.mal_id);
        p.setName(dto.name);
        p.setType(dto.type);
        p.setUrl(dto.url);
        em.persist(p);
        return p;
    }

    private static Data.Genre findOrCreateGenre(AnimeParsing.Genre dto, EntityManager em) {
        Data.Genre existing = em.find(Data.Genre.class, dto.mal_id);
        if (existing != null) return existing;

        Data.Genre g = new Data.Genre();
        g.setMalId(dto.mal_id);
        g.setName(dto.name);
        g.setType(dto.type);
        g.setUrl(dto.url);
        em.persist(g);
        return g;
    }

    private static Data.Demographic findOrCreateDemographic(AnimeParsing.Demographic dto, EntityManager em) {
        Data.Demographic existing = em.find(Data.Demographic.class, dto.mal_id);
        if (existing != null) return existing;

        Data.Demographic d = new Data.Demographic();
        d.setMalId(dto.mal_id);
        d.setName(dto.name);
        d.setUrl(dto.url);
        em.persist(d);
        return d;
    }
}
