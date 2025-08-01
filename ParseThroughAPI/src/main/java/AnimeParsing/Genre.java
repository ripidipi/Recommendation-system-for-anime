package AnimeParsing;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Genre {

    public int mal_id;
    public String type;
    public String name;
    public String url;

}