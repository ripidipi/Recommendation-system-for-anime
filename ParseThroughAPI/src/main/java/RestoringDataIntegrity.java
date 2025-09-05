import Scripts.DataIntegrityRestorer;

public class RestoringDataIntegrity {

    public static void main(String[] args) {
        String dbUrl = System.getProperty("hibernate.hikari.dataSource.url",
                System.getenv().getOrDefault("DB_URL", "jdbc:postgresql://localhost:5432/anime_db"));
        String dbUser = System.getProperty("hibernate.hikari.dataSource.user",
                System.getenv().getOrDefault("DB_USER", "rudeus"));
        String dbPass = System.getProperty("hibernate.hikari.dataSource.password",
                System.getenv().getOrDefault("DB_PASS", "sylphi_pants"));

        int batchSize = Integer.parseInt(System.getenv().getOrDefault("BATCH_SIZE", "50"));
        double thresholdPercentage = Double.parseDouble(System.getenv().getOrDefault(
                "THRESHOLD_PERCENTAGE", "0.05"));


        System.out.println("CONFIG:");
        System.out.println(" DB_URL=" + dbUrl);
        System.out.println(" DB_USER=" + dbUser);
        System.out.println(" batchSize=" + batchSize);
        System.out.println(" thresholdPercentage=" + thresholdPercentage);

        System.out.println("Run restoring data integrity (DataIntegrityRestorer)");
        DataIntegrityRestorer dataIntegrityRestorer = new DataIntegrityRestorer(thresholdPercentage,
                batchSize, 500);
        dataIntegrityRestorer.run();
    }

}
