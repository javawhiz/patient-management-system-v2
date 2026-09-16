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

class MedicalHistoryServiceTest {

    private PatientService patientService;
    private MedicalHistoryService historyService;
    private long patientId;

    @BeforeEach
    void setUp() throws Exception {
        resetDatabase();
        EntityManager em = PersistenceConfig.newEntityManager();
        patientService = new PatientService(new PatientRepository(em), new MedicalHistoryRepository(em));
        historyService = new MedicalHistoryService(new MedicalHistoryRepository(em));
        patientId = patientService.create(new PatientService.PatientData("History Patient",
                LocalDate.of(1988, 5, 1), "Male", null, null, "5551234567", null)).getId();
    }

    @AfterEach
    void tearDown() {
        PersistenceConfig.shutdown();
    }

    @Test
    void addsListsUpdatesSoftDeletesAndFindsCurrentPrescription() {
        var older = historyService.add(patientId, LocalDate.of(2026, 1, 1), "Older", "Old Rx", "Old note");
        var newer = historyService.add(patientId, LocalDate.of(2026, 2, 1), "Newer", "New Rx", "New note");

        assertEquals(newer.getId(), historyService.listForPatient(patientId).get(0).getId());
        assertEquals("New Rx", historyService.getCurrentPrescriptionText(patientId));

        historyService.update(patientId, older.getId(), LocalDate.of(2026, 3, 1), "Updated", "Updated Rx", "Updated note");
        assertEquals("Updated", historyService.getForEdit(patientId, older.getId()).getDiagnosis());
        assertEquals("Updated Rx", historyService.getCurrentPrescriptionText(patientId));

        historyService.softDelete(patientId, older.getId());
        assertEquals(1, historyService.listForPatient(patientId).size());
        assertEquals("New Rx", historyService.getCurrentPrescriptionText(patientId));
    }

    private void resetDatabase() throws Exception {
        PersistenceConfig.shutdown();
        Files.deleteIfExists(AppPaths.h2DatabasePath().resolveSibling("patientdb.mv.db"));
        Files.deleteIfExists(AppPaths.h2DatabasePath().resolveSibling("patientdb.trace.db"));
        PersistenceConfig.initialize();
    }
}