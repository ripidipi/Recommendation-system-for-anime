package mapper;

import data.Users;
import user_parsing.UserLite;
import utils.DateTime;
import jakarta.persistence.EntityManager;

import java.time.OffsetDateTime;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class UserMapper {

    private static final String FOR_USER = " for user ";

    // TODO reduce complexity
    public static Users mapOrCreate(UserLite dto, EntityManager em) {
        Users user = em.find(Users.class, dto.malId);
        if (user == null) {
            user = new Users();
            user.setMalId(dto.malId);
        }

        user.setUsername(dto.username);
        user.setUrl(dto.url);

        if (dto.lastOnline != null && !dto.lastOnline.isBlank()) {
            OffsetDateTime lo = DateTime.parseToOffsetDateTime(dto.lastOnline);
            if (lo != null) user.setLastOnline(lo);
            else System.out.println("Error to parse last_online: " + dto.lastOnline + FOR_USER + dto.username);
        }

        user.setGender(dto.gender);

        if (dto.birthday != null && !dto.birthday.isBlank()) {
            LocalDate bd = DateTime.parseToLocalDate(dto.birthday);
            if (bd != null) user.setBirthday(bd);
            else System.out.println("Unable to parse birthday: " + dto.birthday + FOR_USER + dto.username);
        }

        user.setLocation(dto.location);

        if (dto.joined != null && !dto.joined.isBlank()) {
            OffsetDateTime j = DateTime.parseToOffsetDateTime(dto.joined);
            if (j != null) user.setJoined(j);
            else System.out.println("Unable to parse joined: " + dto.joined + FOR_USER + dto.username);
        }

        user.setUpdatedAt(OffsetDateTime.now());

        return em.merge(user);
    }
}
