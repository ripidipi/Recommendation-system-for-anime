package Scripts;

import Utils.SimpleDataExtract;

import java.io.File;
import java.util.*;

public class DataOutputToFile {

    private int MINIMUM_NUMBER_OF_ANIME_IN_USER_LISTS;
    private int MINIMUM_NUMBER_OF_COMPLETED_ANIME_IN_USER_LISTS;
    private int QUANTITY_OF_USER_TO_EXTRACT;
    private final SimpleDataExtract extractor;

    private static List<String> USER_ANIME_COLUMNS = List.of(
            "user_id", "anime_id", "score", "status"
    );
    private static List<String> ANIME_COLUMNS = List.of(
            "mal_id","episodes","score","scored_by", "rating", "season", "type",
            "title", "synopsis", "status"
    );
    private static List<String> ANIME_JOINS_COLUMNS = List.of(
            "(SELECT STRING_AGG(DISTINCT g.name, ', ')\n" +
                    "FROM genre g\n" +
                    "JOIN anime_genre ag ON g.mal_id = ag.genre_id\n" +
                    "WHERE ag.anime_id = a.mal_id) AS genres",
            "(SELECT STRING_AGG(DISTINCT g.name, ', ')\n" +
                    "FROM genre g\n" +
                    "JOIN anime_theme ag ON g.mal_id = ag.genre_id\n" +
                    "WHERE ag.anime_id = a.mal_id) AS themes",
            "(SELECT STRING_AGG(DISTINCT d.name, ', ')\n" +
                    "FROM demographic d\n" +
                    "JOIN anime_demographic ad ON d.mal_id = ad.demographic_id\n" +
                    "WHERE ad.anime_id = a.mal_id) AS demographics",
            "(SELECT STRING_AGG(DISTINCT s.name, ', ')\n" +
                    "FROM producer s\n" +
                    "JOIN anime_studio as_ ON s.mal_id = as_.producer_id\n" +
                    "WHERE as_.anime_id = a.mal_id) AS studios"
    );
    private static List<String> ANIME_EVALUATION = List.of();
    private static List<String> ANIME_EVALUATION_FILTERS = List.of("us.completed > ?", "us.total_entries > ?",
            "r.score IS NOT NULL");
    private static List<String> ANIME_FILTERS = List.of();


    public static void setUserAnimeColumns(List<String> userAnimeColumns) {
        USER_ANIME_COLUMNS = userAnimeColumns;
    }

    public static void setAnimeColumns(List<String> animeColumns) {
        ANIME_COLUMNS = animeColumns;
    }

    public static void setAnimeJoins(List<String> animeJoins) {
        ANIME_JOINS_COLUMNS = animeJoins;
    }

    public static void setAnimeEvaluation(List<String> animeEvaluation) {
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

    private String buildSelect(String table, List<String> columns) {
        return "SELECT " + String.join(",\n", columns) + " FROM " + table;
    }

    private String buildFilter(List<String> filters) {
        if (filters == null || filters.isEmpty()) {
            return "";
        }
        return "WHERE " + String.join(" AND ", filters);
    }

    private List<String> buildColumnsForTable(String tableVariable, List<String> columns, List<String> rowLines) {
        List<String> newColumns = new ArrayList<>();
        for (String column : columns) {
            newColumns.add(tableVariable + "." + column);
        }
        if (rowLines != null) newColumns.addAll(rowLines);
        return newColumns;
    }

    private void saveEvaluations(File outFile) throws Exception {
        String usersSql = buildSelect("user_anime_stat r",
                buildColumnsForTable("r", USER_ANIME_COLUMNS, ANIME_EVALUATION)) +
                "\nJOIN\n" +
                "user_stat us ON us.user_id = r.user_id\n" +
                buildFilter(ANIME_EVALUATION_FILTERS);

        extractor.exportQueryToParquet(usersSql,
                List.of(MINIMUM_NUMBER_OF_COMPLETED_ANIME_IN_USER_LISTS, MINIMUM_NUMBER_OF_ANIME_IN_USER_LISTS),
                outFile, Set.of("user_id"));
    }

    private void saveAnimes(File outFile) throws Exception {
        String animeSql = buildSelect("anime a",
                buildColumnsForTable("a", ANIME_COLUMNS, ANIME_JOINS_COLUMNS)) +
                buildFilter(ANIME_FILTERS);
        extractor.exportQueryToParquet(animeSql, null, outFile, null);
    }

    public void extractUsersToFile(File outDir) {
        if (!outDir.exists()) outDir.mkdirs();
        try {
            Scanner sc = new Scanner(System.in);

            File animeFile = new File(outDir, "anime.parquet");
            if (animeFile.exists()) {
                System.out.print("File with anime list, Have already existed. Do you want to skip it? (y/n)");
                String line = sc.nextLine();
                if (!line.equalsIgnoreCase("y")) {
                    animeFile.delete();
                    saveAnimes(animeFile);
                }
            } else {
                saveAnimes(animeFile);
            }

            File evaluationsFile = new File(outDir, "evaluations.parquet");
            if (evaluationsFile.exists()) {
                System.out.print("File with users evaluation, Have already existed. Do you want to skip it? (y/n)");
                String line = sc.nextLine();
                if (!line.equalsIgnoreCase("y")) {
                    evaluationsFile.delete();
                    saveEvaluations(evaluationsFile);
                }
            } else {
                saveEvaluations(evaluationsFile);
            }

            System.out.println("Export finished to " + outDir.getAbsolutePath());
        } catch ( Exception e ) {
            System.out.println("Error while exporting: " + e.getMessage());
            e.printStackTrace();
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
