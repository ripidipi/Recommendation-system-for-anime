package Scripts;

import static AnimeParsing.FetchTop.countPages;
import static AnimeParsing.FetchTop.fetchAndPersistAnime;
import static UserParsing.FetchUsers.fetchAndPersistRandomUsers;

public class FetchAndPersist {

    int NUMBER_OF_USERS;
    int NUMBER_OF_PAGES = -1;
    int NUMBER_OF_ANIME_IN_LIST;

    public FetchAndPersist(int numberOfUsers, int numberOfAnimeInLists) {
        NUMBER_OF_USERS = numberOfUsers;
        NUMBER_OF_ANIME_IN_LIST = numberOfAnimeInLists;
    }

    public FetchAndPersist(int numberOfUsers, int numberOfAnimeInLists, int numberOfPages) {
        NUMBER_OF_USERS = numberOfUsers;
        NUMBER_OF_ANIME_IN_LIST = numberOfAnimeInLists;
        NUMBER_OF_PAGES = numberOfPages;
    }

    public void fillUserDB() {
        fetchAndPersistRandomUsers(NUMBER_OF_USERS, NUMBER_OF_ANIME_IN_LIST);
    }

    public void fillAnimeDB() {
        int numberOfPage;
        if (NUMBER_OF_PAGES == -1)
            numberOfPage = countPages();
        else
            numberOfPage = NUMBER_OF_PAGES;
        fetchAndPersistAnime(numberOfPage);
    }

}
