package UserParsing;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CategoryStats {

    public double days_watched;
    public double mean_score;
    public int watching;
    public int completed;
    public int on_hold;
    public int dropped;
    public int plan_to_watch;
    public int total_entries;
    public int rewatched;
    public int episodes_watched;


}
