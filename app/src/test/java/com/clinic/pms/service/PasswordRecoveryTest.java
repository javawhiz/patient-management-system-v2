package com.clinic.pms.service;

import static org.junit.jupiter.api.Assertions.*;

import com.clinic.pms.config.AppPaths;
import com.clinic.pms.config.PersistenceConfig;
import com.clinic.pms.repository.DoctorCredentialRepository;
import jakarta.persistence.EntityManager;
import java.nio.file.Files;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PasswordRecoveryTest {

    private AuthService authService;

    @BeforeEach
    void setUp() throws Exception {
        resetDatabase();
        EntityManager em = PersistenceConfig.newEntityManager();
        authService = new AuthService(new DoctorCredentialRepository(em));
        authService.createInitialCredential("doctor", "Secret123");
    }

    @AfterEach
    void tearDown() {
        PersistenceConfig.shutdown();
    }

    @Test
    void resetRequiresCorrectRecoveryAnswerAndValidPassword() {
        assertFalse(authService.resetPassword("doctor", "wrong", "NewSecret123"));
        assertFalse(authService.resetPassword("doctor", "garima.j89@gmail.com", ""));
        assertTrue(authService.login("doctor", "Secret123"));

        assertTrue(authService.resetPassword("doctor", "garima.j89@gmail.com", "NewSecret123"));
        assertFalse(authService.login("doctor", "Secret123"));
        assertTrue(authService.login("doctor", "NewSecret123"));
    }

    private void resetDatabase() throws Exception {
        PersistenceConfig.shutdown();
        Files.deleteIfExists(AppPaths.h2DatabasePath().resolveSibling("patientdb.mv.db"));
        Files.deleteIfExists(AppPaths.h2DatabasePath().resolveSibling("patientdb.trace.db"));
        PersistenceConfig.initialize();
    }
}