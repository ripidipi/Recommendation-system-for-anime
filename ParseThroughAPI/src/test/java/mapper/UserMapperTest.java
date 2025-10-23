package mapper;

import data.Users;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import user_parsing.UserLite;
import utils.DateTime;
import jakarta.persistence.EntityManager;
import org.mockito.MockedStatic;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserMapperTest {

    @Mock
    EntityManager em;

    private final int malId = 123;

    @BeforeEach
    void setUp() {
        reset(em);
    }

    @Test
    void createNewUser_persistsAndSetsAllParsableFields() {
        UserLite dto = new UserLite();
        dto.malId = malId;
        dto.username = "alice";
        dto.url = "https://example.com/alice";
        dto.gender = "Female";
        dto.location = "Wonderland";
        dto.lastOnline = "2024-01-02T03:04:05Z";
        dto.birthday = "1990-12-31";
        dto.joined = "2020-05-10T10:00:00Z";

        when(em.find(Users.class, malId)).thenReturn(null);

        OffsetDateTime parsedLastOnline = OffsetDateTime.of(2024,1,2,3,4,5,0, ZoneOffset.UTC);
        LocalDate parsedBirthday = LocalDate.of(1990,12,31);
        OffsetDateTime parsedJoined = OffsetDateTime.of(2020,5,10,10,0,0,0, ZoneOffset.UTC);

        try (MockedStatic<DateTime> dt = mockStatic(DateTime.class)) {
            dt.when(() -> DateTime.parseToOffsetDateTime(dto.lastOnline)).thenReturn(parsedLastOnline);
            dt.when(() -> DateTime.parseToLocalDate(dto.birthday)).thenReturn(parsedBirthday);
            dt.when(() -> DateTime.parseToOffsetDateTime(dto.joined)).thenReturn(parsedJoined);

            ArgumentCaptor<Users> captor = ArgumentCaptor.forClass(Users.class);

            OffsetDateTime before = OffsetDateTime.now(ZoneOffset.UTC);
            Users result = UserMapper.map(dto, em);
            OffsetDateTime after = OffsetDateTime.now(ZoneOffset.UTC);

            verify(em).find(Users.class, malId);
            verify(em).persist(captor.capture());
            Users persisted = captor.getValue();

            assertAll(
                    () -> assertThat(result).isSameAs(persisted),
                    () -> assertThat(persisted.getMalId()).isEqualTo(malId),
                    () -> assertThat(persisted.getUsername()).isEqualTo("alice"),
                    () -> assertThat(persisted.getUrl()).isEqualTo("https://example.com/alice"),
                    () -> assertThat(persisted.getGender()).isEqualTo("Female"),
                    () -> assertThat(persisted.getLocation()).isEqualTo("Wonderland"),
                    () -> assertThat(persisted.getLastOnline()).isEqualTo(parsedLastOnline),
                    () -> assertThat(persisted.getBirthday()).isEqualTo(parsedBirthday),
                    () -> assertThat(persisted.getJoined()).isEqualTo(parsedJoined),
                    () -> assertThat(persisted.getUpdatedAt()).isNotNull(),
                    () -> assertThat(persisted.getUpdatedAt()).isBetween(before.minusSeconds(1), after.plusSeconds(1))
            );
        }
    }

    @Test
    void updateExistingUser_mutatesManagedEntity_andDoesNotPersist() {
        UserLite dto = new UserLite();
        dto.malId = malId;
        dto.username = "bob";
        dto.url = "https://example.com/bob";
        dto.gender = "Male";
        dto.location = "Builderland";
        dto.lastOnline = "2025-02-03T04:05:06Z";
        dto.birthday = "1985-07-07";
        dto.joined = "2019-01-01T00:00:00Z";

        Users existing = new Users();
        existing.setMalId(malId);
        existing.setUsername("oldname");
        existing.setUrl("oldurl");
        existing.setGender("Other");
        existing.setLocation("oldloc");
        existing.setLastOnline(OffsetDateTime.of(2000,1,1,0,0,0,0, ZoneOffset.UTC));
        existing.setBirthday(LocalDate.of(1970,1,1));
        existing.setJoined(OffsetDateTime.of(2001,1,1,0,0,0,0, ZoneOffset.UTC));
        existing.setUpdatedAt(OffsetDateTime.of(2020,1,1,0,0,0,0, ZoneOffset.UTC));

        when(em.find(Users.class, malId)).thenReturn(existing);

        OffsetDateTime parsedLastOnline = OffsetDateTime.of(2025,2,3,4,5,6,0, ZoneOffset.UTC);
        LocalDate parsedBirthday = LocalDate.of(1985,7,7);
        OffsetDateTime parsedJoined = OffsetDateTime.of(2019,1,1,0,0,0,0, ZoneOffset.UTC);

        try (MockedStatic<DateTime> dt = mockStatic(DateTime.class)) {
            dt.when(() -> DateTime.parseToOffsetDateTime(dto.lastOnline)).thenReturn(parsedLastOnline);
            dt.when(() -> DateTime.parseToLocalDate(dto.birthday)).thenReturn(parsedBirthday);
            dt.when(() -> DateTime.parseToOffsetDateTime(dto.joined)).thenReturn(parsedJoined);

            OffsetDateTime before = OffsetDateTime.now(ZoneOffset.UTC);
            Users returned = UserMapper.map(dto, em);
            OffsetDateTime after = OffsetDateTime.now(ZoneOffset.UTC);

            verify(em).find(Users.class, malId);
            verify(em, never()).persist(any(Users.class));

            assertAll(
                    () -> assertThat(returned).isSameAs(existing),
                    () -> assertThat(existing.getUsername()).isEqualTo("bob"),
                    () -> assertThat(existing.getUrl()).isEqualTo("https://example.com/bob"),
                    () -> assertThat(existing.getGender()).isEqualTo("Male"),
                    () -> assertThat(existing.getLocation()).isEqualTo("Builderland"),
                    () -> assertThat(existing.getLastOnline()).isEqualTo(parsedLastOnline),
                    () -> assertThat(existing.getBirthday()).isEqualTo(parsedBirthday),
                    () -> assertThat(existing.getJoined()).isEqualTo(parsedJoined),
                    () -> assertThat(existing.getUpdatedAt()).isNotNull(),
                    () -> assertThat(existing.getUpdatedAt()).isBetween(before.minusSeconds(1), after.plusSeconds(1))
            );
        }
    }

    @Test
    void invalidDateParsing_doesNotSetThatField() {
        UserLite dto = new UserLite();
        dto.malId = malId;
        dto.username = "charlie";
        dto.lastOnline = "invalid-last-online";
        dto.birthday = "invalid-birthday";
        dto.joined = "invalid-joined";

        when(em.find(Users.class, malId)).thenReturn(null);

        try (MockedStatic<DateTime> dt = mockStatic(DateTime.class)) {
            dt.when(() -> DateTime.parseToOffsetDateTime(dto.lastOnline)).thenReturn(null);
            dt.when(() -> DateTime.parseToLocalDate(dto.birthday)).thenReturn(null);
            dt.when(() -> DateTime.parseToOffsetDateTime(dto.joined)).thenReturn(null);

            ArgumentCaptor<Users> captor = ArgumentCaptor.forClass(Users.class);
            UserMapper.map(dto, em);

            verify(em).persist(captor.capture());
            Users persisted = captor.getValue();

            assertAll(
                    () -> assertThat(persisted.getLastOnline()).isNull(),
                    () -> assertThat(persisted.getBirthday()).isNull(),
                    () -> assertThat(persisted.getJoined()).isNull(),
                    () -> assertThat(persisted.getUsername()).isEqualTo("charlie")
            );
        }
    }

    @Test
    void blankAndNullDateStrings_areIgnored() {
        UserLite dto = new UserLite();
        dto.malId = malId;
        dto.username = "dora";
        dto.lastOnline = "";
        dto.birthday = null;
        dto.joined = "   ";

        when(em.find(Users.class, malId)).thenReturn(null);

        try (MockedStatic<DateTime> dt = mockStatic(DateTime.class)) {
            dt.when(() -> DateTime.parseToOffsetDateTime(anyString())).thenAnswer(invocation -> {
                throw new AssertionError("parseToOffsetDateTime should not be called for blank/ null inputs");
            });
            dt.when(() -> DateTime.parseToLocalDate(anyString())).thenAnswer(invocation -> {
                throw new AssertionError("parseToLocalDate should not be called for blank/ null inputs");
            });

            ArgumentCaptor<Users> captor = ArgumentCaptor.forClass(Users.class);
            UserMapper.map(dto, em);
            verify(em).persist(captor.capture());
            Users persisted = captor.getValue();

            assertAll(
                    () -> assertThat(persisted.getLastOnline()).isNull(),
                    () -> assertThat(persisted.getBirthday()).isNull(),
                    () -> assertThat(persisted.getJoined()).isNull(),
                    () -> assertThat(persisted.getUsername()).isEqualTo("dora")
            );
        }
    }

    @Test
    void nullDto_willThrowNpe() {
        assertThrows(NullPointerException.class, () -> UserMapper.map(null, em));
    }
}
