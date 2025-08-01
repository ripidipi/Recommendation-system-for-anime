package AnimeParsing;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;


public class Main {
    private static final String BASE = "https://api.jikan.moe/v4";
    private static final HttpClient client = HttpClient.newHttpClient();
    private static final ObjectMapper mapper = new ObjectMapper();

    public static void main(String[] args)  {
        for (int i = 1; i < 1000; i++) {
            try {
                AnimeTopResult animeTopResult = fetchTopAnime(i);
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
    }

    private static AnimeTopResult fetchTopAnime(int page) throws Exception {
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

    private static class HttpRequestException extends RuntimeException {
        public HttpRequestException(String msg) {
            super(msg);
        }
    }

}
