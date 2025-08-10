package AnimeParsing;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class Anime {

    public int malId;
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

}
