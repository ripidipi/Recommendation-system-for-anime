package Scripts;

import Utils.SimpleDataExtract;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

public class DataOutputToFile {

    private int MINIMUM_NUMBER_OF_ANIME_IN_USER_LISTS;
    private int MINIMUM_NUMBER_OF_COMPLETED_ANIME_IN_USER_LISTS;
    private int QUANTITY_OF_USER_TO_EXTRACT;
    private final SimpleDataExtract extractor;

    private static Set<String> USER_ANIME_COLUMNS = Set.of(
            "user_id", "anime_id", "score", "status"
    );
    private static Set<String> ANIME_COLUMNS = Set.of(
            "mal_id","episodes","score","scored_by", "rating", "season", "type",
            "title", "synopsis", "status"
    );
    private static Set<String> ANIME_JOINS_COLUMNS = Set.of(
            """
                    (SELECT STRING_AGG(DISTINCT g.name, ', ')
                         FROM genre g
                         JOIN anime_genre ag ON g.mal_id = ag.genre_id
                         WHERE ag.anime_id = a.mal_id) AS genres
            """, """
                    (SELECT STRING_AGG(DISTINCT g.name, ', ')
                         FROM genre g
                         JOIN anime_theme ag ON g.mal_id = ag.genre_id
                         WHERE ag.anime_id = a.mal_id) AS themes
            """, """
                    (SELECT STRING_AGG(DISTINCT d.name, ', ')
                         FROM demographic d
                         JOIN anime_demographic ad ON d.mal_id = ad.demographic_id
                         WHERE ad.anime_id = a.mal_id) AS demographics
            """, """
                    (SELECT STRING_AGG(DISTINCT s.name, ', ')
                         FROM producer s
                         JOIN anime_studio as_ ON s.mal_id = as_.producer_id
                         WHERE as_.anime_id = a.mal_id) AS studios
            """);
    private static Set<String> ANIME_EVALUATION = Set.of();
    private static Set<String> ANIME_EVALUATION_FILTERS = Set.of("us.total_entries > ?", "us.completed > ?");
    private static Set<String> ANIME_FILTERS = Set.of();


    public static void setUserAnimeColumns(Set<String> userAnimeColumns) {
        USER_ANIME_COLUMNS = userAnimeColumns;
    }

    public static void setAnimeColumns(Set<String> animeColumns) {
        ANIME_COLUMNS = animeColumns;
    }

    public static void setAnimeJoins(Set<String> animeJoins) {
        ANIME_JOINS_COLUMNS = animeJoins;
    }

    public static void setAnimeEvaluation(Set<String> animeEvaluation) {
        ANIME_EVALUATION = animeEvaluation;
    }

    public DataOutputToFile(int minNumberOfAnimeInLists, int minNumberOfCompletedAnimeInLists,
                            int quantityOfUserToExtract, Properties dbProps) {
        this.MINIMUM_NUMBER_OF_ANIME_IN_USER_LISTS = minNumberOfAnimeInLists;
        this.MINIMUM_NUMBER_OF_COMPLETED_ANIME_IN_USER_LISTS = minNumberOfCompletedAnimeInLists;
        this.QUANTITY_OF_USER_TO_EXTRACT = quantityOfUserToExtract;
        this.extractor = new SimpleDataExtract(dbProps, 1000);
    }

    public DataOutputToFile(int minNumberOfAnimeInLists, int minNumberOfCompletedAnimeInLists,
                            Properties dbProps) {
        this(minNumberOfAnimeInLists, minNumberOfCompletedAnimeInLists, Integer.MAX_VALUE, dbProps);
    }

    public DataOutputToFile(Properties dbProps) {
        this(Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE, dbProps);
    }

    public void close() { extractor.close(); }

    private String buildSelect(String table, Set<String> columns) {
        return "SELECT " + String.join(",\n", columns) + " FROM " + table;
    }

    private String buildFilter(Set<String> filters) {
        if (filters.isEmpty()) {
            return "";
        }
        return "WHERE " + String.join(" AND ", filters);
    }

    private Set<String> buildColumnsForTable(String table_variable, Set<String> columns, Set<String> rowLines) {
        Set<String> newColumns = new HashSet<>();
        for (String column : columns) {
            newColumns.add(table_variable + "." + column);
        }
        newColumns.addAll(rowLines);
        return newColumns;
    }

    public void extractUsersToFile(File outDir) {
        if (!outDir.exists()) outDir.mkdirs();
        try {
            File animeFile = new File(outDir, "anime.parquet");
            String animeSql = buildSelect("anime a",
                    buildColumnsForTable("a", ANIME_COLUMNS, ANIME_JOINS_COLUMNS)) +
                    buildFilter(ANIME_FILTERS);
            extractor.exportQueryToParquet(animeSql, null, animeFile, null);

            File usersFile = new File(outDir, "users.parquet");
            String usersSql = buildSelect("user_anime_stat r",
                    buildColumnsForTable("r", USER_ANIME_COLUMNS, ANIME_EVALUATION)) +
                            """
                            \nJOIN
                                user_stat us ON us.user_id = r.user_id
                            """ +
                    buildFilter(ANIME_EVALUATION_FILTERS);
            System.out.println(usersSql);
            extractor.exportQueryToParquet(usersSql, List.of(MINIMUM_NUMBER_OF_ANIME_IN_USER_LISTS,
                    MINIMUM_NUMBER_OF_COMPLETED_ANIME_IN_USER_LISTS), usersFile, Set.of("user_id"));

            System.out.println("Export finished to " + outDir.getAbsolutePath());
        } catch ( Exception e ) {
            System.out.println(e.getMessage());
        }
    }


    public static void main(String[] args) {
        Properties dbProps = new Properties();
        String dbUrl = System.getProperty("hibernate.hikari.dataSource.url",
                System.getenv().getOrDefault("DB_URL", "jdbc:postgresql://localhost:5432/anime_db"));
        String dbUser = System.getProperty("hibernate.hikari.dataSource.user",
                System.getenv().getOrDefault("DB_USER", "rudeus"));
        String dbPass = System.getProperty("hibernate.hikari.dataSource.password",
                System.getenv().getOrDefault("DB_PASS", "sylphi_pants"));
        dbProps.setProperty("jdbc.url", dbUrl);
        dbProps.setProperty("jdbc.user", dbUser);
        dbProps.setProperty("jdbc.password", dbPass);
        DataOutputToFile outFile = new DataOutputToFile(dbProps);
        File outDir = new File("out");
        outFile.extractUsersToFile(outDir);
    }
}
