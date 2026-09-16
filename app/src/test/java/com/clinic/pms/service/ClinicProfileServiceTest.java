package com.clinic.pms.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.clinic.pms.config.AppPaths;
import com.clinic.pms.config.PersistenceConfig;
import com.clinic.pms.repository.DoctorClinicProfileRepository;
import jakarta.persistence.EntityManager;
import java.nio.file.Files;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ClinicProfileServiceTest {

    private ClinicProfileService clinicProfileService;

    @BeforeEach
    void setUp() throws Exception {
        resetDatabase();
        EntityManager em = PersistenceConfig.newEntityManager();
        clinicProfileService = new ClinicProfileService(new DoctorClinicProfileRepository(em));
    }

    @AfterEach
    void tearDown() {
        PersistenceConfig.shutdown();
    }

    @Test
    void savesGetsAndUpdatesSingletonClinicProfile() {
        clinicProfileService.saveInitialProfile("Dr One", "Clinic One", "Address One", "REG1");
        assertEquals("Clinic One", clinicProfileService.get().getClinicName());

        clinicProfileService.update("Dr Two", "Clinic Two", "Address Two", "REG2");
        assertEquals("Dr Two", clinicProfileService.get().getDoctorName());
        assertEquals("REG2", clinicProfileService.get().getRegistrationNumber());
    }

    private void resetDatabase() throws Exception {
        PersistenceConfig.shutdown();
        Files.deleteIfExists(AppPaths.h2DatabasePath().resolveSibling("patientdb.mv.db"));
        Files.deleteIfExists(AppPaths.h2DatabasePath().resolveSibling("patientdb.trace.db"));
        PersistenceConfig.initialize();
    }
}