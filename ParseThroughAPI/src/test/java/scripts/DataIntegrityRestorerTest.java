package scripts;

import data.UserStat;
import data.Users;
import mapper.UserStatMapper;
import mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import user_parsing.FetchUsers;
import user_parsing.Parser;
import user_parsing.StatsData;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;

import java.math.BigInteger;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DataIntegrityRestorerTest {

    @Test
    void processUserById_nullMalId_noException() {
        try (MockedStatic<Parser> parserStatic = mockStatic(Parser.class)) {
            EntityManagerFactory emf = mock(EntityManagerFactory.class);
            parserStatic.when(Parser::getEmf).thenReturn(emf);

            DataIntegrityRestorer restorer = new DataIntegrityRestorer(0.5, 10, 10);
            assertDoesNotThrow(() -> restorer.processUserById(null));
        }
    }

    @Test
    void run_batches_processed() {
        try (MockedStatic<Parser> parserStatic = mockStatic(Parser.class)) {
            EntityManagerFactory emf = mock(EntityManagerFactory.class);
            parserStatic.when(Parser::getEmf).thenReturn(emf);

            EntityManager em = mock(EntityManager.class);
            @SuppressWarnings("unchecked")
            TypedQuery<Users> query = mock(TypedQuery.class);
            when(em.createQuery(startsWith("SELECT u FROM Users"), eq(Users.class))).thenReturn(query);
            when(query.setFirstResult(anyInt())).thenReturn(query);
            when(query.setMaxResults(anyInt())).thenReturn(query);

            Users u1 = mock(Users.class);
            when(u1.getMalId()).thenReturn(1);
            when(u1.getUsername()).thenReturn("a");
            Users u2 = mock(Users.class);
            when(u2.getMalId()).thenReturn(2);
            when(u2.getUsername()).thenReturn("b");

            when(query.getResultList()).thenReturn(List.of(u1, u2)).thenReturn(Collections.emptyList());
            when(emf.createEntityManager()).thenReturn(em);

            DataIntegrityRestorer real = new DataIntegrityRestorer(0.5, 10, 10);
            DataIntegrityRestorer spy = Mockito.spy(real);
            doNothing().when(spy).processUserById(anyInt());

            spy.run();

            verify(spy, times(1)).processUserById(1);
            verify(spy, times(1)).processUserById(2);
        }
    }

    @Test
    void processUserById_userNotFound_noResync() {
        try (MockedStatic<Parser> parserStatic = mockStatic(Parser.class);
             MockedConstruction<UserResyncService> resyncConstruction = mockConstruction(UserResyncService.class)) {

            EntityManagerFactory emf = mock(EntityManagerFactory.class);
            parserStatic.when(Parser::getEmf).thenReturn(emf);

            EntityManager em1 = mock(EntityManager.class);
            EntityTransaction tx1 = mock(EntityTransaction.class);
            when(em1.getTransaction()).thenReturn(tx1);
            when(tx1.isActive()).thenReturn(false);

            when(em1.find(eq(Users.class), eq(123))).thenReturn(null);

            Query nativeQuery = mock(Query.class);
            when(em1.createNativeQuery(anyString())).thenReturn(nativeQuery);
            when(nativeQuery.setParameter(eq(1), any())).thenReturn(nativeQuery);
            when(nativeQuery.getSingleResult()).thenReturn(BigInteger.ZERO);

            when(emf.createEntityManager()).thenReturn(em1);

            DataIntegrityRestorer restorer = new DataIntegrityRestorer(0.5, 10, 10);
            UserResyncService created = resyncConstruction.constructed().get(0);

            assertDoesNotThrow(() -> restorer.processUserById(123));
            verifyNoInteractions(created);
        }
    }

    @Test
    void thresholdExceeded_resyncFails_deleteOnFailureTrue() {
        try (MockedStatic<Parser> parserStatic = mockStatic(Parser.class);
             MockedConstruction<UserResyncService> resyncConstruction = mockConstruction(UserResyncService.class,
                     (mock, ctx) -> when(mock.resyncUserUpsertFetchWithRetries(anyString(), anyInt(), anyInt(), anyLong())).thenReturn(false))) {

            EntityManagerFactory emf = mock(EntityManagerFactory.class);
            parserStatic.when(Parser::getEmf).thenReturn(emf);

            EntityManager em1 = mock(EntityManager.class);
            EntityTransaction tx1 = mock(EntityTransaction.class);
            when(em1.getTransaction()).thenReturn(tx1);

            Users user = mock(Users.class);
            when(user.getUsername()).thenReturn("testuser");
            when(user.getMalId()).thenReturn(321);
            when(user.getUrl()).thenReturn("http://example");
            when(em1.find(Users.class, 321)).thenReturn(user);

            Query nativeQuery = mock(Query.class);
            when(em1.createNativeQuery(startsWith("SELECT COUNT(*)"))).thenReturn(nativeQuery);
            when(nativeQuery.setParameter(eq(1), any())).thenReturn(nativeQuery);
            when(nativeQuery.getSingleResult()).thenReturn(BigInteger.valueOf(20));

            UserStat stat = mock(UserStat.class);
            when(stat.getTotalEntries()).thenReturn(1);
            @SuppressWarnings("unchecked")
            TypedQuery<UserStat> statQuery = mock(TypedQuery.class);
            when(em1.createQuery(startsWith("SELECT s FROM UserStat"), eq(UserStat.class))).thenReturn(statQuery);
            when(statQuery.setParameter(eq("u"), any())).thenReturn(statQuery);
            when(statQuery.getResultStream()).thenReturn(Stream.of(stat));

            when(emf.createEntityManager()).thenReturn(em1);

            DataIntegrityRestorer restorer = new DataIntegrityRestorer(0.5, 10, 10, true);
            UserResyncService created = resyncConstruction.constructed().get(0);

            restorer.processUserById(321);

            verify(created, times(1)).resyncUserUpsertFetchWithRetries("testuser", 321, 3, 500);
            verify(created, times(1)).deleteUserData(321);
        }
    }

    @Test
    void thresholdExceeded_resyncSucceeds_updatesUserStat() {
        try (MockedStatic<Parser> parserStatic = mockStatic(Parser.class);
             MockedConstruction<UserResyncService> resyncConstruction = mockConstruction(UserResyncService.class,
                     (mock, ctx) -> when(mock.resyncUserUpsertFetchWithRetries(anyString(), anyInt(), anyInt(), anyLong())).thenReturn(true));
             MockedStatic<FetchUsers> fetchUsersStatic = mockStatic(FetchUsers.class);
             MockedStatic<UserStatMapper> userStatMapperStatic = mockStatic(UserStatMapper.class);
             MockedStatic<UserMapper> userMapperStatic = mockStatic(UserMapper.class)) {

            EntityManagerFactory emf = mock(EntityManagerFactory.class);
            parserStatic.when(Parser::getEmf).thenReturn(emf);

            EntityManager em1 = mock(EntityManager.class);
            EntityTransaction tx1 = mock(EntityTransaction.class);
            when(em1.getTransaction()).thenReturn(tx1);

            Users user = mock(Users.class);
            when(user.getUsername()).thenReturn("gooduser");
            when(user.getMalId()).thenReturn(777);
            when(user.getUrl()).thenReturn("http://profile");
            when(em1.find(Users.class, 777)).thenReturn(user);

            Query nativeQuery = mock(Query.class);
            when(em1.createNativeQuery(startsWith("SELECT COUNT(*)"))).thenReturn(nativeQuery);
            when(nativeQuery.setParameter(eq(1), any())).thenReturn(nativeQuery);
            when(nativeQuery.getSingleResult()).thenReturn(BigInteger.valueOf(30));

            UserStat stat = mock(UserStat.class);
            when(stat.getTotalEntries()).thenReturn(5);
            @SuppressWarnings("unchecked")
            TypedQuery<UserStat> statQuery = mock(TypedQuery.class);
            when(em1.createQuery(startsWith("SELECT s FROM UserStat"), eq(UserStat.class))).thenReturn(statQuery);
            when(statQuery.setParameter(eq("u"), any())).thenReturn(statQuery);
            when(statQuery.getResultStream()).thenReturn(Stream.of(stat));

            EntityManager em2 = mock(EntityManager.class);
            EntityTransaction tx2 = mock(EntityTransaction.class);
            when(em2.getTransaction()).thenReturn(tx2);

            when(emf.createEntityManager()).thenReturn(em1, em2);

            StatsData fresh = mock(StatsData.class);
            fetchUsersStatic.when(() -> FetchUsers.fetchUserStats("gooduser")).thenReturn(fresh);

            UserStat mapped = mock(UserStat.class);
            userStatMapperStatic.when(() -> UserStatMapper.mapOrCreate(eq(fresh), any(), eq(em2))).thenReturn(mapped);

            userMapperStatic.when(() -> UserMapper.map(any(), any())).thenReturn(user);

            DataIntegrityRestorer restorer = new DataIntegrityRestorer(0.5, 10, 10, true);
            restorer.processUserById(777);

            UserResyncService created = resyncConstruction.constructed().get(0);
            verify(created, times(1)).resyncUserUpsertFetchWithRetries("gooduser", 777, 3, 500);
            fetchUsersStatic.verify(() -> FetchUsers.fetchUserStats("gooduser"), times(1));
            verify(em2, times(1)).merge(mapped);
        }
    }

    @Test
    void reportedZero_nativeNonZero_triggersResyncAndDelete() {
        try (MockedStatic<Parser> parserStatic = mockStatic(Parser.class);
             MockedConstruction<UserResyncService> resyncConstruction = mockConstruction(UserResyncService.class,
                     (mock, ctx) -> when(mock.resyncUserUpsertFetchWithRetries(anyString(), anyInt(), anyInt(), anyLong())).thenReturn(false))) {

            EntityManagerFactory emf = mock(EntityManagerFactory.class);
            parserStatic.when(Parser::getEmf).thenReturn(emf);

            EntityManager em1 = mock(EntityManager.class);
            EntityTransaction tx1 = mock(EntityTransaction.class);
            when(em1.getTransaction()).thenReturn(tx1);

            Users user = mock(Users.class);
            when(user.getUsername()).thenReturn("zeroReported");
            when(user.getMalId()).thenReturn(555);
            when(user.getUrl()).thenReturn("http://u");
            when(em1.find(Users.class, 555)).thenReturn(user);

            Query nativeQuery = mock(Query.class);
            when(em1.createNativeQuery(startsWith("SELECT COUNT(*)"))).thenReturn(nativeQuery);
            when(nativeQuery.setParameter(eq(1), any())).thenReturn(nativeQuery);
            when(nativeQuery.getSingleResult()).thenReturn(BigInteger.valueOf(10));

            @SuppressWarnings("unchecked")
            TypedQuery<UserStat> statQuery = mock(TypedQuery.class);
            when(em1.createQuery(startsWith("SELECT s FROM UserStat"), eq(UserStat.class))).thenReturn(statQuery);
            when(statQuery.setParameter(eq("u"), any())).thenReturn(statQuery);
            when(statQuery.getResultStream()).thenReturn(Stream.empty());

            when(emf.createEntityManager()).thenReturn(em1);

            DataIntegrityRestorer restorer = new DataIntegrityRestorer(0.1, 10, 10, true);
            restorer.processUserById(555);

            UserResyncService created = resyncConstruction.constructed().get(0);
            verify(created, times(1)).resyncUserUpsertFetchWithRetries("zeroReported", 555, 3, 500);
            verify(created, times(1)).deleteUserData(555);
        }
    }
}
