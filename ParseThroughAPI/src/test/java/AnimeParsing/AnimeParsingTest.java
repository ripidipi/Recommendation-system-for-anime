package AnimeParsing;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.instancio.Instancio;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestReporter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.within;

class AnimeParsingTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void shouldCreateAnimeWithInstancio() {
        Anime anime = Instancio.of(Anime.class)
                .withSeed(1111L)
                .create();

        assertThat(anime.getTitle()).isNotNull();
        assertThat(anime.getProducers()).isNotNull();
        assertThat(anime.getGenres()).isNotEmpty();
        assertThat(anime.getEpisodes()).isNotNull();
        assertThat(anime.getApproved()).isNotNull();
        assertThat(anime.getMalId()).isNotNull();
        assertThat(anime.getUrl()).isNotNull();
        assertThat(anime.getStatus()).isNotNull();
        assertThat(anime.getScoredBy()).isNotNull();
        assertThat(anime.getRating()).isNotNull();
        assertThat(anime.getLicensors()).isNotNull();
        assertThat(anime.getDemographics()).isNotNull();
    }

    @RepeatedTest(100)
    void instancioRandomSeed(TestReporter reporter) {
        long seed = new Random().nextLong();
        reporter.publishEntry("seed", Long.toString(seed));
        Anime anime = Instancio.of(Anime.class).withSeed(seed).create();

        try {
            assertThat(anime.getTitle()).isNotBlank();
            assertThat(anime.getGenres()).isNotNull().isNotEmpty();
        } catch (AssertionError ae) {
            throw new AssertionError("Seed=" + seed, ae);
        }
    }

    @Test
    void shouldDeserializeSingleAnimeFromJsonFile() throws IOException {
        String json = Files.readString(Paths.get("src/test/resources/json/anime_single.json"));

        Anime anime = mapper.readValue(json, Anime.class);

        assertThat(anime.malId).isEqualTo(52991);
        assertThat(anime.title).isEqualTo("Sousou no Frieren");
        assertThat(anime.titleEnglish).isEqualTo("Frieren: Beyond Journey's End");
        assertThat(anime.titleJapanese).isEqualTo("葬送のフリーレン");
        assertThat(anime.episodes).isEqualTo(28);
        assertThat(anime.score).isCloseTo(9.29, within(1e-4));;

        assertThat(anime.producers).extracting("name")
                .contains("Aniplex", "Dentsu", "TOHO animation");
        assertThat(anime.studios).extracting("name")
                .contains("Madhouse");
        assertThat(anime.genres).extracting("name")
                .contains("Adventure", "Drama", "Fantasy");
    }

    @Test
    void shouldDeserializeTopOfAnimeFromJsonFile() throws IOException {
        String json = Files.readString(Paths.get("src/test/resources/json/anime_top_result.json"));

        AnimeTopResult animeTopResult = mapper.readValue(json, AnimeTopResult.class);

        assertThat(animeTopResult.data).isNotNull();
        assertThat(animeTopResult.data.getFirst().getMalId()).isEqualTo(52991);
        assertThat(animeTopResult.data.getLast().getMalId()).isEqualTo(54492);
    }
}
