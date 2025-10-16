package anime_parsing;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.*;
import org.mockito.MockedStatic;
import testhelpers.TestHelpers;

import static org.assertj.core.api.Assertions.assertThat;

class CountPagesTest {

    private MockWebServer server;

    @BeforeAll
    static void disableDb() {
        EmfHolder.disableForTests();
    }

    @BeforeEach
    void setup() throws Exception {
        server = new MockWebServer();
        server.start();
        String base = server.url("/v4").toString();
        if (base.endsWith("/")) base = base.substring(0, base.length() - 1);
        FetchTop.setBASE(base);
    }

    @AfterEach
    void teardown() throws Exception {
        if (server != null) server.shutdown();
    }

    @Test
    void countPages_findsThreePages() {
        server.enqueue(new MockResponse().setResponseCode(200).setBody("{\"data\":[{\"mal_id\":1}]}"));
        server.enqueue(new MockResponse().setResponseCode(200).setBody("{\"data\":[{\"mal_id\":1}]}"));
        server.enqueue(new MockResponse().setResponseCode(200).setBody("{\"data\":[{\"mal_id\":2}]}"));
        server.enqueue(new MockResponse().setResponseCode(200).setBody("{\"data\":[]}"));
        server.enqueue(new MockResponse().setResponseCode(200).setBody("{\"data\":[{\"mal_id\":3}]}"));

        try (MockedStatic<Parser> parserMock = TestHelpers.mockParserDoNothing()) {
            int pages = FetchTop.countPages();
            assertThat(pages).isEqualTo(3);
        }
    }
}
