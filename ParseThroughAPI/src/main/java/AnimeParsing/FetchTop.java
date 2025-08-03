package AnimeParsing;

import Exeptions.HttpRequestException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;


public class FetchTop {

    private static final String BASE = "https://api.jikan.moe/v4";
    private static final HttpClient client = HttpClient.newHttpClient();
    private static final ObjectMapper mapper = new ObjectMapper();

    public static void main(String[] args) {
        fetchAndPersistTopAnime(700);
    }

    public static void fetchAndPersistTopAnime(int numberOfPages)  {
        for (int i = 1; i <= numberOfPages; i++) {
            try {
                AnimeTopResult animeTopResult = fetchTopAnimePage(i);
                for (var anime: animeTopResult.data) {
                    Parser.saveAnimeToDB(anime);
                }
            } catch (Exception e) {
                System.out.println(e.getMessage());
                i--;
            } finally {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException _) { }
            }
        }
        System.out.println("Total anime pages fetched: " + numberOfPages);
    }

    private static AnimeTopResult fetchTopAnimePage(int page) throws IOException, InterruptedException {
        String url = String.format("%s/top/anime?page=%d", BASE, page);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new HttpRequestException("HTTP " + response.statusCode());
        }
        return mapper.readValue(response.body(), AnimeTopResult.class);
    }



}
