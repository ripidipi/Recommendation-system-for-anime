package UserParsing;

import AnimeParsing.Anime;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.OffsetDateTime;


@JsonIgnoreProperties(ignoreUnknown = true)
public class UserAnimeEntry {
    public Anime anime;
    public Integer score;
    public String status;
    public Integer episodes_watched;
    public OffsetDateTime lastUpdated;

}
