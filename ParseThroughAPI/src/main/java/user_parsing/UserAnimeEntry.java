package user_parsing;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class UserAnimeEntry {

    public Integer animeId;
    public Integer score;
    public Integer status;
    public Integer numWatchedEpisodes;
    public Long createdAt;
    public Long updatedAt;
}
