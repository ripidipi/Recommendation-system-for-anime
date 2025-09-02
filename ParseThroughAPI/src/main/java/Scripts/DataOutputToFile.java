package Scripts;

import Utils.SimpleDataExtract;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

public class DataOutputToFile {

    private final int MINIMUM_NUMBER_OF_ANIME_IN_USER_LISTS;
    private final int MINIMUM_NUMBER_OF_COMPLETED_ANIME_IN_USER_LISTS;
    private final int QUANTITY_OF_USER_TO_EXTRACT;
    private final SimpleDataExtract extractor;

    private static Set<String> USER_ANIME_COLUMNS = Set.of(
            "user_id","anime_id","score","status"
    );
    private static Set<String> ANIME_COLUMNS = Set.of(
            "mal_id","episodes","score","scored_by", "rating", "season", "type",
            "title", "synopsis", "status"
    );

    public static void setUserAnimeColumns(Set<String> userAnimeColumns) {
        USER_ANIME_COLUMNS = userAnimeColumns;
    }

    public static void setAnimeColumns(Set<String> animeColumns) {
        ANIME_COLUMNS = animeColumns;
    }

    public static Set<String> getUserAnimeColumns() {
        return USER_ANIME_COLUMNS;
    }

    public static Set<String> getAnimeColumns() {
        return ANIME_COLUMNS;
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
        return "SELECT " + String.join(", ", columns) + " FROM " + table;
    }

    private Set<String> buildColumnsForTable(String table_variable, Set<String> columns) {
        Set<String> newColumns = new HashSet<>();
        for (String column : columns) {
            newColumns.add(table_variable + "." + column);
        }
        return columns;
    }

    public void extractUsersToFile(File outDir) throws Exception {
        if (!outDir.exists()) outDir.mkdirs();

        File animeFile = new File(outDir, "anime.parquet");
        String animeSql = buildSelect("anime", buildColumnsForTable("a", ANIME_COLUMNS));
        extractor.exportQueryToParquet(animeSql, null, animeFile, null);

        File usersFile = new File(outDir, "users.parquet");
        String usersSql = buildSelect("users_anime_stat", buildColumnsForTable("r", USER_ANIME_COLUMNS));
        extractor.exportQueryToParquet(usersSql, null, usersFile, Set.of("user_id"));

        System.out.println("Export finished to " + outDir.getAbsolutePath());
    }

}
