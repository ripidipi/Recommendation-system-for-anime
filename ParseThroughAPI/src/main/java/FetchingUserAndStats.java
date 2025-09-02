import Scripts.FetchAndPersist;

public class FetchingUserAndStats {

    public static void main(String[] args) {
        String dbUrl = System.getProperty("hibernate.hikari.dataSource.url",
                System.getenv().getOrDefault("DB_URL", "jdbc:postgresql://localhost:5432/anime_db"));
        String dbUser = System.getProperty("hibernate.hikari.dataSource.user",
                System.getenv().getOrDefault("DB_USER", "rudeus"));
        String dbPass = System.getProperty("hibernate.hikari.dataSource.password",
                System.getenv().getOrDefault("DB_PASS", "sylphi_pants"));

        int numberOfUsers = Integer.parseInt(System.getenv().getOrDefault(
                "NUMBER_OF_USER_TO_FETCH", "10000"));
        int minNumberOfAnimeInLists = Integer.parseInt(System.getenv().getOrDefault(
                "MINIMUM_NUMBER_OF_ANIME_IN_USER_LISTS", "50"));
        int minNumberOfCompletedAnimeInLists = Integer.parseInt(System.getenv().getOrDefault(
                "MINIMUM_NUMBER_OF_COMPLETED_ANIME_IN_USER_LISTS", "0"));


        System.out.println("CONFIG:");
        System.out.println(" DB_URL=" + dbUrl);
        System.out.println(" DB_USER=" + dbUser);
        System.out.println(" JIKAN_BASE=" + System.getenv().getOrDefault("JIKAN_BASE",
                "https://api.jikan.moe/v4"));
        System.out.println(" numberOfUsers=" + numberOfUsers);
        System.out.println(" minAnimeInLists=" + minNumberOfAnimeInLists);
        System.out.println(" minNumberOfCompletedAnimeInLists=" + minNumberOfCompletedAnimeInLists);

        FetchAndPersist fetching;
        fetching = new FetchAndPersist(numberOfUsers, minNumberOfAnimeInLists,
                null, minNumberOfCompletedAnimeInLists);

        System.out.println("Run fetching users and stats");
        fetching.fillUserDB();
        System.out.println("Users parsing finished.");
    }

}
