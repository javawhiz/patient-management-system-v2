package com.clinic.pms.service;

import static org.junit.jupiter.api.Assertions.*;

import com.clinic.pms.config.AppPaths;
import com.clinic.pms.config.PersistenceConfig;
import com.clinic.pms.repository.BackupMetadataRepository;
import com.clinic.pms.repository.DoctorClinicProfileRepository;
import com.clinic.pms.repository.DoctorCredentialRepository;
import com.clinic.pms.repository.MedicalHistoryRepository;
import com.clinic.pms.repository.PatientRepository;
import jakarta.persistence.EntityManager;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BackupServiceTest {

    private PatientService patientService;
    private MedicalHistoryService historyService;
    private BackupService backupService;
    private long patientId;

    @BeforeEach
    void setUp() throws Exception {
        resetDatabase();
        EntityManager em = PersistenceConfig.newEntityManager();
        authAndClinic(em);
        patientService = new PatientService(new PatientRepository(em), new MedicalHistoryRepository(em));
        historyService = new MedicalHistoryService(new MedicalHistoryRepository(em));
        backupService = new BackupService(em, new BackupMetadataRepository(em));
        patientId = patientService.create(new PatientService.PatientData("Backup Patient",
                LocalDate.of(1980, 1, 1), "Undisclosed", null, null, "5551234567", null)).getId();
        var history = historyService.add(patientId, LocalDate.of(2026, 1, 1), "Diagnosis", "Prescription", "Notes");
        historyService.softDelete(patientId, history.getId());
    }

    @AfterEach
    void tearDown() {
        PersistenceConfig.shutdown();
    }

    @Test
    void exportsCurrentTablesIncludingSoftDeletedHistoryAndRestoresTransactionally() {
        String sql = backupService.exportSql();

        assertTrue(sql.contains("INSERT INTO doctor_credential"));
        assertTrue(sql.contains("INSERT INTO doctor_clinic_profile"));
        assertTrue(sql.contains("INSERT INTO backup_metadata"));
        assertTrue(sql.contains("INSERT INTO patient"));
        assertTrue(sql.contains("INSERT INTO medical_history_entry"));
        assertTrue(sql.contains("Prescription"));
        assertTrue(sql.contains("Notes"));
        assertFalse(sql.contains("INSERT INTO prescription"));
        assertFalse(sql.contains("INSERT INTO personal_note"));

        int restored = backupService.restoreFromSql(sql.getBytes(StandardCharsets.UTF_8));
        assertEquals(1, restored);
        assertEquals("Backup Patient", patientService.getById(patientId).getFullName());
        assertTrue(historyService.listForPatient(patientId).isEmpty());
        assertThrows(RuntimeException.class, () -> backupService.restoreFromSql("bad".getBytes(StandardCharsets.UTF_8)));
    }

    private void authAndClinic(EntityManager em) {
        new AuthService(new DoctorCredentialRepository(em)).createInitialCredential("doctor", "Secret123");
        new ClinicProfileService(new DoctorClinicProfileRepository(em))
                .saveInitialProfile("Dr Test", "Test Clinic", "123 Main", "REG123");
    }

    private void resetDatabase() throws Exception {
        PersistenceConfig.shutdown();
        Files.deleteIfExists(AppPaths.h2DatabasePath().resolveSibling("patientdb.mv.db"));
        Files.deleteIfExists(AppPaths.h2DatabasePath().resolveSibling("patientdb.trace.db"));
        PersistenceConfig.initialize();
    }
}