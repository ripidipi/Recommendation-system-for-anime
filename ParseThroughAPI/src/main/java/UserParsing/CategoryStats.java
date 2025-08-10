package UserParsing;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class CategoryStats {

    public double daysWatched;
    public double meanScore;
    public int watching;
    public int completed;
    public int onHold;
    public int dropped;
    public int planToWatch;
    public int totalEntries;
    public int rewatched;
    public int episodesWatched;


}
