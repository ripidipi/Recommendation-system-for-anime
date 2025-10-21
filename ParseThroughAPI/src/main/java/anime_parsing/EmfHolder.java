package anime_parsing;

import exeptions.ParserException;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;


public final class EmfHolder {

    private static volatile EntityManagerFactory emf;
    private static final Object LOCK = new Object();
    private static final Logger LOGGER = LoggerFactory.getLogger(EmfHolder.class);

    private static volatile boolean disabledForTests = Boolean.getBoolean("tests.disableDb");

    private EmfHolder() {}

    public static void disableForTests() {
        disabledForTests = true;
        closeEmf();
    }

    public static EntityManagerFactory setEmfForTests(EntityManagerFactory testEmf) {
        synchronized (LOCK) {
            disabledForTests = false;
            EntityManagerFactory old = emf;
            emf = testEmf;
            return old;
        }
    }

    public static EntityManagerFactory getEmf() {
        if (disabledForTests) {
            return null;
        }
        if (emf == null) {
            synchronized (LOCK) {
                if (emf == null) {
                    emf = createEmfInternal();
                }
            }
        }
        return emf;
    }

    private static EntityManagerFactory createEmfInternal() {
        try {
            Map<String, Object> props = new HashMap<>();
            props.put("hibernate.bytecode.provider", "javassist");
            return Persistence.createEntityManagerFactory("animePU", props);
        } catch (Exception e) {
            throw new ParserException("Failed to initialize EntityManagerFactory", e);
        }
    }

    public static void closeEmf() {
        synchronized (LOCK) {
            if (emf != null) {
                try {
                    emf.close();
                } catch (Exception e) {
                    LOGGER.warn("Error closing EntityManagerFactory", e);
                } finally {
                    emf = null;
                }
            }
        }
    }
}
