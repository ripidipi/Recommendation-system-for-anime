package mapper;

import anime_parsing.Anime;
import jakarta.persistence.EntityManager;

public class AnimeMapper {

    public static data.Anime map(Anime dto, EntityManager em) {
        data.Anime entity = new data.Anime();
        entity.setMalId(dto.malId);
        seting(entity, dto, em);

        return entity;
    }

    public static void update(data.Anime entity, Anime dto, EntityManager em) {
        seting(entity, dto, em);
    }

    private static void seting(data.Anime entity, Anime dto, EntityManager em) {
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

        if (dto.producers != null)
            entity.setProducers(dto.producers.stream()
                    .map(p -> findOrCreateProducer(p, em))
                    .toList());

        if (dto.licensors != null)
            entity.setLicensors(dto.licensors.stream()
                    .map(p -> findOrCreateProducer(p, em))
                    .toList());

        if (dto.studios != null)
            entity.setStudios(dto.studios.stream()
                    .map(p -> findOrCreateProducer(p, em))
                    .toList());

        if (dto.genres != null)
            entity.setGenres(dto.genres.stream()
                    .map(g -> findOrCreateGenre(g, em))
                    .toList());

        if (dto.themes != null)
            entity.setThemes(dto.themes.stream()
                    .map(g -> findOrCreateGenre(g, em))
                    .toList());

        if (dto.demographics != null)
            entity.setDemographics(dto.demographics.stream()
                    .map(d -> findOrCreateDemographic(d, em))
                    .toList());
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
