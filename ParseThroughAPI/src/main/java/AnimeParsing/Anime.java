package AnimeParsing;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class Anime {

    public Integer malId;
    public String url;
    public String status;
    public String title;
    public Boolean approved;
    public String titleEnglish;
    public String titleJapanese;
    public String type;
    public Integer episodes;
    public String rating;
    public Double score;
    public Integer scoredBy;
    public String synopsis;
    public String background;
    public String season;
    public Integer year;
    public List<Producer> producers;
    public List<Producer> licensors;
    public List<Producer> studios;
    public List<Genre> genres;
    public List<Genre> themes;
    public List<Demographic> demographics;

    public Integer getMalId() {
        return malId;
    }

    public String getUrl() {
        return url;
    }

    public String getStatus() {
        return status;
    }

    public String getTitle() {
        return title;
    }

    public Boolean getApproved() {
        return approved;
    }

    public String getTitleEnglish() {
        return titleEnglish;
    }

    public String getTitleJapanese() {
        return titleJapanese;
    }

    public String getType() {
        return type;
    }

    public Integer getEpisodes() {
        return episodes;
    }

    public String getRating() {
        return rating;
    }

    public Double getScore() {
        return score;
    }

    public Integer getScoredBy() {
        return scoredBy;
    }

    public String getSynopsis() {
        return synopsis;
    }

    public String getBackground() {
        return background;
    }

    public String getSeason() {
        return season;
    }

    public Integer getYear() {
        return year;
    }

    public List<Producer> getProducers() {
        return producers;
    }

    public List<Producer> getLicensors() {
        return licensors;
    }

    public List<Producer> getStudios() {
        return studios;
    }

    public List<Genre> getGenres() {
        return genres;
    }

    public List<Genre> getThemes() {
        return themes;
    }

    public List<Demographic> getDemographics() {
        return demographics;
    }
}
