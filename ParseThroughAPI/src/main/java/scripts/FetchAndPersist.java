package scripts;

import static anime_parsing.FetchTop.countPages;
import static anime_parsing.FetchTop.fetchAndPersistAnime;
import static user_parsing.FetchUsers.fetchAndPersistRandomUsers;

public class FetchAndPersist {

    Integer numberOfUsers;
    Integer numberOfPages;
    Integer numberOfAnimeInList;
    Integer numberOfCompletedAnimeInList = 0;

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
        if (numberOfPages == null) {
            numberOfPage = countPages();
            System.out.println("FetchAndPersist.fillAnimeDB: countPages() -> " + numberOfPage);
        } else {
            numberOfPage = numberOfPages;
            System.out.println("FetchAndPersist.fillAnimeDB: using NUMBER_OF_PAGES -> " + numberOfPage);
        }
        fetchAndPersistAnime(numberOfPage);
    }

}
