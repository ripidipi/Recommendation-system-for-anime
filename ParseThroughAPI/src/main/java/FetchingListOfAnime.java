import Scripts.FetchAndPersist;

public class FetchingListOfAnime {

    public static void main(String[] args) {
        String dbUrl = System.getProperty("hibernate.hikari.dataSource.url",
                System.getenv().getOrDefault("DB_URL", "jdbc:postgresql://localhost:5432/anime_db"));
        String dbUser = System.getProperty("hibernate.hikari.dataSource.user",
                System.getenv().getOrDefault("DB_USER", "rudeus"));
        String dbPass = System.getProperty("hibernate.hikari.dataSource.password",
                System.getenv().getOrDefault("DB_PASS", "sylphi_pants"));

        String pagesEnv = System.getenv().getOrDefault("NUMBER_OF_PAGE_FROM_ANIME_TOP", "null");
        Integer numberOfPages = null;
        if (pagesEnv != null && !pagesEnv.equalsIgnoreCase("null") && !pagesEnv.isBlank()) {
            try { numberOfPages = Integer.parseInt(pagesEnv); } catch (NumberFormatException ignored) {}
        }

        System.out.println("CONFIG:");
        System.out.println(" DB_URL=" + dbUrl);
        System.out.println(" DB_USER=" + dbUser);
        System.out.println(" JIKAN_BASE=" + System.getenv().getOrDefault("JIKAN_BASE",
                "https://api.jikan.moe/v4"));
        System.out.println(" numberOfPages=" + numberOfPages);

        FetchAndPersist fetching;
        fetching = new FetchAndPersist(null, null, numberOfPages, null);

        System.out.println("Run fetching anime top list");
        fetching.fillAnimeDB();
        System.out.println("Anime parsing finished.");
    }


}
