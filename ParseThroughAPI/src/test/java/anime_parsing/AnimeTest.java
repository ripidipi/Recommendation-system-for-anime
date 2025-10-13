package anime_parsing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AnimeTest {

    @Test
    void gettersReturnValuesAssignedToPublicFields() {
        Anime a = new Anime();

        a.malId = 101;
        a.url = "https://example.test/anime/101";
        a.status = "Finished Airing";
        a.title = "My Title";
        a.approved = Boolean.TRUE;
        a.titleEnglish = "My Title EN";
        a.titleJapanese = "テスト";
        a.type = "TV";
        a.episodes = 12;
        a.rating = "PG-13";
        a.score = 7.5;
        a.scoredBy = 1234;
        a.synopsis = "synopsis";
        a.background = "background";
        a.season = "Winter";
        a.year = 2020;

        List<Producer> producers = List.of();
        List<Producer> licensors = List.of();
        List<Producer> studios = List.of();
        List<Genre> genres = List.of();
        List<Genre> themes = List.of();
        List<Demographic> demographics = List.of();

        a.producers = producers;
        a.licensors = licensors;
        a.studios = studios;
        a.genres = genres;
        a.themes = themes;
        a.demographics = demographics;

        assertAll("Test of getters and links",
                () -> assertEquals(101, a.getMalId()),
                () -> assertEquals("https://example.test/anime/101", a.getUrl()),
                () -> assertEquals("Finished Airing", a.getStatus()),
                () -> assertEquals("My Title", a.getTitle()),
                () -> assertTrue(a.getApproved()),
                () -> assertEquals("My Title EN", a.getTitleEnglish()),
                () -> assertEquals("テスト", a.getTitleJapanese()),
                () -> assertEquals("TV", a.getType()),
                () -> assertEquals(12, a.getEpisodes()),
                () -> assertEquals("PG-13", a.getRating()),
                () -> assertEquals(7.5, a.getScore()),
                () -> assertEquals(1234, a.getScoredBy()),
                () -> assertEquals("synopsis", a.getSynopsis()),
                () -> assertEquals("background", a.getBackground()),
                () -> assertEquals("Winter", a.getSeason()),
                () -> assertEquals(2020, a.getYear()),

                () -> assertSame(producers, a.getProducers()),
                () -> assertSame(licensors, a.getLicensors()),
                () -> assertSame(studios, a.getStudios()),
                () -> assertSame(genres, a.getGenres()),
                () -> assertSame(themes, a.getThemes()),
                () -> assertSame(demographics, a.getDemographics())
        );
    }

    @Test
    void defaultNewInstanceHasNullFields() {
        Anime a = new Anime();

        assertAll(
                () -> assertNull(a.getMalId(), "malId expected null"),
                () -> assertNull(a.getUrl(), "url expected null"),
                () -> assertNull(a.getTitle(), "title expected null"),
                () -> assertNull(a.getProducers(), "producers expected null"),
                () -> assertNull(a.getGenres(), "genres expected null")
        );
    }

    @Test
    void jsonDeserializationUsesSnakeCaseNamingStrategy() throws Exception {
        String json = """
            {
              "mal_id": 777,
              "url": "https://example.test/anime/777",
              "status": "Currently Airing",
              "title": "Snake Case Test",
              "approved": false,
              "title_english": "Snake EN",
              "title_japanese": "ジャパン",
              "type": "OVA",
              "episodes": 3,
              "rating": "R",
              "score": 9.1,
              "scored_by": 500,
              "synopsis": "desc",
              "background": "bg",
              "season": "Spring",
              "year": 2025,
              "producers": [],
              "licensors": [],
              "studios": [],
              "genres": [],
              "themes": [],
              "demographics": []
            }
            """;

        ObjectMapper mapper = new ObjectMapper();
        Anime a = mapper.readValue(json, Anime.class);

        assertAll(
                () -> assertEquals(777, a.getMalId()),
                () -> assertEquals("https://example.test/anime/777", a.getUrl()),
                () -> assertEquals("Currently Airing", a.getStatus()),
                () -> assertEquals("Snake Case Test", a.getTitle()),
                () -> assertFalse(a.getApproved()),
                () -> assertEquals("Snake EN", a.getTitleEnglish()),
                () -> assertEquals("ジャパン", a.getTitleJapanese()),
                () -> assertEquals("OVA", a.getType()),
                () -> assertEquals(3, a.getEpisodes()),
                () -> assertEquals("R", a.getRating()),
                () -> assertEquals(9.1, a.getScore()),
                () -> assertEquals(500, a.getScoredBy()),
                () -> assertEquals("desc", a.getSynopsis()),
                () -> assertEquals("bg", a.getBackground()),
                () -> assertEquals("Spring", a.getSeason()),
                () -> assertEquals(2025, a.getYear()),

                () -> assertNotNull(a.getProducers()),
                () -> assertTrue(a.getProducers().isEmpty()),
                () -> assertNotNull(a.getGenres()),
                () -> assertTrue(a.getGenres().isEmpty())
        );
    }

    @Test
    void classHasJacksonAnnotations() {
        Class<Anime> clazz = Anime.class;

        assertAll("Check of JSON annotations",
                () -> assertTrue(clazz.isAnnotationPresent(com.fasterxml.jackson.annotation.JsonIgnoreProperties.class),
                        "should be @JsonIgnoreProperties"),
                () -> {
                    com.fasterxml.jackson.databind.annotation.JsonNaming naming =
                            clazz.getAnnotation(com.fasterxml.jackson.databind.annotation.JsonNaming.class);
                    assertNotNull(naming, "@JsonNaming");
                    assertEquals(PropertyNamingStrategies.SnakeCaseStrategy.class, naming.value(),
                            "JsonNaming should have PropertyNamingStrategies.SnakeCaseStrategy");
                }
        );
    }
}
