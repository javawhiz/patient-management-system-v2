package com.clinic.pms.service;

import com.clinic.pms.repository.MedicalHistoryRepository;

import static org.junit.jupiter.api.Assertions.*;

import com.clinic.pms.config.AppPaths;
import com.clinic.pms.config.PersistenceConfig;
import com.clinic.pms.repository.*;
import jakarta.persistence.EntityManager;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

/**
 * End-to-end smoke test exercising the full service layer without any UI,
 * validating the same flows covered manually against the earlier Spring
 * Boot prototype: setup, login, patient CRUD, soft-delete/restore, and a
 * full backup export -> restore round trip.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ServiceSmokeTest {

    private static AuthService authService;
    private static PatientService patientService;
    private static MedicalHistoryService historyService;
    private static ClinicProfileService clinicProfileService;
    private static BackupService backupService;
    private static long patientId;
    private static long historyEntryId;

    @BeforeAll
    static void setUp() throws IOException {
        Files.deleteIfExists(AppPaths.dataDir().resolve("patientdb.mv.db"));
        Files.deleteIfExists(AppPaths.dataDir().resolve("patientdb.trace.db"));
        PersistenceConfig.initialize();
        EntityManager em = PersistenceConfig.newEntityManager();

        authService = new AuthService(new DoctorCredentialRepository(em));
        BackupMetadataRepository backupMetadataRepository = new BackupMetadataRepository(em);
        patientService = new PatientService(new PatientRepository(em), new MedicalHistoryRepository(em));
        historyService = new MedicalHistoryService(new MedicalHistoryRepository(em));
        clinicProfileService = new ClinicProfileService(new DoctorClinicProfileRepository(em));
        backupService = new BackupService(em, backupMetadataRepository);
    }

    @AfterAll
    static void tearDown() {
        PersistenceConfig.shutdown();
    }

    @Test
    @Order(1)
    void firstRunSetupAndLogin() {
        assertFalse(authService.isInitialized());
        authService.createInitialCredential("testdoc", "TestPass123!");
        clinicProfileService.saveInitialProfile("Dr Test", "Test Clinic", "123 Main St", "REG123");

        assertTrue(authService.login("testdoc", "TestPass123!"));
        assertFalse(authService.login("testdoc", "wrong"));
        assertFalse(authService.login("wronguser", "TestPass123!"));
    }

    @Test
    @Order(2)
    void resetPasswordRequiresCorrectRecoveryAnswer() {
        assertFalse(authService.resetPassword("testdoc", "wrong-answer", "NewPass123!"));
        assertTrue(authService.login("testdoc", "TestPass123!"));

        assertTrue(authService.resetPassword(
                "testdoc", "garima.j89@gmail.com", "NewPass123!"));
        assertFalse(authService.login("testdoc", "TestPass123!"));
        assertTrue(authService.login("testdoc", "NewPass123!"));
    }

    @Test
    @Order(3)
    void createSearchUpdatePatient() {
        var data = new PatientService.PatientData(
            "John Doe", LocalDate.of(1990, 1, 1), "Male", null, null, "5551234567", null);
        var patient = patientService.create(data);
        patientId = patient.getId();

        List<?> results = patientService.search("John", null, null, null);
        assertEquals(1, results.size());

        var updated = new PatientService.PatientData(
            "John Doe", LocalDate.of(1990, 1, 1), "Male", null, null, "5559999999", null);
        patientService.update(patientId, updated);
        assertEquals("5559999999", patientService.getById(patientId).getPhoneNumber());
    }

    @Test
    @Order(4)
    void historyPrescriptionAndNotes() {
        var entry = historyService.add(patientId, LocalDate.of(2026, 1, 1),
                "Checkup", "Med A", "All normal");
        historyEntryId = entry.getId();
        assertEquals(1, historyService.listForPatient(patientId).size());
        assertEquals("Med A", historyService.getCurrentPrescriptionText(patientId));

        historyService.update(patientId, historyEntryId, LocalDate.of(2026, 1, 2),
                "Follow-up", "Med B", "Improved");
        var updated = historyService.getForEdit(patientId, historyEntryId);
        assertEquals("Follow-up", updated.getDiagnosis());
        assertEquals("Med B", updated.getPrescription());
        assertEquals("Improved", updated.getNotes());
        assertEquals("Med B", historyService.getCurrentPrescriptionText(patientId));
    }

    @Test
    @Order(5)
    void softDeleteAndRestore() {
        patientService.softDelete(patientId);
        assertTrue(patientService.search("John", null, null, null).isEmpty());
        assertEquals(1, patientService.listDeleted().size());

        patientService.restore(patientId);
        assertFalse(patientService.getById(patientId).isDeleted());
        assertEquals(1, patientService.search("John", null, null, null).size());

        historyService.softDelete(patientId, historyEntryId);
        assertTrue(historyService.listForPatient(patientId).isEmpty());
        assertEquals("", historyService.getCurrentPrescriptionText(patientId));
    }

    @Test
    @Order(6)
    void backupExportAndRestoreRoundTrip() {
        String sql = backupService.exportSql();
        assertTrue(sql.contains("INSERT INTO doctor_credential"));
        assertTrue(sql.contains("INSERT INTO doctor_clinic_profile"));
        assertTrue(sql.contains("INSERT INTO backup_metadata"));
        assertTrue(sql.contains("INSERT INTO patient"));
        assertTrue(sql.contains("INSERT INTO medical_history_entry"));
        assertFalse(sql.contains("INSERT INTO prescription"));
        assertFalse(sql.contains("INSERT INTO personal_note"));

        int restored = backupService.restoreFromSql(sql.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        assertEquals(1, restored);
        assertEquals("John Doe", patientService.getById(patientId).getFullName());
        assertTrue(historyService.listForPatient(patientId).isEmpty());
    }
}
