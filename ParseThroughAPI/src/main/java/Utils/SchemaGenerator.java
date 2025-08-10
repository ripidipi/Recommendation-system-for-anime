package Utils;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

public class SchemaGenerator {
    public static void main(String[] args) {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("animePU");
        emf.close();
        System.out.println("Schema generated!");
    }
}