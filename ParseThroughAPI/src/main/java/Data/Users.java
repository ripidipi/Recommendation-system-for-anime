package Data;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "users")
public class Users {

    @Id
    @Column(name = "mal_id")
    private Integer malId;

    @Column(name = "username", nullable = false, length = 100)
    private String username;

    @Column(name = "url", length = 255)
    private String url;

    @Column(name = "last_online")
    private OffsetDateTime lastOnline;

    @Column(name = "gender", length = 32)
    private String gender;

    @Column(name = "birthday")
    private LocalDate birthday;

    @Column(name = "location", length = 255)
    private String location;

    @Column(name = "joined")
    private OffsetDateTime joined;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    public Integer getMalId() {
        return malId;
    }

    public void setMalId(Integer malId) {
        this.malId = malId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public OffsetDateTime getLastOnline() {
        return lastOnline;
    }

    public void setLastOnline(OffsetDateTime lastOnline) {
        this.lastOnline = lastOnline;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public LocalDate getBirthday() {
        return birthday;
    }

    public void setBirthday(LocalDate birthday) {
        this.birthday = birthday;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public OffsetDateTime getJoined() {
        return joined;
    }

    public void setJoined(OffsetDateTime joined) {
        this.joined = joined;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
