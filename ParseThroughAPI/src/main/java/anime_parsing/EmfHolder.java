package anime_parsing;

import exeptions.ParserException;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class EmfHolder {
    static final EntityManagerFactory EMF = createEmf();
    private static final Logger LOGGER = LoggerFactory.getLogger(EmfHolder.class);

    private EmfHolder() {}

    public static EntityManagerFactory createEmf() {
        try {
            Map<String, Object> props = new HashMap<>();
            props.put("hibernate.bytecode.provider", "javassist");
            return Persistence.createEntityManagerFactory("animePU", props);
        } catch (Exception e) {
            LOGGER.error("Failed to create EntityManagerFactory", e);
            throw new ParserException("Failed to initialize EntityManagerFactory", e);
        }
    }
}