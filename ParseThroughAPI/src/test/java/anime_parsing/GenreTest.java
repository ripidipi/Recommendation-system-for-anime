package anime_parsing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GenreTest {

    @Test
    void fieldsAndDefaults() {
        Genre g = new Genre();
        g.malId = 5;
        g.type = "genre";
        g.name = "Action";
        g.url = "https://example.test/genre/5";

        assertAll("Genre fields",
                () -> assertEquals(5, g.malId),
                () -> assertEquals("genre", g.type),
                () -> assertEquals("Action", g.name),
                () -> assertEquals("https://example.test/genre/5", g.url)
        );

        Genre empty = new Genre();
        assertAll("Default values for new Genre instance",
                () -> assertEquals(0, empty.malId),
                () -> assertNull(empty.type),
                () -> assertNull(empty.name),
                () -> assertNull(empty.url)
        );
    }

    @Test
    void jsonDeserializationAndAnnotations() throws Exception {
        String json = """
            {
              "mal_id": 99,
              "type": "genre",
              "name": "Drama",
              "url": "https://example.test/genre/99"
            }
            """;

        ObjectMapper mapper = new ObjectMapper();
        Genre g = mapper.readValue(json, Genre.class);

        assertAll("Genre JSON -> object",
                () -> assertEquals(99, g.malId),
                () -> assertEquals("genre", g.type),
                () -> assertEquals("Drama", g.name),
                () -> assertEquals("https://example.test/genre/99", g.url)
        );

        assertTrue(Genre.class.isAnnotationPresent(JsonIgnoreProperties.class),
                "Genre should have @JsonIgnoreProperties");

        JsonNaming naming = Genre.class.getAnnotation(JsonNaming.class);
        assertNotNull(naming, "Genre should have @JsonNaming");
        assertEquals(PropertyNamingStrategies.SnakeCaseStrategy.class, naming.value(),
                "@JsonNaming should use SnakeCaseStrategy");
    }
}
