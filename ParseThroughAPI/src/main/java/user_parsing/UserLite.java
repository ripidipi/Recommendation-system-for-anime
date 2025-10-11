package user_parsing;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;


@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class UserLite {

    public int malId;
    public String username;
    public String url;
    public String lastOnline;
    public String gender;
    public String birthday;
    public String location;
    public String joined;

}
