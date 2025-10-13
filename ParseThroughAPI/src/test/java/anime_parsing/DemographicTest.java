package anime_parsing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DemographicTest {

    @Test
    void publicFieldsAndDefaults() {
        Demographic d = new Demographic();
        d.malId = 7;
        d.name = "Shounen";
        d.url = "https://example.test/demographic/7";

        assertAll("Demographic public fields",
                () -> assertEquals(7, d.malId),
                () -> assertEquals("Shounen", d.name),
                () -> assertEquals("https://example.test/demographic/7", d.url)
        );

        Demographic empty = new Demographic();
        assertAll("Default values for new Demographic instance",
                () -> assertEquals(0, empty.malId),
                () -> assertNull(empty.name),
                () -> assertNull(empty.url)
        );
    }

    @Test
    void jsonDeserializationAndAnnotations() throws Exception {
        String json = """
            {
              "mal_id": 314,
              "name": "Seinen",
              "url": "https://example.test/demographic/314"
            }
            """;

        ObjectMapper mapper = new ObjectMapper();
        Demographic d = mapper.readValue(json, Demographic.class);

        assertAll("Demographic JSON -> object",
                () -> assertEquals(314, d.malId),
                () -> assertEquals("Seinen", d.name),
                () -> assertEquals("https://example.test/demographic/314", d.url)
        );

        assertTrue(Demographic.class.isAnnotationPresent(JsonIgnoreProperties.class),
                "Demographic should have @JsonIgnoreProperties");

        JsonNaming naming = Demographic.class.getAnnotation(JsonNaming.class);
        assertNotNull(naming, "Demographic should have @JsonNaming");
        assertEquals(PropertyNamingStrategies.SnakeCaseStrategy.class, naming.value(),
                "@JsonNaming should use SnakeCaseStrategy");
    }
}
