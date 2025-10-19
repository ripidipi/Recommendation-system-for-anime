package scripts;

import anime_parsing.FetchTop;
import anime_parsing.Parser;
import anime_parsing.ParserBackedPersister;

import static user_parsing.FetchUsers.fetchAndPersistRandomUsers;

public class FetchAndPersist {

    Integer numberOfUsers;
    Integer numberOfPages;
    Integer numberOfAnimeInList;
    Integer numberOfCompletedAnimeInList;

    public FetchAndPersist(Integer numberOfUsers, Integer numberOfAnimeInLists,
                           Integer numberOfPages, Integer numberOfCompletedAnimeInLists) {
        this.numberOfUsers = numberOfUsers;
        numberOfAnimeInList = numberOfAnimeInLists;
        this.numberOfPages = numberOfPages;
        numberOfCompletedAnimeInList = numberOfCompletedAnimeInLists;
    }

    public void fillUserDB() {
        System.out.println("FetchAndPersist.fillUserDB: users=" + numberOfUsers + ", minAnimeInLists=" + numberOfAnimeInList);
        fetchAndPersistRandomUsers(numberOfUsers, numberOfAnimeInList, numberOfCompletedAnimeInList);
    }

    public void fillAnimeDB() {
        int numberOfPage;
        ParserBackedPersister animePersister = new ParserBackedPersister();
        FetchTop fetchTop = FetchTop.createDefault(animePersister);
        if (numberOfPages == null) {
            numberOfPage = fetchTop.countPages();
            System.out.println("FetchAndPersist.fillAnimeDB: countPages() -> " + numberOfPage);
        } else {
            numberOfPage = numberOfPages;
            System.out.println("FetchAndPersist.fillAnimeDB: using NUMBER_OF_PAGES -> " + numberOfPage);
        }
        fetchTop.fetchAndPersistAnime(numberOfPage);
    }

}
