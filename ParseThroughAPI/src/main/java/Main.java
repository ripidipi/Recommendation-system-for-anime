import Scripts.FetchAndPersist;
import UserParsing.FetchUsers;

public class Main {

    public static void main(String[] args) {
        FetchAndPersist fetching = new FetchAndPersist(1_000, 50);
        // fetching.fillAnimeDB();
        fetching.fillUserDB();

    }

}
