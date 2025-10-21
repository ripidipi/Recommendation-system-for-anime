package mapper;

import anime_parsing.Anime;
import anime_parsing.Demographic;
import anime_parsing.Genre;
import anime_parsing.Producer;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import org.instancio.Instancio;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestReporter;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.instancio.Select.field;
import static org.junit.jupiter.api.Assertions.*;
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
            assertAll(
                    () -> assertThat(persisted.getMalId()).isEqualTo(dtoProducer.malId),
                    () -> assertThat(persisted.getName()).isEqualTo(dtoProducer.name),
                    () -> assertThat(persisted.getUrl()).isEqualTo(dtoProducer.url),

                    () -> assertThat(entity.getMalId()).isEqualTo(dto.malId),
                    () -> assertThat(entity.getTitle()).isEqualTo(dto.title),
                    () -> assertThat(entity.getProducers()).isNotEmpty()
            );
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
            assertAll(
                    () -> assertThat(persisted.getMalId()).isEqualTo(dtoGenre.malId),
                    () -> assertThat(persisted.getName()).isEqualTo(dtoGenre.name),
                    () -> assertThat(persisted.getUrl()).isEqualTo(dtoGenre.url),

                    () -> assertThat(entity.getMalId()).isEqualTo(dto.malId),
                    () -> assertThat(entity.getTitle()).isEqualTo(dto.title),
                    () -> assertThat(entity.getGenres()).isNotEmpty()
            );
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

            assertAll(
                    () -> assertThat(persisted.getMalId()).isEqualTo(dtoDemographic.malId),
                    () -> assertThat(persisted.getName()).isEqualTo(dtoDemographic.name),
                    () -> assertThat(persisted.getUrl()).isEqualTo(dtoDemographic.url),

                    () -> assertThat(entity.getMalId()).isEqualTo(dto.malId),
                    () -> assertThat(entity.getTitle()).isEqualTo(dto.title),
                    () -> assertThat(entity.getDemographics()).isNotEmpty()
            );
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
        assertAll(
                () -> assertThat(entity).isNotNull(),
                () -> assertThat(entity.getMalId()).isEqualTo(42),
                () -> assertThat(entity.getTitle()).isEqualTo("Test Title"),
                () -> assertThat(entity.getUrl()).isEqualTo("https://e"),
                () -> assertThat(entity.getScore()).isEqualTo(8.77),
                () -> assertThat(entity.getYear()).isEqualTo(2024),
                () -> assertThat(entity.getEpisodes()).isEqualTo(12),
                () -> assertThat(entity.getApproved()).isTrue(),
                () -> assertThat(entity.getTitleEnglish()).isEqualTo("Test Title EN"),
                () -> assertThat(entity.getTitleJapanese()).isEqualTo("テストタイトル")
        );
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

    @Test
    void map_resultCollectionsAreMutable() {
        Producer p = new Producer();
        p.malId = 1;
        p.name = "P";
        p.url = "u";

        Genre g = new Genre();
        g.malId = 2;
        g.name = "G";
        g.url = "u";

        Demographic d = new Demographic();
        d.malId = 3;
        d.name = "D";
        d.url = "u";

        Anime dto = new Anime();
        dto.malId = 10;
        dto.title = "T";
        dto.producers = List.of(p);
        dto.genres = List.of(g);
        dto.demographics = List.of(d);

        when(em.find(data.Producer.class, 1)).thenReturn(null);
        when(em.find(data.Genre.class, 2)).thenReturn(null);
        when(em.find(data.Demographic.class, 3)).thenReturn(null);

        data.Anime entity = AnimeMapper.map(dto, em);

        assertNotNull(entity.getProducers());
        assertNotNull(entity.getGenres());
        assertNotNull(entity.getDemographics());

        entity.getProducers().clear();
        entity.getProducers().add(new data.Producer());

        entity.getGenres().clear();
        entity.getGenres().add(new data.Genre());

        entity.getDemographics().clear();
        entity.getDemographics().add(new data.Demographic());

        assertThat(entity.getProducers()).hasSize(1);
        assertThat(entity.getGenres()).hasSize(1);
        assertThat(entity.getDemographics()).hasSize(1);
    }

    @Test
    void update_mutatesExistingCollectionsRatherThanReplacingReference() {
        data.Anime existing = new data.Anime();
        existing.setMalId(200);

        List<data.Producer> originalList = new ArrayList<>();
        data.Producer ep = new data.Producer();
        ep.setMalId(777);
        originalList.add(ep);
        existing.setProducers(originalList);

        Producer dtoP = new Producer();
        dtoP.malId = 888;
        dtoP.name = "New";
        dtoP.url = "u";

        Anime dto = new Anime();
        dto.malId = 200;
        dto.title = "New Title";
        dto.producers = List.of(dtoP);

        when(em.find(data.Producer.class, 888)).thenReturn(null);

        AnimeMapper.update(existing, dto, em);

        assertSame(originalList, existing.getProducers(), "Producers collection reference must remain same");
        assertThat(existing.getProducers()).extracting(data.Producer::getMalId).contains(888);
        assertThat(existing.getTitle()).isEqualTo("New Title");
    }

    @Test
    void map_handlesNullCollectionsByInitializingEmptyMutableCollections() {
        Anime dto = new Anime();
        dto.malId = 500;
        dto.title = "Null Collections Test";
        dto.producers = null;
        dto.genres = null;
        dto.demographics = null;

        data.Anime entity = AnimeMapper.map(dto, em);

        assertNotNull(entity.getProducers());
        assertNotNull(entity.getGenres());
        assertNotNull(entity.getDemographics());

        assertThat(entity.getProducers()).isEmpty();
        assertThat(entity.getGenres()).isEmpty();
        assertThat(entity.getDemographics()).isEmpty();

        entity.getProducers().add(new data.Producer());
        assertThat(entity.getProducers()).hasSize(1);
    }

    @Test
    void map_propagatesExceptionWhenEntityManagerFindFails() {
        Producer p = new Producer();
        p.malId = 999;
        p.name = "X";

        Anime dto = new Anime();
        dto.malId = 1;
        dto.title = "Should fail";
        dto.producers = List.of(p);

        when(em.find(data.Producer.class, 999)).thenThrow(new PersistenceException("db down"));

        assertThrows(PersistenceException.class, () -> AnimeMapper.map(dto, em));
    }

    @Test
    void map_propagatesExceptionWhenEntityManagerPersistFails() {
        Producer p = new Producer();
        p.malId = 42;
        p.name = "Y";

        Anime dto = new Anime();
        dto.malId = 2;
        dto.title = "Persist fail";
        dto.producers = List.of(p);

        when(em.find(data.Producer.class, 42)).thenReturn(null);
        doThrow(new PersistenceException("persist failed")).when(em).persist(any(data.Producer.class));

        assertThrows(PersistenceException.class, () -> AnimeMapper.map(dto, em));
    }
}
