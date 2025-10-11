package scripts;

import utils.SimpleDataExtract;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class DataOutputToFile {

    private final int minimumNumberOfAnimeInUserLists;
    private final int minimumNumberOfCompletedAnimeInUserLists;
    private final int minimumNumberOfRatedAnimeInUserLists;
    private boolean isShowSql;
    private final SimpleDataExtract extractor;

    private List<String> userAnimeColumns = List.of(
            "user_id", "anime_id", "score", "status", "last_updated"
    );
    private List<String> animeColumns = List.of(
            "mal_id","episodes","score","scored_by", "rating", "season", "type",
            "title", "synopsis", "status", "year"
    );

    private static final String GENRES = """
    (SELECT STRING_AGG(DISTINCT g.name, ', ')
     FROM genre g
     JOIN anime_genre ag ON g.mal_id = ag.genre_id
     WHERE ag.anime_id = a.mal_id) AS genres
    """;

    private static final String THEMES = """
    (SELECT STRING_AGG(DISTINCT g.name, ', ')
     FROM genre g
     JOIN anime_theme ag ON g.mal_id = ag.genre_id
     WHERE ag.anime_id = a.mal_id) AS themes
    """;

    private static final String DEMOGRAPHICS = """
    (SELECT STRING_AGG(DISTINCT d.name, ', ')
     FROM demographic d
     JOIN anime_demographic ad ON d.mal_id = ad.demographic_id
     WHERE ad.anime_id = a.mal_id) AS demographics
    """;

    private static final String STUDIOS = """
    (SELECT STRING_AGG(DISTINCT s.name, ', ')
     FROM producer s
     JOIN anime_studio as_ ON s.mal_id = as_.producer_id
     WHERE as_.anime_id = a.mal_id) AS studios
    """;

    private static final List<String> ANIME_JOINS_COLUMNS = List.of(
            GENRES, THEMES, DEMOGRAPHICS, STUDIOS
    );
    private static final List<String> ANIME_EVALUATION_JOIN_COLUMNS = List.of();

    private List<String> animeEvaluationFilters = List.of("us.completed > ?", "us.total_entries > ?",
            "r.score IS NOT NULL", "us.mean_score BETWEEN 3 AND 9.5");
    private List<String> animeFilters = List.of("a.approved = true");

    public void setUserAnimeColumns(List<String> userAnimeColumns) {
        this.userAnimeColumns = userAnimeColumns;
    }

    public void setAnimeColumns(List<String> animeColumns) {
        this.animeColumns = animeColumns;
    }

    public void setAnimeFilters(List<String> animeFilters) {
        this.animeFilters = animeFilters;
    }

    public void setAnimeEvaluationFilters(List<String> animeEvaluationFilters) {
        this.animeEvaluationFilters = animeEvaluationFilters;
    }

    public void setIsShowSql(boolean isShowSql) {
        this.isShowSql = isShowSql;
    }

    public List<String> getAnimeFilters() {
        return animeFilters;
    }

    public List<String> getAnimeEvaluationFilters() {
        return animeEvaluationFilters;
    }

    public List<String> getAnimeColumns() {
        return animeColumns;
    }

    public List<String> getUserAnimeColumns() {
        return userAnimeColumns;
    }

    public int getMinimumNumberOfAnimeInUserLists() {
        return minimumNumberOfAnimeInUserLists;
    }

    public int getMinimumNumberOfCompletedAnimeInUserLists() {
        return minimumNumberOfCompletedAnimeInUserLists;
    }

    public DataOutputToFile(int minNumberOfAnimeInLists, int minNumberOfCompletedAnimeInLists,
                            int minNumberOfRatedAnimeInLists, Properties dbProps) {
        this.minimumNumberOfAnimeInUserLists = minNumberOfAnimeInLists;
        this.minimumNumberOfCompletedAnimeInUserLists = minNumberOfCompletedAnimeInLists;
        this.minimumNumberOfRatedAnimeInUserLists = minNumberOfRatedAnimeInLists;
        this.extractor = new SimpleDataExtract(dbProps, 1000);
    }

    public DataOutputToFile(Properties dbProps) {
        this(Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE, dbProps);
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
                buildColumnsForTable("r", userAnimeColumns, ANIME_EVALUATION_JOIN_COLUMNS)) +
                "\nJOIN\n" +
                "user_stat us ON us.user_id = r.user_id" +
                "\nJOIN (\n" +
                " SELECT user_id" +
                " FROM user_anime_stat" +
                " WHERE score IS NOT NULL" +
                " GROUP BY user_id " +
                " HAVING COUNT(score) > ? " +
                " ) AS active_users ON r.user_id = active_users.user_id\n" +
                buildFilter(animeEvaluationFilters);
        if (isShowSql)
            System.out.println(usersSql);
        extractor.exportQueryToParquet(usersSql,
                List.of(minimumNumberOfCompletedAnimeInUserLists,
                        minimumNumberOfAnimeInUserLists,
                        minimumNumberOfRatedAnimeInUserLists),
                outFile, Set.of("user_id"));
    }

    private void saveAnimes(File outFile) throws Exception {
        String animeSql = buildSelect("anime a ",
                buildColumnsForTable("a", animeColumns, ANIME_JOINS_COLUMNS)) +
                buildFilter(animeFilters);
        if (isShowSql)
            System.out.println(animeSql);
        extractor.exportQueryToParquet(animeSql, null, outFile, null);
    }

    public void extractUsersToFile(File outDir) {
        Path out = outDir.toPath();
        try {
            Files.createDirectories(out);
        } catch (IOException e) {System.out.println("Directory creation failed");}

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
}
