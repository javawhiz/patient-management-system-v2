package com.clinic.pms.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** Singleton row (id=1) tracking the last successful backup for logout/close reminders. */
@Entity
@Table(name = "backup_metadata")
public class BackupMetadata {

    @Id
    private Long id;

    @Column(name = "last_backup_at")
    private LocalDateTime lastBackupAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public LocalDateTime getLastBackupAt() { return lastBackupAt; }
    public void setLastBackupAt(LocalDateTime lastBackupAt) { this.lastBackupAt = lastBackupAt; }
}
