package UserParsing;

import Exeptions.HttpRequestException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

import static UserParsing.Parser.saveUserAndStats;

public class FetchUsers {

    private static final String BASE = "https://api.jikan.moe/v4";
    private static final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    private static final ObjectMapper mapper = new ObjectMapper();

    public static void main(String[] args) {
        fetchAndPersistRandomUsers(10, 10);
    }

    public static void fetchAndPersistRandomUsers(int numberOfUsers, int numberOfAnimeInLists) {
        for (int i = 0; i < numberOfUsers; i++) {
            try {
                UserLite curUser = fetchRandomUser();
                StatsData sd = fetchUserStats(curUser.username);
                if (sd.anime.total_entries < numberOfAnimeInLists) {
                    throw new Exception("Too few anime watched " + sd.anime.total_entries);
                }
                saveUserAndStats(curUser);
            } catch (Exception e) {
                e.printStackTrace();
                i--;
            } finally {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException _) { }
            }
        }
        System.out.println("Fetched " + numberOfUsers + " users");

    }

    public static void fetchAndPersistUserByUsername(String username, int tryNumber) {
        try {
            UserLite curUser = fetchUserByUsername(username);
            saveUserAndStats(curUser);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            if (tryNumber < 5)
                fetchAndPersistUserByUsername(username, tryNumber + 1);
        }
    }

    private static JsonNode responseHandle(String url) throws IOException, InterruptedException {
        HttpResponse<String> resp = client.send(requestBuild(url), HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 200) throw new HttpRequestException("HTTP " + resp.statusCode());
        return mapper.readTree(resp.body()).get("data");
    }

    private static HttpRequest requestBuild(String url) {
        return HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .timeout(Duration.ofSeconds(10))
                .build();
    }

    public static UserLite fetchRandomUser() throws IOException, InterruptedException {
        JsonNode data = responseHandle(BASE + "/random/users");
        return mapper.treeToValue(data, UserLite.class);
    }

    public static StatsData fetchUserStats(String username) throws IOException, InterruptedException {
        String url = BASE + "/users/" + username + "/statistics";
        String body = client.send(requestBuild(url), HttpResponse.BodyHandlers.ofString()).body();
        return mapper.readValue(body, StatsResponse.class).data;
    }

    public static UserLite fetchUserByUsername(String username) throws IOException, InterruptedException {
        JsonNode data = responseHandle(BASE + "/users/" + username);
        return mapper.treeToValue(data, UserLite.class);
    }

}
