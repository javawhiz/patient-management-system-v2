package com.clinic.pms.service;

import static org.junit.jupiter.api.Assertions.*;

import com.clinic.pms.config.AppPaths;
import com.clinic.pms.config.PersistenceConfig;
import com.clinic.pms.repository.BackupMetadataRepository;
import jakarta.persistence.EntityManager;
import java.nio.file.Files;
import java.time.LocalDateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BackupReminderServiceTest {

    private BackupMetadataRepository repository;
    private BackupReminderService reminderService;

    @BeforeEach
    void setUp() throws Exception {
        resetDatabase();
        EntityManager em = PersistenceConfig.newEntityManager();
        repository = new BackupMetadataRepository(em);
        reminderService = new BackupReminderService(repository);
    }

    @AfterEach
    void tearDown() {
        PersistenceConfig.shutdown();
    }

    @Test
    void reminderIsDueWhenNeverBackedUpOrOlderThanFiveDays() {
        assertTrue(reminderService.isReminderDue());

        var metadata = repository.find().orElseThrow();
        metadata.setLastBackupAt(LocalDateTime.now().minusDays(4));
        repository.save(metadata);
        assertFalse(reminderService.isReminderDue());

        metadata.setLastBackupAt(LocalDateTime.now().minusDays(6));
        repository.save(metadata);
        assertTrue(reminderService.isReminderDue());
    }

    private void resetDatabase() throws Exception {
        PersistenceConfig.shutdown();
        Files.deleteIfExists(AppPaths.h2DatabasePath().resolveSibling("patientdb.mv.db"));
        Files.deleteIfExists(AppPaths.h2DatabasePath().resolveSibling("patientdb.trace.db"));
        PersistenceConfig.initialize();
    }
}