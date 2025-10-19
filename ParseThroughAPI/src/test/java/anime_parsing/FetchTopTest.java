package anime_parsing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.*;

import java.net.http.HttpClient;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FetchTopTest {

    private MockWebServer server;
    private ExecutorService executor;
    private FetchTop fetchTop;

    @BeforeAll
    static void disableRealDb() {
        EmfHolder.disableForTests();
    }

    @BeforeEach
    void setup() throws Exception {
        server = new MockWebServer();
        server.start();

        String base = server.url("/v4").toString();
        if (base.endsWith("/")) base = base.substring(0, base.length() - 1);

        ObjectMapper mapper = new ObjectMapper()
                .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
                .findAndRegisterModules();

        executor = Executors.newSingleThreadExecutor();

        InMemoryPersister persister = new InMemoryPersister();

        fetchTop = new FetchTop(
                HttpClient.newBuilder().build(),
                mapper,
                persister,
                executor,
                1,
                10L,
                3,
                3,
                2L,
                5L,
                base
        );
    }

    @AfterEach
    void teardown() throws Exception {
        if (server != null) server.shutdown();
        if (executor != null) {
            executor.shutdownNow();
            executor.awaitTermination(2, TimeUnit.SECONDS);
        }
    }

    @Test
    void countPages_findsThreePages() {
        server.enqueue(new MockResponse().setResponseCode(200).setBody("{\"data\":[{\"mal_id\":1}]}"));
        server.enqueue(new MockResponse().setResponseCode(200).setBody("{\"data\":[{\"mal_id\":1}]}"));
        server.enqueue(new MockResponse().setResponseCode(200).setBody("{\"data\":[{\"mal_id\":2}]}"));
        server.enqueue(new MockResponse().setResponseCode(200).setBody("{\"data\":[]}"));
        server.enqueue(new MockResponse().setResponseCode(200).setBody("{\"data\":[{\"mal_id\":3}]}"));

        int pages = fetchTop.countPages();

        assertThat(pages).isEqualTo(3);
    }

    @Test
    void fetchTopAnimePage_parsesResponse() throws Exception {
        String body = "{\"data\":[{\"mal_id\":10,\"title\":\"Test Anime\"}]}";
        server.enqueue(new MockResponse().setResponseCode(200).setBody(body));

        AnimeTopResult res = fetchTop.fetchTopAnimePage(1);

        assertThat(res).isNotNull();
        assertThat(res.data).hasSize(1);
        assertThat(res.data.get(0).malId).isEqualTo(10);
        assertThat(res.data.get(0).title).isEqualTo("Test Anime");
    }

    @Test
    void pageExists_returnsFalseWhenServerError() {
        server.enqueue(new MockResponse().setResponseCode(500).setBody("Server Error"));

        boolean exists = fetchTop.pageExists(1);

        assertThat(exists).isFalse();
    }

    @Test
    void fetchAndPersistAnime_savesAllItems() throws Exception {
        String body = "{\"data\":[{\"mal_id\":1,\"title\":\"A\"},{\"mal_id\":2,\"title\":\"B\"}]}";
        server.enqueue(new MockResponse().setResponseCode(200).setBody(body));

        InMemoryPersister persister = new InMemoryPersister();

        fetchTop = new FetchTop(
                HttpClient.newBuilder().build(),
                fetchTop.mapper,
                persister,
                executor,
                1, 10L, 3, 3, 2L, 5L,
                server.url("/v4").toString().replaceAll("/$", "")
        );

        fetchTop.fetchAndPersistAnime(1);

        executor.shutdown();
        executor.awaitTermination(2, TimeUnit.SECONDS);

        List<Anime> stored = persister.getStored();
        assertThat(stored).hasSize(2);
        assertThat(stored.get(0).title).isEqualTo("A");
        assertThat(stored.get(1).title).isEqualTo("B");
    }

    @Test
    void fetchAndPersistAnime_continuesWhenPersisterFails() throws Exception {
        String body = "{\"data\":[{\"mal_id\":1,\"title\":\"A\"},{\"mal_id\":2,\"title\":\"B\"}]}";
        server.enqueue(new MockResponse().setResponseCode(200).setBody(body));

        AtomicInteger saved = new AtomicInteger(0);
        AnimePersister throwingPersister = anime -> {
            if (anime.malId == 1) throw new RuntimeException("Simulated save failure");
            saved.incrementAndGet();
        };

        fetchTop = new FetchTop(
                HttpClient.newBuilder().build(),
                fetchTop.mapper,
                throwingPersister,
                executor,
                1, 10L, 3, 3,2L, 5L,
                server.url("/v4").toString().replaceAll("/$", "")
        );

        fetchTop.fetchAndPersistAnime(1);

        executor.shutdown();
        executor.awaitTermination(2, TimeUnit.SECONDS);

        assertThat(saved.get()).isEqualTo(1);
    }

    @Test
    void fetchTopAnimePage_throwsOnNon200Response() {
        server.enqueue(new MockResponse().setResponseCode(404).setBody("Not found"));

        assertThrows(exeptions.HttpRequestException.class, () -> fetchTop.fetchTopAnimePage(1));
    }

    static class InMemoryPersister implements AnimePersister {
        private final CopyOnWriteArrayList<Anime> stored = new CopyOnWriteArrayList<>();

        @Override
        public void save(Anime anime) {
            stored.add(anime);
        }

        List<Anime> getStored() {
            return Collections.unmodifiableList(stored);
        }
    }
}
