package com.clinic.pms.config;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import java.util.Map;
import org.flywaydb.core.Flyway;

/**
 * Single {@link EntityManagerFactory} for the app's lifetime. Schema is
 * migrated via Flyway before Hibernate ever validates it (constitution
 * Principle IV: hibernate.hbm2ddl.auto=validate, never auto-generate).
 */
public final class PersistenceConfig {

    private static EntityManagerFactory emf;

    private PersistenceConfig() {}

    public static synchronized void initialize() {
        if (emf != null) {
            return;
        }
        String jdbcUrl = AppPaths.h2JdbcUrl();

        Flyway.configure()
                .dataSource(jdbcUrl, "sa", "")
                .locations("classpath:db/migration")
                .load()
                .migrate();

        Map<String, String> overrides = Map.of(
                "jakarta.persistence.jdbc.url", jdbcUrl,
                "jakarta.persistence.jdbc.user", "sa",
                "jakarta.persistence.jdbc.password", "",
                "jakarta.persistence.jdbc.driver", "org.h2.Driver");

        emf = Persistence.createEntityManagerFactory("pms", overrides);
    }

    public static EntityManager newEntityManager() {
        if (emf == null) {
            throw new IllegalStateException("PersistenceConfig.initialize() must be called first");
        }
        return emf.createEntityManager();
    }

    public static void shutdown() {
        if (emf != null) {
            emf.close();
            emf = null;
        }
    }
}
