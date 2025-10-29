package scripts;

import anime_parsing.FetchTop;
import anime_parsing.ParserBackedPersister;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static user_parsing.FetchUsers.fetchAndPersistRandomUsers;

public class FetchAndPersist {

    Integer numberOfUsers;
    Integer numberOfPages;
    Integer numberOfAnimeInList;
    Integer numberOfCompletedAnimeInList;
    private static final Logger LOGGER = LoggerFactory.getLogger(FetchAndPersist.class);

    public FetchAndPersist(Integer numberOfUsers, Integer numberOfAnimeInLists,
                           Integer numberOfPages, Integer numberOfCompletedAnimeInLists) {
        this.numberOfUsers = numberOfUsers;
        numberOfAnimeInList = numberOfAnimeInLists;
        this.numberOfPages = numberOfPages;
        numberOfCompletedAnimeInList = numberOfCompletedAnimeInLists;
    }

    public void fillUserDB() {
        LOGGER.info("FetchAndPersist.fillUserDB: users={}, minAnimeInLists={}", numberOfUsers, numberOfAnimeInList);
        fetchAndPersistRandomUsers(numberOfUsers, numberOfAnimeInList, numberOfCompletedAnimeInList);
    }

    public void fillAnimeDB() {
        int numberOfPage;
        ParserBackedPersister animePersister = new ParserBackedPersister();
        FetchTop fetchTop = FetchTop.createDefault(animePersister);
        if (numberOfPages == null) {
            numberOfPage = fetchTop.countPages();
            LOGGER.info("FetchAndPersist.fillAnimeDB: countPages() -> {}", numberOfPage);
        } else {
            numberOfPage = numberOfPages;
            LOGGER.info("FetchAndPersist.fillAnimeDB: using NUMBER_OF_PAGES -> {}", numberOfPage);
        }
        fetchTop.fetchAndPersistAnime(numberOfPage);
    }

}
