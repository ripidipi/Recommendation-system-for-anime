package Scripts;

import static AnimeParsing.FetchTop.countPages;
import static AnimeParsing.FetchTop.fetchAndPersistAnime;
import static UserParsing.FetchUsers.fetchAndPersistRandomUsers;

public class FetchAndPersist {

    Integer NUMBER_OF_USERS;
    Integer NUMBER_OF_PAGES;
    Integer NUMBER_OF_ANIME_IN_LIST;
    Integer NUMBER_OF_COMPLETED_ANIME_IN_LIST = 0;

    public FetchAndPersist(int numberOfUsers, int numberOfAnimeInLists,
                           Integer numberOfPages, int numberOfCompletedAnimeInLists) {
        NUMBER_OF_USERS = numberOfUsers;
        NUMBER_OF_ANIME_IN_LIST = numberOfAnimeInLists;
        NUMBER_OF_PAGES = numberOfPages;
        NUMBER_OF_COMPLETED_ANIME_IN_LIST = numberOfCompletedAnimeInLists;
    }

    public void fillUserDB() {
        System.out.println("FetchAndPersist.fillUserDB: users=" + NUMBER_OF_USERS + ", minAnimeInLists=" + NUMBER_OF_ANIME_IN_LIST);
        fetchAndPersistRandomUsers(NUMBER_OF_USERS, NUMBER_OF_ANIME_IN_LIST, NUMBER_OF_COMPLETED_ANIME_IN_LIST);
    }

    public void fillAnimeDB() {
        int numberOfPage;
        if (NUMBER_OF_PAGES == null) {
            numberOfPage = countPages();
            System.out.println("FetchAndPersist.fillAnimeDB: countPages() -> " + numberOfPage);
        } else {
            numberOfPage = NUMBER_OF_PAGES;
            System.out.println("FetchAndPersist.fillAnimeDB: using NUMBER_OF_PAGES -> " + numberOfPage);
        }
        fetchAndPersistAnime(numberOfPage);
    }

}
