package data;

import jakarta.persistence.*;
import java.util.List;

@Entity
@Table(name = "anime")
public class Anime {

    @Id
    @Column(name = "mal_id", nullable = false)
    private int malId;

    @Column(name = "url", nullable = false, length = 1024)
    private String url;

    @Column(name = "status", length = 128)
    private String status;

    @Column(name = "title", nullable = false, length = 512)
    private String title;

    @Column(name = "approved")
    private Boolean approved;

    @Column(name = "title_english", length = 512)
    private String titleEnglish;

    @Column(name = "title_japanese", length = 512)
    private String titleJapanese;

    @Column(name = "type", length = 64)
    private String type;

    @Column(name = "episodes")
    private Integer episodes;

    @Column(name = "rating", length = 64)
    private String rating;

    @Column(name = "score")
    private Double score;

    @Column(name = "scored_by")
    private Integer scoredBy;

    @Column(name = "synopsis", columnDefinition = "TEXT")
    private String synopsis;

    @Column(name = "background", columnDefinition = "TEXT")
    private String background;

    @Column(name = "season", length = 32)
    private String season;

    @Column(name = "year")
    private Integer year;

    @ManyToMany(cascade = CascadeType.ALL)
    @JoinTable(
            name = "anime_producer",
            joinColumns = @JoinColumn(name = "anime_id"),
            inverseJoinColumns = @JoinColumn(name = "producer_id")
    )
    private List<Producer> producers;

    @ManyToMany(cascade = CascadeType.ALL)
    @JoinTable(
            name = "anime_licensor",
            joinColumns = @JoinColumn(name = "anime_id"),
            inverseJoinColumns = @JoinColumn(name = "producer_id")
    )
    private List<Producer> licensors;

    @ManyToMany(cascade = CascadeType.ALL)
    @JoinTable(
            name = "anime_studio",
            joinColumns = @JoinColumn(name = "anime_id"),
            inverseJoinColumns = @JoinColumn(name = "producer_id")
    )
    private List<Producer> studios;

    @ManyToMany(cascade = CascadeType.ALL)
    @JoinTable(
            name = "anime_genre",
            joinColumns = @JoinColumn(name = "anime_id"),
            inverseJoinColumns = @JoinColumn(name = "genre_id")
    )
    private List<Genre> genres;

    @ManyToMany(cascade = CascadeType.ALL)
    @JoinTable(
            name = "anime_theme",
            joinColumns = @JoinColumn(name = "anime_id"),
            inverseJoinColumns = @JoinColumn(name = "genre_id")
    )
    private List<Genre> themes;

    @ManyToMany(cascade = CascadeType.ALL)
    @JoinTable(
            name = "anime_demographic",
            joinColumns = @JoinColumn(name = "anime_id"),
            inverseJoinColumns = @JoinColumn(name = "demographic_id")
    )
    private List<Demographic> demographics;

    public int getMalId() {
        return malId;
    }

    public void setMalId(int malId) {
        this.malId = malId;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Boolean getApproved() {
        return approved;
    }

    public void setApproved(Boolean approved) {
        this.approved = approved;
    }

    public String getTitleEnglish() {
        return titleEnglish;
    }

    public void setTitleEnglish(String titleEnglish) {
        this.titleEnglish = titleEnglish;
    }

    public String getTitleJapanese() {
        return titleJapanese;
    }

    public void setTitleJapanese(String titleJapanese) {
        this.titleJapanese = titleJapanese;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Integer getEpisodes() {
        return episodes;
    }

    public void setEpisodes(Integer episodes) {
        this.episodes = episodes;
    }

    public String getRating() {
        return rating;
    }

    public void setRating(String rating) {
        this.rating = rating;
    }

    public Double getScore() {
        return score;
    }

    public void setScore(Double score) {
        this.score = score;
    }

    public Integer getScoredBy() {
        return scoredBy;
    }

    public void setScoredBy(Integer scoredBy) {
        this.scoredBy = scoredBy;
    }

    public String getSynopsis() {
        return synopsis;
    }

    public void setSynopsis(String synopsis) {
        this.synopsis = synopsis;
    }

    public String getBackground() {
        return background;
    }

    public void setBackground(String background) {
        this.background = background;
    }

    public String getSeason() {
        return season;
    }

    public void setSeason(String season) {
        this.season = season;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public List<Producer> getProducers() {
        return producers;
    }

    public void setProducers(List<Producer> producers) {
        this.producers = producers;
    }

    public List<Producer> getLicensors() {
        return licensors;
    }

    public void setLicensors(List<Producer> licensors) {
        this.licensors = licensors;
    }

    public List<Producer> getStudios() {
        return studios;
    }

    public void setStudios(List<Producer> studios) {
        this.studios = studios;
    }

    public List<Genre> getGenres() {
        return genres;
    }

    public void setGenres(List<Genre> genres) {
        this.genres = genres;
    }

    public List<Genre> getThemes() {
        return themes;
    }

    public void setThemes(List<Genre> themes) {
        this.themes = themes;
    }

    public List<Demographic> getDemographics() {
        return demographics;
    }

    public void setDemographics(List<Demographic> demographics) {
        this.demographics = demographics;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Anime that = (Anime) o;
        return malId == that.malId;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(malId);
    }
}
