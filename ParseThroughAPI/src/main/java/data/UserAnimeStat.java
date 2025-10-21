package data;

import jakarta.persistence.*;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.Objects;

@Entity
@Table(name = "user_anime_stat")
@IdClass(UserAnimeStat.UserAnimeKey.class)
public class UserAnimeStat {

    @Id
    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @Id
    @Column(name = "anime_id", nullable = false)
    private Integer animeId;

    @Column(name = "score")
    private Integer score;

    @Column(name = "status", length = 64)
    private String status;

    @Column(name = "episodes_watched")
    private Integer episodesWatched;

    @Column(name = "last_updated")
    private OffsetDateTime lastUpdated;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private Users user;

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public Integer getAnimeId() {
        return animeId;
    }

    public void setAnimeId(Integer animeId) {
        this.animeId = animeId;
    }

    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = score;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getEpisodesWatched() {
        return episodesWatched;
    }

    public void setEpisodesWatched(Integer episodesWatched) {
        this.episodesWatched = episodesWatched;
    }

    public OffsetDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(OffsetDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public Users getUser() {
        return user;
    }

    public void setUser(Users user) {
        this.user = user;
    }

    public static class UserAnimeKey implements Serializable {
        private Integer userId;
        private Integer animeId;

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            UserAnimeKey that = (UserAnimeKey) o;
            return Objects.equals(userId, that.userId) && Objects.equals(animeId, that.animeId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(userId, animeId);
        }

        public Integer getUserId() {
            return userId;
        }

        public void setUserId(Integer userId) {
            this.userId = userId;
        }

        public Integer getAnimeId() {
            return animeId;
        }

        public void setAnimeId(Integer animeId) {
            this.animeId = animeId;
        }
    }
}


