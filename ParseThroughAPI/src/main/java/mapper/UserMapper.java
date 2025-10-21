package mapper;

import data.Users;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import user_parsing.UserLite;
import utils.DateTime;
import jakarta.persistence.EntityManager;

import java.time.OffsetDateTime;
import java.time.LocalDate;

public class UserMapper {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserMapper.class);

    private UserMapper() {}

    /**
     * @return managed entity (persisted for creation)
     */
    public static Users map(UserLite dto, EntityManager em) {

        Users user = em.find(Users.class, dto.malId);
        if (user == null) {
            user = new Users();
            user.setMalId(dto.malId);
            em.persist(user);
        }

        apply(user, dto);
        return user;
    }

    private static void apply(Users entity, UserLite dto) {
        entity.setUsername(dto.username);
        entity.setUrl(dto.url);

        if (dto.lastOnline != null && !dto.lastOnline.isBlank()) {
            OffsetDateTime lo = DateTime.parseToOffsetDateTime(dto.lastOnline);
            if (lo != null) entity.setLastOnline(lo);
            else LOGGER.error("Error to parse last_online: {} for user {}", dto.lastOnline, dto.username);
        }

        entity.setGender(dto.gender);

        if (dto.birthday != null && !dto.birthday.isBlank()) {
            LocalDate bd = DateTime.parseToLocalDate(dto.birthday);
            if (bd != null) entity.setBirthday(bd);
            else LOGGER.warn("Unable to parse birthday: {} for {}", dto.birthday, dto.username);
        }

        entity.setLocation(dto.location);

        if (dto.joined != null && !dto.joined.isBlank()) {
            OffsetDateTime j = DateTime.parseToOffsetDateTime(dto.joined);
            if (j != null) entity.setJoined(j);
            else LOGGER.warn("Unable to parse joined: {} for {}", dto.joined, dto.username);
        }

        entity.setUpdatedAt(OffsetDateTime.now());
    }
}
