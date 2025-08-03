package UserParsing;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;


@JsonIgnoreProperties(ignoreUnknown = true)
public class UserLite {

    public int mal_id;
    public String username;
    public String url;
    public String last_online;
    public String gender;
    public String birthday;
    public String location;
    public String joined;

}
