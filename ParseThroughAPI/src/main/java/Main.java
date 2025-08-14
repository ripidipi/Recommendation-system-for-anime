import Scripts.DataIntegrityRestorer;
import Scripts.FetchAndPersist;
import UserParsing.FetchUsers;

public class Main {

    public static void main(String[] args) {
        String dbUrl = System.getProperty("hibernate.hikari.dataSource.url",
                System.getenv().getOrDefault("DB_URL", "jdbc:postgresql://localhost:5432/anime_db"));
        String dbUser = System.getProperty("hibernate.hikari.dataSource.user",
                System.getenv().getOrDefault("DB_USER", "rudeus"));
        String dbPass = System.getProperty("hibernate.hikari.dataSource.password",
                System.getenv().getOrDefault("DB_PASS", "sylphi_pants"));

        boolean fetchOnlyUsersAndStats = Boolean.parseBoolean(System.getenv().getOrDefault(
                "FETCH_ONLY_USERS_AND_STATS", "TRUE"));
        boolean fetchOnlyAnimeList = Boolean.parseBoolean(System.getenv().getOrDefault(
                "FETCH_ONLY_ANIME_LIST", "FALSE"));
        boolean runFullRestoringDataIntegrity = Boolean.parseBoolean(System.getenv().getOrDefault(
                "RUN_FULL_RESTORING_DATA_INTEGRITY", "FALSE"));

        String pagesEnv = System.getenv().getOrDefault("NUMBER_OF_PAGE_FROM_ANIME_TOP", "null");
        Integer numberOfPages = null;
        if (pagesEnv != null && !pagesEnv.equalsIgnoreCase("null") && !pagesEnv.isBlank()) {
            try { numberOfPages = Integer.parseInt(pagesEnv); } catch (NumberFormatException ignored) {}
        }

        int numberOfUsers = Integer.parseInt(System.getenv().getOrDefault(
                "NUMBER_OF_USER_TO_FETCH", "1000"));
        int minNumberOfAnimeInLists = Integer.parseInt(System.getenv().getOrDefault(
                "MINIMUM_NUMBER_OF_ANIME_IN_USER_LISTS", "50"));
        int minNumberOfCompletedAnimeInLists = Integer.parseInt(System.getenv().getOrDefault(
                "MINIMUM_NUMBER_OF_COMPLETED_ANIME_IN_USER_LISTS", "0"));
        int batchSize = Integer.parseInt(System.getenv().getOrDefault("BATCH_SIZE", "50"));
        double thresholdPercentage = Double.parseDouble(System.getenv().getOrDefault(
                "THRESHOLD_PERCENTAGE", "0.05"));

        System.out.println("CONFIG:");
        System.out.println(" DB_URL=" + dbUrl);
        System.out.println(" DB_USER=" + dbUser);
        System.out.println(" JIKAN_BASE=" + System.getenv().getOrDefault("JIKAN_BASE",
                "https://api.jikan.moe/v4"));
        System.out.println(" fetchOnlyUsersAndStats=" + fetchOnlyUsersAndStats);
        System.out.println(" fetchOnlyAnimeList=" + fetchOnlyAnimeList);
        System.out.println(" numberOfPages=" + numberOfPages);
        System.out.println(" numberOfUsers=" + numberOfUsers);
        System.out.println(" minAnimeInLists=" + minNumberOfAnimeInLists);
        System.out.println(" batchSize=" + batchSize);
        System.out.println(" thresholdPercentage=" + thresholdPercentage);

        FetchAndPersist fetching;
        if (numberOfPages == null) {
            fetching = new FetchAndPersist(numberOfUsers, minNumberOfAnimeInLists,
                    null, minNumberOfCompletedAnimeInLists);
        } else {
            fetching = new FetchAndPersist(numberOfUsers, minNumberOfAnimeInLists, numberOfPages, minNumberOfCompletedAnimeInLists);
        }

        if (fetchOnlyAnimeList) {
            System.out.println("Run fetching anime top list");
            fetching.fillAnimeDB();
            System.out.println("Anime parsing finished.");
            return;
        }

        if (fetchOnlyUsersAndStats) {
            System.out.println("Run fetching users and stats");
            fetching.fillUserDB();
            System.out.println("Users parsing finished.");
        } else {
            System.out.println("Rum full pipeline (anime -> users -> restore)");
            fetching.fillAnimeDB();
            fetching.fillUserDB();
        }
        if (runFullRestoringDataIntegrity) {
            System.out.println("Run restoring data integrity (DataIntegrityRestorer)");
            DataIntegrityRestorer dataIntegrityRestorer = new DataIntegrityRestorer(thresholdPercentage,
                    batchSize, 100);
            dataIntegrityRestorer.run();
        }

        System.out.println("Done.");
    }
}
