import scripts.DataOutputToFile;

import java.io.File;
import java.util.List;
import java.util.Properties;

public class SaveDataFromSQLToParquetFile {

    public static void main(String[] args) {
        System.setProperty("java.security.manager", "allow");
        Properties dbProps = new Properties();
        String dbUrl = System.getProperty("hibernate.hikari.dataSource.url",
                System.getenv().getOrDefault("DB_URL", "jdbc:postgresql://localhost:5432/anime_db"));
        String dbUser = System.getProperty("hibernate.hikari.dataSource.user",
                System.getenv().getOrDefault("DB_USER", "rudeus"));
        String dbPass = System.getProperty("hibernate.hikari.dataSource.password",
                System.getenv().getOrDefault("DB_PASS", "sylphi_pants"));

        int minimalFinished = Integer.parseInt(System.getenv().getOrDefault(
                "MINIMUM_NUMBER_OF_ANIME_IN_USER_LISTS", "50"));
        int minimalCompleted = Integer.parseInt(System.getenv().getOrDefault(
                "MINIMUM_NUMBER_OF_COMPLETED_ANIME", "10"));
        int minNumberOfRatedAnimeInLists = Integer.parseInt(System.getenv().getOrDefault(
                "MINIMUM_NUMBER_OF_RATED_ANIME_IN_USER_LISTS", "25"));
        String animeColumns = (System.getenv().getOrDefault(
                "USER_ANIME_COLUMNS", null));
        String usersAnimeColumns = System.getenv().getOrDefault(
                "ANIME_COLUMNS", null);
        String animeFilters = (System.getenv().getOrDefault(
                "ANIME_FILTERS", null));
        String userFilters = System.getenv().getOrDefault(
                "ANIME_EVALUATION_FILTERS", null);
        Boolean showSQL = Boolean.parseBoolean(System.getenv().getOrDefault(
                "SHOW_SQL", "true"));

        dbProps.setProperty("jdbc.url", dbUrl);
        dbProps.setProperty("jdbc.user", dbUser);
        dbProps.setProperty("jdbc.password", dbPass);
        DataOutputToFile outFile = new DataOutputToFile(minimalFinished,
                minimalCompleted, minNumberOfRatedAnimeInLists, dbProps);

        System.out.println("CONFIG:");
        System.out.println(" DB_URL=" + dbUrl);
        System.out.println(" DB_USER=" + dbUser);
        System.out.println(" minimalNumberOfAnimeInLists=" +
                outFile.getMINIMUM_NUMBER_OF_ANIME_IN_USER_LISTS());
        System.out.println(" minimalNumberOfCompletedAnimeInLists=" +
                outFile.getMINIMUM_NUMBER_OF_COMPLETED_ANIME_IN_USER_LISTS());

        if (animeColumns != null) {
            outFile.setAnimeColumns(List.of(animeColumns.split(",")));
            System.out.println("New anime columns are set up: " + outFile.getAnimeColumns());
        }
        if (usersAnimeColumns != null) {
            outFile.setUserAnimeColumns(List.of(usersAnimeColumns.split(",")));
            System.out.println("New users evaluation columns are set up: " + outFile.getUserAnimeColumns());
        }
        if (animeFilters != null) {
            outFile.setAnimeFilters(List.of(animeFilters.split(",")));
            System.out.println("New anime filters are set up: " + outFile.getAnimeFilters());
        }
        if (userFilters != null) {
            outFile.setAnimeEvaluationFilters(List.of(userFilters.split(",")));
            System.out.println("New users evaluation filters are set up: " + outFile.getAnimeEvaluationFilters());
        }
        if (showSQL) {
            outFile.setSHOW_SQL(true);
            System.out.println("SQl will be displayed in the terminal");
        }
        File outDir = new File("out");

        outFile.extractUsersToFile(outDir);
    }

}
