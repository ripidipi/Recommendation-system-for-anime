package UserParsing;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ScoreBucket {
    public int score;
    public int votes;
    public double percentage;
}