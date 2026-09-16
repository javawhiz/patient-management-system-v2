package com.clinic.pms.service;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.clinic.pms.config.AppPaths;
import com.clinic.pms.config.PersistenceConfig;
import com.clinic.pms.repository.BackupMetadataRepository;
import jakarta.persistence.EntityManager;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class H2NativeBackupServiceTest {

    private static EntityManager entityManager;
    private static H2NativeBackupService service;
    private static Path output;

    @BeforeAll
    static void setUp() throws Exception {
        Files.deleteIfExists(AppPaths.h2DatabasePath().resolveSibling("patientdb.mv.db"));
        Files.deleteIfExists(AppPaths.h2DatabasePath().resolveSibling("patientdb.trace.db"));
        PersistenceConfig.initialize();
        entityManager = PersistenceConfig.newEntityManager();
        BackupService backupService = new BackupService(entityManager, new BackupMetadataRepository(entityManager));
        service = new H2NativeBackupService(entityManager, backupService);
        output = Files.createTempFile("pms-h2-test-", ".h2.sql");
    }

    @AfterAll
    static void tearDown() throws Exception {
        PersistenceConfig.shutdown();
        Files.deleteIfExists(output);
    }

    @Test
    void exportsFullNonDestructiveH2Script() throws Exception {
        service.exportScript(output);
        String script = Files.readString(output);

        assertTrue(script.startsWith(H2NativeBackupService.BACKUP_HEADER));
        assertTrue(script.contains("CREATE "));
        assertTrue(script.contains("PATIENT"));
        assertTrue(script.contains("DOCTOR_CREDENTIAL"));
        assertTrue(script.contains("CREATE USER") == false);
        assertTrue(script.toUpperCase().contains("DROP TABLE") == false);
    }

    @Test
    void restoresIntoFreshDatabaseAndReopensIt() throws Exception {
        service.exportScript(output);
        service.restoreScript(output);
        PersistenceConfig.initialize();

        try (EntityManager restoredEntityManager = PersistenceConfig.newEntityManager()) {
            Long tableCount = (Long) restoredEntityManager.createNativeQuery(
                    "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES "
                            + "WHERE TABLE_SCHEMA = 'PUBLIC' AND TABLE_NAME = 'PATIENT'")
                    .getSingleResult();
            assertTrue(tableCount == 1);
        }
    }
}
