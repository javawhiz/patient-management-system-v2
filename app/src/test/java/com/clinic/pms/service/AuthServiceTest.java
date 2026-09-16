package com.clinic.pms.service;

import static org.junit.jupiter.api.Assertions.*;

import com.clinic.pms.config.AppPaths;
import com.clinic.pms.config.PersistenceConfig;
import com.clinic.pms.exception.BadRequestException;
import com.clinic.pms.repository.DoctorCredentialRepository;
import jakarta.persistence.EntityManager;
import java.nio.file.Files;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AuthServiceTest {

    private AuthService authService;

    @BeforeEach
    void setUp() throws Exception {
        resetDatabase();
        EntityManager em = PersistenceConfig.newEntityManager();
        authService = new AuthService(new DoctorCredentialRepository(em));
    }

    @AfterEach
    void tearDown() {
        PersistenceConfig.shutdown();
    }

    @Test
    void createsSingleCredentialAndLogsInWithGenericFailures() {
        assertFalse(authService.isInitialized());
        authService.createInitialCredential("doctor", "Secret123");

        assertTrue(authService.isInitialized());
        assertTrue(authService.login("doctor", "Secret123"));
        assertFalse(authService.login("doctor", "wrong"));
        assertFalse(authService.login("missing", "Secret123"));
        assertThrows(BadRequestException.class,
                () -> authService.createInitialCredential("other", "Secret123"));
    }

    private void resetDatabase() throws Exception {
        PersistenceConfig.shutdown();
        Files.deleteIfExists(AppPaths.h2DatabasePath().resolveSibling("patientdb.mv.db"));
        Files.deleteIfExists(AppPaths.h2DatabasePath().resolveSibling("patientdb.trace.db"));
        PersistenceConfig.initialize();
    }
}