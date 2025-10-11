package mapper;

import anime_parsing.Anime;
import anime_parsing.Demographic;
import anime_parsing.Genre;
import anime_parsing.Producer;
import jakarta.persistence.EntityManager;
import org.instancio.Instancio;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestReporter;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.instancio.Select.field;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnimeMapperTest {

    @Mock
    EntityManager em;

    @Captor
    ArgumentCaptor<data.Producer> producerCaptor;
    @Captor
    ArgumentCaptor<data.Genre> genreCaptor;
    @Captor
    ArgumentCaptor<data.Demographic> demographicCaptor;

    private <T> T createWithSeed(Class<T> classOfObject, long seed) {
        return Instancio.of(classOfObject).withSeed(seed).create();
    }

    @RepeatedTest(10)
    void map_shouldPersistNewProducerWhenNotExists(TestReporter reporter) {
        long seed = new Random().nextLong();
        reporter.publishEntry("seed", Long.toString(seed));

        Producer dtoProducer = createWithSeed(Producer.class, seed);
        Anime dto = Instancio.of(Anime.class)
                .set(field(Anime::getProducers), List.of(dtoProducer))
                .ignore(field(Anime::getStudios))
                .ignore(field(Anime::getLicensors))
                .ignore(field(Anime::getGenres))
                .ignore(field(Anime::getThemes))
                .ignore(field(Anime::getDemographics))
                .withSeed(seed + 1)
                .create();

        when(em.find(data.Producer.class, dtoProducer.malId)).thenReturn(null);

        var entity = AnimeMapper.map(dto, em);

        try {
            verify(em, atLeastOnce()).persist(producerCaptor.capture());

            List<data.Producer> allPersisted = producerCaptor.getAllValues();
            data.Producer persisted = allPersisted.stream()
                    .filter(p -> p.getMalId() == dtoProducer.malId)
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("No persisted producer with malId=" +
                            dtoProducer.malId + " — captured: " + allPersisted));

            assertThat(persisted.getMalId()).isEqualTo(dtoProducer.malId);
            assertThat(persisted.getName()).isEqualTo(dtoProducer.name);
            assertThat(persisted.getUrl()).isEqualTo(dtoProducer.url);

            assertThat(entity.getMalId()).isEqualTo(dto.malId);
            assertThat(entity.getTitle()).isEqualTo(dto.title);
            assertThat(entity.getProducers()).isNotEmpty();
        } catch (AssertionError ae) {
            throw new AssertionError("Failed with seed = " + seed, ae);
        }
    }

    @RepeatedTest(10)
    void map_shouldNotPersistProducerWhenExists(TestReporter reporter) {
        long seed = new Random().nextLong();
        reporter.publishEntry("seed", Long.toString(seed));

        Producer dtoProducer = createWithSeed(Producer.class, seed);
        Anime dto = Instancio.of(Anime.class)
                .set(field(Anime::getProducers), List.of(dtoProducer))
                .ignore(field(Anime::getStudios))
                .ignore(field(Anime::getLicensors))
                .ignore(field(Anime::getGenres))
                .ignore(field(Anime::getThemes))
                .ignore(field(Anime::getDemographics))
                .withSeed(seed + 1)
                .create();

        data.Producer existing = new data.Producer();
        existing.setMalId(dtoProducer.malId);
        existing.setName(dtoProducer.name);
        existing.setUrl(dtoProducer.url);
        existing.setType(dtoProducer.type);

        when(em.find(data.Producer.class, dtoProducer.malId)).thenReturn(existing);

        try {
            var entity = AnimeMapper.map(dto, em);

            verify(em, never()).persist(any(data.Producer.class));

            assertThat(entity.getProducers()).anySatisfy(p -> assertThat(p).isSameAs(existing));

            assertThat(entity.getMalId()).isEqualTo(dto.malId);
            assertThat(entity.getTitle()).isEqualTo(dto.title);
        } catch (AssertionError ae) {
            throw new AssertionError("Failed with seed = " + seed, ae);
        }
    }

    @RepeatedTest(10)
    void map_shouldPersistNewGenreWhenNotExists(TestReporter reporter) {
        long seed = new Random().nextLong();
        reporter.publishEntry("seed", Long.toString(seed));

        Genre dtoGenre = createWithSeed(Genre.class, seed);
        Anime dto = Instancio.of(Anime.class)
                .set(field(Anime::getGenres), List.of(dtoGenre))
                .ignore(field(Anime::getProducers))
                .ignore(field(Anime::getStudios))
                .ignore(field(Anime::getLicensors))
                .ignore(field(Anime::getThemes))
                .ignore(field(Anime::getDemographics))
                .withSeed(seed + 1)
                .create();

        when(em.find(data.Genre.class, dtoGenre.malId)).thenReturn(null);

        var entity = AnimeMapper.map(dto, em);

        try {
            verify(em, atLeastOnce()).persist(genreCaptor.capture());

            List<data.Genre> allPersisted = genreCaptor.getAllValues();
            data.Genre persisted = allPersisted.stream()
                    .filter(p -> p.getMalId() == dtoGenre.malId)
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("No persisted genre with malId=" +
                            dtoGenre.malId + " — captured: " + allPersisted));

            assertThat(persisted.getMalId()).isEqualTo(dtoGenre.malId);
            assertThat(persisted.getName()).isEqualTo(dtoGenre.name);
            assertThat(persisted.getUrl()).isEqualTo(dtoGenre.url);

            assertThat(entity.getMalId()).isEqualTo(dto.malId);
            assertThat(entity.getTitle()).isEqualTo(dto.title);
            assertThat(entity.getGenres()).isNotEmpty();
        } catch (AssertionError ae) {
            throw new AssertionError("Failed with seed = " + seed, ae);
        }
    }

    @RepeatedTest(10)
    void map_shouldNotPersistGenreWhenExists(TestReporter reporter) {
        long seed = new Random().nextLong();
        reporter.publishEntry("seed", Long.toString(seed));

        Genre dtoGenre = createWithSeed(Genre.class, seed);
        Anime dto = Instancio.of(Anime.class)
                .set(field(Anime::getGenres), List.of(dtoGenre))
                .ignore(field(Anime::getProducers))
                .ignore(field(Anime::getStudios))
                .ignore(field(Anime::getLicensors))
                .ignore(field(Anime::getThemes))
                .ignore(field(Anime::getDemographics))
                .withSeed(seed + 1)
                .create();

        data.Genre existing = new data.Genre();
        existing.setMalId(dtoGenre.malId);
        existing.setName(dtoGenre.name);
        existing.setUrl(dtoGenre.url);
        existing.setType(dtoGenre.type);

        when(em.find(data.Genre.class, dtoGenre.malId)).thenReturn(existing);

        try {
            var entity = AnimeMapper.map(dto, em);

            verify(em, never()).persist(any(data.Genre.class));

            assertThat(entity.getGenres()).anySatisfy(p -> assertThat(p).isSameAs(existing));

            assertThat(entity.getMalId()).isEqualTo(dto.malId);
            assertThat(entity.getTitle()).isEqualTo(dto.title);
        } catch (AssertionError ae) {
            throw new AssertionError("Failed with seed = " + seed, ae);
        }
    }

    @RepeatedTest(10)
    void map_shouldPersistNewDemographicWhenNotExists(TestReporter reporter) {
        long seed = new Random().nextLong();
        reporter.publishEntry("seed", Long.toString(seed));

        Demographic dtoDemographic = createWithSeed(Demographic.class, seed);
        Anime dto = Instancio.of(Anime.class)
                .set(field(Anime::getDemographics), List.of(dtoDemographic))
                .ignore(field(Anime::getProducers))
                .ignore(field(Anime::getStudios))
                .ignore(field(Anime::getLicensors))
                .ignore(field(Anime::getThemes))
                .ignore(field(Anime::getGenres))
                .withSeed(seed+1)
                .create();

        when(em.find(data.Demographic.class, dtoDemographic.malId)).thenReturn(null);

        var entity = AnimeMapper.map(dto, em);

        try {
            verify(em, atLeastOnce()).persist(demographicCaptor.capture());

            List<data.Demographic> allPersisted = demographicCaptor.getAllValues();
            data.Demographic persisted = allPersisted.stream()
                    .filter(p -> p.getMalId() == dtoDemographic.malId)
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("No persisted demographic with malId=" +
                            dtoDemographic.malId + " — captured: " + allPersisted));

            assertThat(persisted.getMalId()).isEqualTo(dtoDemographic.malId);
            assertThat(persisted.getName()).isEqualTo(dtoDemographic.name);
            assertThat(persisted.getUrl()).isEqualTo(dtoDemographic.url);

            assertThat(entity.getMalId()).isEqualTo(dto.malId);
            assertThat(entity.getTitle()).isEqualTo(dto.title);
            assertThat(entity.getDemographics()).isNotEmpty();
        } catch (AssertionError ae) {
            throw new AssertionError("Failed with seed = " + seed, ae);
        }
    }

    @RepeatedTest(10)
    void map_shouldNotPersistDemographicWhenExists(TestReporter reporter) {
        long seed = new Random().nextLong();
        reporter.publishEntry("seed", Long.toString(seed));

        Demographic dtoDemographic = createWithSeed(Demographic.class, seed);
        Anime dto = Instancio.of(Anime.class)
                .set(field(Anime::getDemographics), List.of(dtoDemographic))
                .ignore(field(Anime::getProducers))
                .ignore(field(Anime::getStudios))
                .ignore(field(Anime::getLicensors))
                .ignore(field(Anime::getThemes))
                .ignore(field(Anime::getGenres))
                .withSeed(seed + 1)
                .create();

        data.Demographic existing = new data.Demographic();
        existing.setMalId(dtoDemographic.malId);
        existing.setName(dtoDemographic.name);
        existing.setUrl(dtoDemographic.url);

        when(em.find(data.Demographic.class, dtoDemographic.malId)).thenReturn(existing);

        try {
            var entity = AnimeMapper.map(dto, em);

            verify(em, never()).persist(any(data.Demographic.class));

            assertThat(entity.getDemographics()).anySatisfy(p -> assertThat(p).isSameAs(existing));

            assertThat(entity.getMalId()).isEqualTo(dto.malId);
            assertThat(entity.getTitle()).isEqualTo(dto.title);
        } catch (AssertionError ae) {
            throw new AssertionError("Failed with seed = " + seed, ae);
        }
    }

    @Test
    void map_setsSimpleFieldsCorrectly() {
        Anime dto = new Anime();
        dto.malId = 42;
        dto.title = "Test Title";
        dto.url = "https://e";
        dto.score = 8.77;
        dto.year = 2024;
        dto.episodes = 12;
        dto.approved = true;
        dto.titleEnglish = "Test Title EN";
        dto.titleJapanese = "テストタイトル";

        data.Anime entity = AnimeMapper.map(dto, em);

        assertThat(entity).isNotNull();
        assertThat(entity.getMalId()).isEqualTo(42);
        assertThat(entity.getTitle()).isEqualTo("Test Title");
        assertThat(entity.getUrl()).isEqualTo("https://e");
        assertThat(entity.getScore()).isEqualTo(8.77);
        assertThat(entity.getYear()).isEqualTo(2024);
        assertThat(entity.getEpisodes()).isEqualTo(12);
        assertThat(entity.getApproved()).isTrue();
        assertThat(entity.getTitleEnglish()).isEqualTo("Test Title EN");
        assertThat(entity.getTitleJapanese()).isEqualTo("テストタイトル");
    }

    @Test
    void update_doesNotDuplicateExistingProducers() {
        data.Anime existingAnime = new data.Anime();
        existingAnime.setMalId(100);
        data.Producer existingProducer = new data.Producer();
        existingProducer.setMalId(555);
        existingProducer.setName("Existing Prod");
        existingAnime.setProducers(List.of(existingProducer));

        Producer dtoProducer = new Producer();
        dtoProducer.malId = 555;
        dtoProducer.name = "Existing Prod";
        dtoProducer.url = "u";

        Anime dto = new Anime();
        dto.malId = 100;
        dto.title = "Updated Title";
        dto.producers = List.of(dtoProducer);

        when(em.find(data.Producer.class, 555)).thenReturn(existingProducer);

        AnimeMapper.update(existingAnime, dto, em);

        verify(em, never()).persist(any(data.Producer.class));

        assertThat(existingAnime.getProducers()).isNotNull();
        assertThat(existingAnime.getProducers()).containsExactly(existingProducer);

        assertThat(existingAnime.getTitle()).isEqualTo("Updated Title");
    }

}
