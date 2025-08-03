package Mapper;

import Data.Users;
import UserParsing.UserLite;
import jakarta.persistence.EntityManager;

import java.time.OffsetDateTime;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class UserMapper {

    private static final DateTimeFormatter ISO_DATETIME = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
    private static final DateTimeFormatter ISO_OFFSET = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    public static Users mapOrUpdate(UserLite dto, EntityManager em) {
        Users user = em.find(Users.class, dto.mal_id);
        if (user == null) {
            user = new Users();
            user.setMalId(dto.mal_id);
        }

        user.setUsername(dto.username);
        user.setUrl(dto.url);

        if (dto.last_online != null && !dto.last_online.isBlank()) {
            try {
                user.setLastOnline(OffsetDateTime.parse(dto.last_online, ISO_DATETIME));
            } catch (Exception e) {
                System.out.println("Parsing error last_online: " + dto.last_online);
            }
        }

        user.setGender(dto.gender);

        if (dto.birthday != null && !dto.birthday.isBlank()) {
            try {
                user.setBirthday(LocalDate.parse(dto.birthday, ISO_OFFSET));
            } catch (Exception e) {
                System.out.println("Parsing error birthday: " + dto.birthday);
            }
        }

        user.setLocation(dto.location);

        if (dto.joined != null && !dto.joined.isBlank()) {
            try {
                user.setJoined(OffsetDateTime.parse(dto.joined, ISO_DATETIME));
            } catch (Exception e) {
                System.out.println("Parsing error joined: " + dto.joined);
            }
        }

        user.setUpdatedAt(OffsetDateTime.now());

        return em.merge(user);
    }
}
