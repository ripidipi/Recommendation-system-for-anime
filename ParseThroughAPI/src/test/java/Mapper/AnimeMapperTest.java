package Mapper;

import AnimeParsing.Anime;
import AnimeParsing.Demographic;
import AnimeParsing.Genre;
import AnimeParsing.Producer;
import jakarta.persistence.EntityManager;
import org.instancio.Instancio;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.TestReporter;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.instancio.Select.field;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnimeMapperTest {

    @Mock
    EntityManager em;

    @Captor
    ArgumentCaptor<Data.Producer> producerCaptor;
    @Captor
    ArgumentCaptor<Data.Genre> genreCaptor;
    @Captor
    ArgumentCaptor<Data.Demographic> demographicCaptor;

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
                .withSeed(seed + 1)
                .create();

        when(em.find(Data.Producer.class, dtoProducer.malId)).thenReturn(null);

        var entity = AnimeMapper.map(dto, em);

        try {
            verify(em, atLeastOnce()).persist(producerCaptor.capture());

            List<Data.Producer> allPersisted = producerCaptor.getAllValues();
            Data.Producer persisted = allPersisted.stream()
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
                .withSeed(seed + 1)
                .create();

        Data.Producer existing = new Data.Producer();
        existing.setMalId(dtoProducer.malId);
        existing.setName(dtoProducer.name);
        existing.setUrl(dtoProducer.url);
        existing.setType(dtoProducer.type);


        when(em.find(Data.Producer.class, dtoProducer.malId)).thenReturn(existing);

        try {
            var entity = AnimeMapper.map(dto, em);

            verify(em, never()).persist(any(Data.Producer.class));

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
                .withSeed(seed + 1)
                .create();

        when(em.find(Data.Genre.class, dtoGenre.malId)).thenReturn(null);

        var entity = AnimeMapper.map(dto, em);

        try {
            verify(em, atLeastOnce()).persist(genreCaptor.capture());

            List<Data.Genre> allPersisted = genreCaptor.getAllValues();
            Data.Genre persisted = allPersisted.stream()
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
                .withSeed(seed + 1)
                .create();

        Data.Genre existing = new Data.Genre();
        existing.setMalId(dtoGenre.malId);
        existing.setName(dtoGenre.name);
        existing.setUrl(dtoGenre.url);
        existing.setType(dtoGenre.type);


        when(em.find(Data.Genre.class, dtoGenre.malId)).thenReturn(existing);

        try {
            var entity = AnimeMapper.map(dto, em);

            verify(em, never()).persist(any(Data.Genre.class));

            assertThat(entity.getGenres()).anySatisfy(p -> assertThat(p).isSameAs(existing));

            assertThat(entity.getMalId()).isEqualTo(dto.malId);
            assertThat(entity.getTitle()).isEqualTo(dto.title);
        } catch (AssertionError ae) {
            throw new AssertionError("Failed with seed = " + seed, ae);
        }
    }

    @RepeatedTest(10)
    @MockitoSettings(strictness = Strictness.LENIENT)
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

        when(em.find(Data.Demographic.class, dtoDemographic.malId)).thenReturn(null);

        var entity = AnimeMapper.map(dto, em);

        try {
            verify(em, atLeastOnce()).persist(demographicCaptor.capture());

            List<Data.Demographic> allPersisted = demographicCaptor.getAllValues();
            Data.Demographic persisted = allPersisted.stream()
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

        Data.Demographic existing = new Data.Demographic();
        existing.setMalId(dtoDemographic.malId);
        existing.setName(dtoDemographic.name);
        existing.setUrl(dtoDemographic.url);

        when(em.find(Data.Demographic.class, dtoDemographic.malId)).thenReturn(existing);

        try {
            var entity = AnimeMapper.map(dto, em);

            verify(em, never()).persist(any(Data.Demographic.class));

            assertThat(entity.getDemographics()).anySatisfy(p -> assertThat(p).isSameAs(existing));

            assertThat(entity.getMalId()).isEqualTo(dto.malId);
            assertThat(entity.getTitle()).isEqualTo(dto.title);
        } catch (AssertionError ae) {
            throw new AssertionError("Failed with seed = " + seed, ae);
        }
    }

}
