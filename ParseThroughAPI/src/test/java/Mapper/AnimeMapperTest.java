package Mapper;

import AnimeParsing.Anime;
import AnimeParsing.Producer;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnimeMapperTest {

    @Mock
    EntityManager em;

    @Captor
    ArgumentCaptor<Data.Producer> producerCaptor;

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
                .withSeed(seed+1)
                .create();

        when(em.find(Data.Producer.class, dtoProducer.malId)).thenReturn(null);

        var entity = AnimeMapper.map(dto, em);

        try {
            verify(em, atLeastOnce()).persist(producerCaptor.capture());

            List<Data.Producer> allPersisted = producerCaptor.getAllValues();
            Data.Producer persisted = allPersisted.stream()
                    .filter(p -> p.getMalId() == dtoProducer.malId)
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("No persisted producer with malId=" + dtoProducer.malId + " — captured: " + allPersisted));

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

}
