package AnimeParsing;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Demographic {

    public int mal_id;
    public String name;
    public String url;

}
