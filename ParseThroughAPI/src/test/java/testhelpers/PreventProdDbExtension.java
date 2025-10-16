package testhelpers;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

public class PreventProdDbExtension implements BeforeAllCallback {
    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        String[] keys = new String[] {"JDBC_URL", "SPRING_DATASOURCE_URL", "DATABASE_URL", "JDBC_DATABASE_URL"};
        for (String k : keys) {
            String v = System.getProperty(k);
            if (v == null) v = System.getenv(k);
            if (v != null) {
                String low = v.toLowerCase();
                if (low.contains("prod") || low.contains("production") || low.contains("aws") || low.contains("rds") || low.contains("amazonaws")) {
                    throw new IllegalStateException("Refusing to run tests: suspicious DB URL: " + v);
                }
            }
        }
    }
}
