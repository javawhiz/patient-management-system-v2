package com.clinic.pms.service;

import static org.junit.jupiter.api.Assertions.*;

import com.clinic.pms.config.AppPaths;
import com.clinic.pms.config.PersistenceConfig;
import com.clinic.pms.repository.MedicalHistoryRepository;
import com.clinic.pms.repository.PatientRepository;
import jakarta.persistence.EntityManager;
import java.nio.file.Files;
import java.time.LocalDate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PatientServiceTest {

    private PatientService patientService;
    private MedicalHistoryService historyService;

    @BeforeEach
    void setUp() throws Exception {
        resetDatabase();
        EntityManager em = PersistenceConfig.newEntityManager();
        patientService = new PatientService(new PatientRepository(em), new MedicalHistoryRepository(em));
        historyService = new MedicalHistoryService(new MedicalHistoryRepository(em));
    }

    @AfterEach
    void tearDown() {
        PersistenceConfig.shutdown();
    }

    @Test
    void createsUpdatesSearchesSoftDeletesAndRestoresPatients() {
        var patient = patientService.create(data("Jane Doe", "5551234567"));
        historyService.add(patient.getId(), LocalDate.of(2026, 1, 15), "Cough", "Rx", "Notes");

        assertEquals(1, patientService.search("Jane", null, null, null).size());
        assertEquals(1, patientService.search(null, "1234", null, null).size());
        assertEquals(1, patientService.search(null, null, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31)).size());

        patientService.update(patient.getId(), data("Jane Roe", "5559999999"));
        assertEquals("Jane Roe", patientService.getById(patient.getId()).getFullName());

        patientService.softDelete(patient.getId());
        assertTrue(patientService.search("Jane", null, null, null).isEmpty());
        assertEquals(1, patientService.listDeleted().size());

        patientService.restore(patient.getId());
        assertFalse(patientService.getById(patient.getId()).isDeleted());
        assertEquals(1, patientService.search("Jane", null, null, null).size());
    }

    private PatientService.PatientData data(String name, String phone) {
        return new PatientService.PatientData(name, LocalDate.of(1990, 1, 1), "Female",
                null, null, phone, null);
    }

    private void resetDatabase() throws Exception {
        PersistenceConfig.shutdown();
        Files.deleteIfExists(AppPaths.h2DatabasePath().resolveSibling("patientdb.mv.db"));
        Files.deleteIfExists(AppPaths.h2DatabasePath().resolveSibling("patientdb.trace.db"));
        PersistenceConfig.initialize();
    }
}