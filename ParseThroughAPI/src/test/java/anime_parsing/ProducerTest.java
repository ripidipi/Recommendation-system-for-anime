package anime_parsing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ProducerTest {

    @Test
    void gettersReturnValuesAssignedToPublicFields() {
        Producer p = new Producer();
        p.malId = 42;
        p.type = "studio";
        p.name = "Studio Example";
        p.url = "https://example.org/producer/42";

        assertAll("Producer getters",
                () -> assertEquals(42, p.getMalId()),
                () -> assertEquals("studio", p.getType()),
                () -> assertEquals("Studio Example", p.getName()),
                () -> assertEquals("https://example.org/producer/42", p.getUrl())
        );
    }

    @Test
    void jsonDeserializationAndAnnotations() throws Exception {
        String json = """
            {
              "mal_id": 777,
              "type": "producer",
              "name": "Instancio Studio",
              "url": "https://example.test/prod/777"
            }
            """;

        ObjectMapper mapper = new ObjectMapper();
        Producer p = mapper.readValue(json, Producer.class);

        assertAll("Producer JSON -> object",
                () -> assertEquals(777, p.getMalId()),
                () -> assertEquals("producer", p.getType()),
                () -> assertEquals("Instancio Studio", p.getName()),
                () -> assertEquals("https://example.test/prod/777", p.getUrl())
        );

        assertTrue(Producer.class.isAnnotationPresent(JsonIgnoreProperties.class),
                "Producer should have @JsonIgnoreProperties");

        JsonNaming naming = Producer.class.getAnnotation(JsonNaming.class);
        assertNotNull(naming, "Producer should have @JsonNaming");
        assertEquals(PropertyNamingStrategies.SnakeCaseStrategy.class, naming.value(),
                "@JsonNaming should use SnakeCaseStrategy");
    }
}
