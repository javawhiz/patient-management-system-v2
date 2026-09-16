package com.clinic.pms.service;

import com.clinic.pms.exception.BadRequestException;
import com.clinic.pms.config.AppPaths;
import com.clinic.pms.config.PersistenceConfig;
import jakarta.persistence.EntityManager;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Locale;
import java.util.UUID;
import org.hibernate.Session;

/** H2-specific full schema-and-data backup using H2's SCRIPT command. */
public class H2NativeBackupService {

    static final String BACKUP_HEADER = "-- PMS_H2_BACKUP_V1";

    private final EntityManager em;
    private final BackupService backupService;

    public H2NativeBackupService(EntityManager em, BackupService backupService) {
        this.em = em;
        this.backupService = backupService;
    }

    /**
     * Exports H2's complete schema and data without the destructive DROP option.
     * The destination is replaced only after the generated script has been read
     * successfully and checked for DROP statements.
     */
    public void exportScript(Path destination) {
        Path temporaryScript = null;
        try {
            Path destinationParent = destination.toAbsolutePath().getParent();
            if (destinationParent == null) {
                throw new IOException("Backup destination has no parent directory");
            }
            Files.createDirectories(destinationParent);
            temporaryScript = Files.createTempFile(destinationParent, ".pms-h2-", ".sql");
            Path h2Script = temporaryScript.resolveSibling(temporaryScript.getFileName() + ".source");

            String escapedPath = h2Script.toAbsolutePath().toString().replace("'", "''");
            Session session = em.unwrap(Session.class);
            session.doWork(connection -> {
                try (Statement statement = connection.createStatement()) {
                    statement.execute("SCRIPT NOSETTINGS TO '" + escapedPath.replace('\\', '/') + "'");
                } catch (SQLException e) {
                    throw new IllegalStateException("H2 SCRIPT export failed", e);
                }
            });

            String script = sanitizeScript(Files.readString(h2Script, StandardCharsets.UTF_8));
            validateScript(BACKUP_HEADER + "\n" + script);
            Files.writeString(temporaryScript, BACKUP_HEADER + "\n" + script, StandardCharsets.UTF_8);
            moveReplacing(temporaryScript, destination);
            backupService.recordSuccessfulBackup();
            temporaryScript = null;
        } catch (IOException | RuntimeException e) {
            throw new BadRequestException("Failed to export H2 backup: " + e.getMessage());
        } finally {
            deleteIfPresent(temporaryScript);
            if (temporaryScript != null) {
                deleteIfPresent(temporaryScript.resolveSibling(temporaryScript.getFileName() + ".source"));
            }
        }
    }

    /** Restores into a fresh H2 database and swaps it in only after validation. */
    public void restoreScript(Path source) {
        Path safetyBackup = AppPaths.dataDir().resolve("PatientBackup_before_h2_restore_"
                + System.currentTimeMillis() + ".sql");
        Path workDir = AppPaths.dataDir().resolve(".pms-h2-restore-" + UUID.randomUUID());
        Path restoredBase = workDir.resolve("patientdb");
        Path liveBase = AppPaths.h2DatabasePath();
        Path oldDatabase = AppPaths.dataDir().resolve("patientdb.before-h2-restore");
        boolean liveMoved = false;
        boolean restoredMoved = false;

        try {
            String script = Files.readString(source, StandardCharsets.UTF_8);
            validateScript(script);
            Files.writeString(safetyBackup, backupService.exportSqlWithoutRecordingBackupTime(), StandardCharsets.UTF_8);
            Files.createDirectories(workDir);
            runScript(script, restoredBase);
            validateRestoredDatabase(restoredBase);

            PersistenceConfig.shutdown();
            deleteDatabaseArtifacts(oldDatabase);
            moveDatabaseFile(liveBase, oldDatabase);
            liveMoved = true;
            moveDatabaseFile(restoredBase, liveBase);
            restoredMoved = true;
            deleteDatabaseArtifacts(oldDatabase);
            deleteRecursively(workDir);
        } catch (IOException | SQLException | RuntimeException e) {
            if (liveMoved) {
                try {
                    if (restoredMoved) {
                        deleteDatabaseArtifacts(liveBase);
                    }
                    moveDatabaseFile(oldDatabase, liveBase);
                } catch (IOException rollbackFailure) {
                    e.addSuppressed(rollbackFailure);
                }
            }
            deleteRecursively(workDir);
            throw new BadRequestException("Failed to restore H2 backup: " + e.getMessage());
        }
    }

    private void runScript(String script, Path databaseBase) throws IOException {
        Path scriptFile = Files.createTempFile("pms-h2-validated-", ".sql");
        try {
            Files.writeString(scriptFile, script, StandardCharsets.UTF_8);
            String databaseUrl = "jdbc:h2:file:" + databaseBase + ";DB_CLOSE_ON_EXIT=FALSE";
            String escapedPath = scriptFile.toAbsolutePath().toString().replace("'", "''");
            try (Connection connection = DriverManager.getConnection(databaseUrl, "sa", "");
                    Statement statement = connection.createStatement()) {
                statement.execute("RUNSCRIPT FROM '" + escapedPath.replace('\\', '/') + "'");
            } catch (SQLException e) {
                throw new IOException("H2 RUNSCRIPT failed: " + e.getMessage(), e);
            }
        } finally {
            deleteIfPresent(scriptFile);
        }
    }

    private void validateRestoredDatabase(Path databaseBase) throws SQLException {
        String databaseUrl = "jdbc:h2:file:" + databaseBase + ";DB_CLOSE_ON_EXIT=FALSE";
        try (Connection connection = DriverManager.getConnection(databaseUrl, "sa", "");
                Statement statement = connection.createStatement()) {
            try (var result = statement.executeQuery("SELECT COUNT(*) FROM doctor_credential")) {
                if (!result.next()) {
                    throw new SQLException("Restored database could not be queried");
                }
            }
            try (var result = statement.executeQuery("SELECT COUNT(*) FROM patient")) {
                if (!result.next()) {
                    throw new SQLException("Restored database could not be queried");
                }
            }
        }
    }

    private void validateScript(String script) throws IOException {
        if (!script.startsWith(BACKUP_HEADER)) {
            throw new IOException("Not a PMS H2 backup");
        }
        String upperScript = script.toUpperCase(Locale.ROOT);
        String[] forbidden = {"DROP ", "DELETE ", "TRUNCATE ", "ALTER USER", "CREATE USER",
                "GRANT ", "REVOKE ", "RUNSCRIPT", "SCRIPT ", "CALL ", "EXECUTE "};
        for (String token : forbidden) {
            if (upperScript.contains(token)) {
                throw new IOException("Backup contains forbidden statement: " + token.trim());
            }
        }
    }

    private String sanitizeScript(String script) {
        StringBuilder sanitized = new StringBuilder();
        for (String line : script.split("\\R", -1)) {
            String upperLine = line.trim().toUpperCase(Locale.ROOT);
            if (upperLine.startsWith("CREATE USER ") || upperLine.startsWith("ALTER USER ")
                    || upperLine.startsWith("GRANT ") || upperLine.startsWith("REVOKE ")) {
                continue;
            }
            sanitized.append(line).append('\n');
        }
        return sanitized.toString();
    }

    private void moveReplacing(Path source, Path destination) throws IOException {
        try {
            Files.move(source, destination, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(source, destination, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private void deleteIfPresent(Path file) {
        if (file != null) {
            try {
                Files.deleteIfExists(file);
            } catch (IOException ignored) {
                // A failed cleanup must not hide the export error.
            }
        }
    }

    private void moveDatabaseFile(Path sourceBase, Path destinationBase) throws IOException {
        Path source = sourceBase.resolveSibling(sourceBase.getFileName() + ".mv.db");
        Path destination = destinationBase.resolveSibling(destinationBase.getFileName() + ".mv.db");
        if (!Files.exists(source)) {
            throw new IOException("Database file was not created: " + source.getFileName());
        }
        moveReplacing(source, destination);
        moveOptionalFile(sourceBase, destinationBase, ".trace.db");
    }

    private void moveOptionalFile(Path sourceBase, Path destinationBase, String suffix) throws IOException {
        Path source = sourceBase.resolveSibling(sourceBase.getFileName() + suffix);
        if (Files.exists(source)) {
            Path destination = destinationBase.resolveSibling(destinationBase.getFileName() + suffix);
            moveReplacing(source, destination);
        }
    }

    private void deleteDatabaseArtifacts(Path databaseBase) {
        deleteIfPresent(databaseBase.resolveSibling(databaseBase.getFileName() + ".mv.db"));
        deleteIfPresent(databaseBase.resolveSibling(databaseBase.getFileName() + ".trace.db"));
        deleteIfPresent(databaseBase.resolveSibling(databaseBase.getFileName() + ".lock.db"));
    }

    private void deleteRecursively(Path directory) {
        if (directory == null || !Files.exists(directory)) {
            return;
        }
        try (var paths = Files.walk(directory)) {
            paths.sorted(java.util.Comparator.reverseOrder()).forEach(this::deleteIfPresent);
        } catch (IOException ignored) {
            // Temporary restore files are best-effort cleanup only.
        }
    }
}
