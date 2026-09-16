package com.clinic.pms.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.clinic.pms.config.AppPaths;
import com.clinic.pms.config.PersistenceConfig;
import com.clinic.pms.entity.MedicalHistoryEntry;
import com.clinic.pms.service.MedicalHistoryService;
import com.clinic.pms.service.PatientService;
import jakarta.persistence.EntityManager;
import java.nio.file.Files;
import java.time.LocalDate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MedicalHistoryRepositoryTest {

    private MedicalHistoryRepository repository;
    private MedicalHistoryService historyService;
    private long patientId;

    @BeforeEach
    void setUp() throws Exception {
        resetDatabase();
        EntityManager em = PersistenceConfig.newEntityManager();
        repository = new MedicalHistoryRepository(em);
        historyService = new MedicalHistoryService(repository);
        PatientService patientService = new PatientService(new PatientRepository(em), new MedicalHistoryRepository(em));
        patientId = patientService.create(new PatientService.PatientData("Repository Patient",
                LocalDate.of(1980, 1, 1), "Undisclosed", null, null, "5551234567", null)).getId();
    }

    @AfterEach
    void tearDown() {
        PersistenceConfig.shutdown();
    }

    @Test
    void listsActiveEntriesInDescendingDateOrder() {
        MedicalHistoryEntry older = historyService.add(patientId, LocalDate.of(2026, 1, 1),
                "Older", "Old Rx", "Old note");
        MedicalHistoryEntry newer = historyService.add(patientId, LocalDate.of(2026, 2, 1),
                "Newer", "New Rx", "New note");
        historyService.softDelete(patientId, older.getId());

        var entries = repository.findByPatientId(patientId);

        assertEquals(1, entries.size());
        assertEquals(newer.getId(), entries.get(0).getId());
    }

    private void resetDatabase() throws Exception {
        PersistenceConfig.shutdown();
        Files.deleteIfExists(AppPaths.h2DatabasePath().resolveSibling("patientdb.mv.db"));
        Files.deleteIfExists(AppPaths.h2DatabasePath().resolveSibling("patientdb.trace.db"));
        PersistenceConfig.initialize();
    }
}