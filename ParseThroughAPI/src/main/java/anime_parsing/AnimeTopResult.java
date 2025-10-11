package anime_parsing;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;


@JsonIgnoreProperties(ignoreUnknown = true)
public class AnimeTopResult {

    public List<Anime> data;

}
