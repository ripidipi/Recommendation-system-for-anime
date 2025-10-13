package anime_parsing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AnimeTopResultTest {

    @Test
    void deserializationProducesListOfAnime() throws Exception {
        String json = """
            {
              "data": [
                {
                  "mal_id": 1,
                  "url": "https://example.test/anime/1",
                  "title": "Top Anime One"
                },
                {
                  "mal_id": 2,
                  "url": "https://example.test/anime/2",
                  "title": "Top Anime Two"
                }
              ]
            }
            """;

        ObjectMapper mapper = new ObjectMapper();
        AnimeTopResult top = mapper.readValue(json, AnimeTopResult.class);

        assertAll("AnimeTopResult deserialization",
                () -> assertNotNull(top, "AnimeTopResult should not be null"),
                () -> assertNotNull(top.data, "data list should not be null"),
                () -> assertEquals(2, top.data.size(), "data should contain two entries")
        );

        Anime first = top.data.get(0);
        assertAll("First anime item",
                () -> assertEquals(1, first.getMalId()),
                () -> assertEquals("Top Anime One", first.getTitle()),
                () -> assertEquals("https://example.test/anime/1", first.getUrl())
        );

        assertTrue(AnimeTopResult.class.isAnnotationPresent(JsonIgnoreProperties.class),
                "AnimeTopResult should have @JsonIgnoreProperties");
    }

    @Test
    void emptyDataDeserializesToEmptyListOrNullDependingOnJson() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        String jsonWithEmptyArray = "{ \"data\": [] }";
        AnimeTopResult withEmpty = mapper.readValue(jsonWithEmptyArray, AnimeTopResult.class);
        assertNotNull(withEmpty);
        assertNotNull(withEmpty.data);
        assertTrue(withEmpty.data.isEmpty(), "data should be empty list when JSON has []");

        String jsonWithoutData = "{}";
        AnimeTopResult without = mapper.readValue(jsonWithoutData, AnimeTopResult.class);
        assertNotNull(without);
        // depending on Jackson defaults and class definition, data may be null
        // we assert the behavior explicitly so tests are clear about expected outcome
        assertNull(without.data, "data should be null when JSON does not contain 'data' field");
    }
}
