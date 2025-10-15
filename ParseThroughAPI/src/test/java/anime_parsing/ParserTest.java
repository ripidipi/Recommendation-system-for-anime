package anime_parsing;

import exeptions.ParserException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.Persistence;
import mapper.AnimeMapper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ParserTest {

    private static EntityManagerFactory mockEmf;
    private static EntityManager mockEm;
    private static EntityTransaction mockTx;

    @BeforeAll
    static void beforeAll() throws Exception {
        mockEmf = mock(EntityManagerFactory.class);
        mockEm = mock(EntityManager.class);
        mockTx = mock(EntityTransaction.class);

        when(mockEmf.createEntityManager()).thenReturn(mockEm);
        when(mockEm.getTransaction()).thenReturn(mockTx);
        when(mockTx.isActive()).thenReturn(true);

        try (MockedStatic<Persistence> persistenceMock = mockStatic(Persistence.class)) {
            persistenceMock.when(() -> Persistence.createEntityManagerFactory(anyString(), any(Map.class)))
                    .thenReturn(mockEmf);

            Class.forName("anime_parsing.EmfHolder", true, ParserTest.class.getClassLoader());
        }
    }

    @AfterAll
    static void afterAll() {
        mockEmf = null;
        mockEm = null;
        mockTx = null;
    }

    @BeforeEach
    void beforeEach() {
        reset(mockEm, mockTx);
        when(mockEm.getTransaction()).thenReturn(mockTx);
        when(mockTx.isActive()).thenReturn(true);
    }

    @Test
    void skipWhenTitleBlank_shouldNotPerformDbOperations() {
        Anime dto = new Anime();
        dto.title = "   ";
        dto.malId = 1;

        Parser.saveAnimeToDB(dto);

        verify(mockTx, never()).begin();
        verify(mockEm, never()).persist(any());
        verify(mockEm, never()).merge(any());
        verify(mockTx, never()).commit();
        verify(mockEm, never()).close();
    }


    @Test
    void saveNewAnime_shouldPersist_andCommit_andClose() {
        Anime dto = new Anime();
        dto.title = "New Anime";
        dto.malId = 10;

        data.Anime mapped = mock(data.Anime.class);

        when(mockEm.find(data.Anime.class, dto.malId)).thenReturn(null);

        try (MockedStatic<AnimeMapper> mm = mockStatic(AnimeMapper.class)) {
            mm.when(() -> AnimeMapper.map(eq(dto), eq(mockEm))).thenReturn(mapped);

            Parser.saveAnimeToDB(dto);

            verify(mockTx).begin();
            verify(mockEm).persist(mapped);
            verify(mockTx).commit();
            verify(mockEm).close();
            mm.verify(() -> AnimeMapper.map(eq(dto), eq(mockEm)), times(1));
        }
    }

    @Test
    void saveExistingAnime_shouldUpdate_andMerge() {
        Anime dto = new Anime();
        dto.title = "Exist Anime";
        dto.malId = 20;

        data.Anime existing = mock(data.Anime.class);

        when(mockEm.find(data.Anime.class, dto.malId)).thenReturn(existing);

        try (MockedStatic<AnimeMapper> mm = mockStatic(AnimeMapper.class)) {
            Parser.saveAnimeToDB(dto);

            mm.verify(() -> AnimeMapper.update(eq(existing), eq(dto), eq(mockEm)), times(1));
            verify(mockEm).merge(existing);
            verify(mockTx).commit();
            verify(mockEm).close();
        }
    }

    @Test
    void exceptionInMapper_shouldRollback_andThrowParserException() {
        Anime dto = new Anime();
        dto.title = "Bad Anime";
        dto.malId = 30;

        when(mockEm.find(data.Anime.class, dto.malId)).thenReturn(null);

        try (MockedStatic<AnimeMapper> mm = mockStatic(AnimeMapper.class)) {
            mm.when(() -> AnimeMapper.map(eq(dto), eq(mockEm))).thenThrow(new RuntimeException("mapper fail"));

            ParserException ex = assertThrows(ParserException.class, () -> Parser.saveAnimeToDB(dto));
            assertTrue(ex.getMessage().contains("Error saving anime") || ex.getMessage().contains("Persistence error"));

            verify(mockTx).begin();
            verify(mockTx).rollback();
            verify(mockEm).close();
            mm.verify(() -> AnimeMapper.map(eq(dto), eq(mockEm)), times(1));
        }
    }
}
