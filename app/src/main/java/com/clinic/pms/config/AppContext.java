package com.clinic.pms.config;

import com.clinic.pms.repository.*;
import com.clinic.pms.service.*;
import jakarta.persistence.EntityManager;

/** Simple manually-wired service locator — no Spring/DI framework needed for this app's size. */
public class AppContext {

    public AuthService authService;
    public BackupReminderService backupReminderService;
    public PatientService patientService;
    public MedicalHistoryService medicalHistoryService;
    public ClinicProfileService clinicProfileService;
    public BackupService backupService;
    public H2NativeBackupService h2NativeBackupService;
    private EntityManager entityManager;

    public AppContext() {
        PersistenceConfig.initialize();
        reloadServices();
    }

    public void reloadServices() {
        this.entityManager = PersistenceConfig.newEntityManager();
        this.authService = new AuthService(new DoctorCredentialRepository(entityManager));
        BackupMetadataRepository backupMetadataRepository = new BackupMetadataRepository(entityManager);
        this.backupReminderService = new BackupReminderService(backupMetadataRepository);
        this.patientService = new PatientService(new PatientRepository(entityManager),
                new MedicalHistoryRepository(entityManager));
        this.medicalHistoryService = new MedicalHistoryService(new MedicalHistoryRepository(entityManager));
        this.clinicProfileService = new ClinicProfileService(new DoctorClinicProfileRepository(entityManager));
        this.backupService = new BackupService(entityManager, backupMetadataRepository);
        this.h2NativeBackupService = new H2NativeBackupService(entityManager, backupService);
    }
}
