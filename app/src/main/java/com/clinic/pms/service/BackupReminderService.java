package com.clinic.pms.service;

import com.clinic.pms.repository.BackupMetadataRepository;
import java.time.LocalDateTime;

/** Computes whether the logout/close backup reminder should be shown (FR-009a, FR-019a). */
public class BackupReminderService {

    private static final long REMINDER_THRESHOLD_DAYS = 5;

    private final BackupMetadataRepository backupMetadataRepository;

    public BackupReminderService(BackupMetadataRepository backupMetadataRepository) {
        this.backupMetadataRepository = backupMetadataRepository;
    }

    public LocalDateTime getLastBackupAt() {
        return backupMetadataRepository.find().map(m -> m.getLastBackupAt()).orElse(null);
    }

    public boolean isReminderDue() {
        LocalDateTime lastBackupAt = getLastBackupAt();
        if (lastBackupAt == null) {
            return true;
        }
        return lastBackupAt.isBefore(LocalDateTime.now().minusDays(REMINDER_THRESHOLD_DAYS));
    }
}
