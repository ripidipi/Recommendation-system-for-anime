
import Scripts.DataIntegrityRestorer;
import Scripts.FetchAndPersist;
import UserParsing.FetchUsers;

public class Main {

    public static void main(String[] args) {
        FetchAndPersist fetching = new FetchAndPersist(100, 50);
        // fetching.fillAnimeDB();
        // fetching.fillUserDB();
        DataIntegrityRestorer dataIntegrityRestorer = new DataIntegrityRestorer(8, 100);
        dataIntegrityRestorer.run();
    }

}
