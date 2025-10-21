package mapper;

import user_parsing.UserAnimeEntry;
import data.UserAnimeStat;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserAnimeStatMapperTest {

    private EntityManager em;

    @BeforeEach
    void setUp() {
        em = mock(EntityManager.class);
    }

    @Test
    void skipWhenDtoIsNull_doesNothing() {
        UserAnimeStatMapper.map(null, 42, em);
        verifyNoInteractions(em);
    }

    @Test
    void skipWhenAnimeIdIsNull_doesNothing() {
        UserAnimeEntry dto = new UserAnimeEntry();
        dto.animeId = null;

        UserAnimeStatMapper.map(dto, 7, em);
        verifyNoInteractions(em);
    }

    @Test
    void skipWhenAnimeNotPresent_logsAndDoesNothing() {
        UserAnimeEntry dto = new UserAnimeEntry();
        dto.animeId = 123;

        when(em.find(data.Anime.class, 123)).thenReturn(null);

        UserAnimeStatMapper.map(dto, 10, em);

        verify(em).find(data.Anime.class, 123);
        verify(em, never()).find(eq(UserAnimeStat.class), any());
        verify(em, never()).persist(any());
    }

    @Test
    void createNewStat_persistsEntityWithCorrectFields() {
        UserAnimeEntry dto = new UserAnimeEntry();
        dto.animeId = 10;
        dto.score = 7;
        dto.status = 2;
        dto.numWatchedEpisodes = 5;
        dto.createdAt = Instant.parse("2023-01-02T03:04:05Z").toEpochMilli() / 1000;
        dto.updatedAt = null;

        when(em.find(data.Anime.class, 10)).thenReturn(new data.Anime());
        when(em.find(eq(UserAnimeStat.class), any())).thenReturn(null);

        UserAnimeStatMapper.map(dto, 99, em);

        ArgumentCaptor<UserAnimeStat> captor = ArgumentCaptor.forClass(UserAnimeStat.class);
        verify(em).persist(captor.capture());

        UserAnimeStat persisted = captor.getValue();
        assertThat(persisted).isNotNull();
        assertThat(persisted.getUserId()).isEqualTo(99);
        assertThat(persisted.getAnimeId()).isEqualTo(10);
        assertThat(persisted.getScore()).isEqualTo(7);
        assertThat(persisted.getStatus()).isEqualTo("completed");
        assertThat(persisted.getEpisodesWatched()).isEqualTo(5);

        OffsetDateTime expected = OffsetDateTime.ofInstant(Instant.ofEpochSecond(dto.createdAt), ZoneOffset.UTC);
        assertThat(persisted.getLastUpdated()).isEqualTo(expected);
    }

    @Test
    void updateExistingStat_mutatesManagedEntity_andDoesNotPersistNew() {
        UserAnimeEntry dto = new UserAnimeEntry();
        dto.animeId = 77;
        dto.score = 9;
        dto.status = 1;
        dto.numWatchedEpisodes = 12;
        dto.createdAt = null;
        dto.updatedAt = Instant.parse("2024-05-01T12:00:00Z").toEpochMilli() / 1000;

        when(em.find(data.Anime.class, 77)).thenReturn(new data.Anime());

        UserAnimeStat existing = new UserAnimeStat();
        existing.setUserId(5);
        existing.setAnimeId(77);
        existing.setScore(1);
        when(em.find(eq(UserAnimeStat.class), any())).thenReturn(existing);

        UserAnimeStatMapper.map(dto, 5, em);

        verify(em, never()).persist(any());
        assertThat(existing.getScore()).isEqualTo(9);
        assertThat(existing.getStatus()).isEqualTo("watching");
        assertThat(existing.getEpisodesWatched()).isEqualTo(12);

        OffsetDateTime expected = OffsetDateTime.ofInstant(Instant.ofEpochSecond(dto.updatedAt), ZoneOffset.UTC);
        assertThat(existing.getLastUpdated()).isEqualTo(expected);
    }

    @Test
    void scoreZero_orNegative_becomesNull() {
        UserAnimeEntry dto = new UserAnimeEntry();
        dto.animeId = 55;
        dto.score = 0;
        dto.status = 3;
        dto.createdAt = Instant.parse("2023-02-02T00:00:00Z").toEpochMilli();

        when(em.find(data.Anime.class, 55)).thenReturn(new data.Anime());
        when(em.find(eq(UserAnimeStat.class), any())).thenReturn(null);

        UserAnimeStatMapper.map(dto, 11, em);

        ArgumentCaptor<UserAnimeStat> captor = ArgumentCaptor.forClass(UserAnimeStat.class);
        verify(em).persist(captor.capture());

        UserAnimeStat persisted = captor.getValue();
        assertThat(persisted.getScore()).isNull();
        assertThat(persisted.getStatus()).isEqualTo("on_hold");
    }

    @Test
    void nullTimestamps_fallsBackToNow() {
        UserAnimeEntry dto = new UserAnimeEntry();
        dto.animeId = 200;
        dto.score = 5;
        dto.status = 4;
        dto.createdAt = null;
        dto.updatedAt = null;

        when(em.find(data.Anime.class, 200)).thenReturn(new data.Anime());
        when(em.find(eq(UserAnimeStat.class), any())).thenReturn(null);

        OffsetDateTime before = OffsetDateTime.now(ZoneOffset.UTC);
        UserAnimeStatMapper.map(dto, 3, em);
        OffsetDateTime after = OffsetDateTime.now(ZoneOffset.UTC);

        ArgumentCaptor<UserAnimeStat> captor = ArgumentCaptor.forClass(UserAnimeStat.class);
        verify(em).persist(captor.capture());
        UserAnimeStat persisted = captor.getValue();

        assertThat(persisted.getLastUpdated()).isNotNull();
        assertThat(persisted.getLastUpdated()).isBetween(before.minusSeconds(1), after.plusSeconds(1));
    }

    @Test
    void unknownStatusCode_getsDefaultPrefix() {
        UserAnimeEntry dto = new UserAnimeEntry();
        dto.animeId = 300;
        dto.score = 8;
        dto.status = 99;
        dto.createdAt = Instant.parse("2022-12-12T12:12:12Z").toEpochMilli();

        when(em.find(data.Anime.class,300)).thenReturn(new data.Anime());
        when(em.find(eq(UserAnimeStat.class), any())).thenReturn(null);

        UserAnimeStatMapper.map(dto, 77, em);

        ArgumentCaptor<UserAnimeStat> captor = ArgumentCaptor.forClass(UserAnimeStat.class);
        verify(em).persist(captor.capture());

        UserAnimeStat persisted = captor.getValue();
        assertThat(persisted.getStatus()).isEqualTo("status_99");
    }
}
