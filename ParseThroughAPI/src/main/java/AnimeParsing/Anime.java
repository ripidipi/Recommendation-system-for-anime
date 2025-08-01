package AnimeParsing;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;


@JsonIgnoreProperties(ignoreUnknown = true)
public class Anime {

    public int mal_id;
    public String url;
    public String title;
    public Boolean approved;
    public String title_english;
    public String title_japanese;
    public String type;
    public Integer episodes;
    public String rating;
    public Double score;
    public Integer scored_by;
    public String synopsis;
    public String background;
    public String season;
    public Integer year;
    public List<Producer> producers;
    public List<Producer> licensors;
    public List<Producer> studios;
    public List<Genre> genres;
    public List<Genre> explicit_genres;
    public List<Genre> themes;
    public List<Demographic> demographics;

}
