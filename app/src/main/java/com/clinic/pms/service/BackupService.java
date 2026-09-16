package com.clinic.pms.service;

import com.clinic.pms.exception.BadRequestException;
import com.clinic.pms.repository.BackupMetadataRepository;
import jakarta.persistence.EntityManager;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.hibernate.Session;

/**
 * Generates and restores plain, ANSI-compatible SQL dumps (constitution
 * Principle V) — never H2's own SCRIPT command, to avoid H2-specific
 * syntax (research.md #5).
 */
public class BackupService {

    /** Written at the top of every export; used to validate restore files (FR-012). */
    static final String BACKUP_HEADER = "-- PMS_BACKUP_V1";

    // FK-safe order for export/restore.
    private static final String[] TABLES_IN_ORDER = {
        "doctor_credential", "doctor_clinic_profile", "backup_metadata", "patient",
        "medical_history_entry",
    };

    private final EntityManager em;
    private final BackupMetadataRepository backupMetadataRepository;

    public BackupService(EntityManager em, BackupMetadataRepository backupMetadataRepository) {
        this.em = em;
        this.backupMetadataRepository = backupMetadataRepository;
    }

    /** Exports every table (including soft-deleted patients) as ANSI INSERT statements. */
    public String exportSql() {
        String sql = generateSql();
        recordSuccessfulBackup();
        return sql;
    }

    String exportSqlWithoutRecordingBackupTime() {
        return generateSql();
    }

    void recordSuccessfulBackup() {
        backupMetadataRepository.find().ifPresent(meta -> {
            meta.setLastBackupAt(LocalDateTime.now());
            backupMetadataRepository.save(meta);
        });
    }

    private String generateSql() {
        StringBuilder sb = new StringBuilder();
        sb.append(BACKUP_HEADER).append("\n");
        sb.append("-- Generated: ").append(LocalDateTime.now()).append("\n\n");

        Session session = em.unwrap(Session.class);
        session.doWork(connection -> {
            for (String table : TABLES_IN_ORDER) {
                appendTableInserts(sb, connection, table);
            }
        });

        return sb.toString();
    }

    private void appendTableInserts(StringBuilder sb, Connection connection, String table) {
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM " + table)) {
            ResultSetMetaData meta = rs.getMetaData();
            int columnCount = meta.getColumnCount();
            while (rs.next()) {
                StringBuilder columns = new StringBuilder();
                StringBuilder values = new StringBuilder();
                for (int i = 1; i <= columnCount; i++) {
                    if (i > 1) {
                        columns.append(", ");
                        values.append(", ");
                    }
                    columns.append(meta.getColumnName(i));
                    values.append(formatValue(rs, i));
                }
                sb.append("INSERT INTO ").append(table).append(" (").append(columns).append(") VALUES (")
                        .append(values).append(");\n");
            }
            sb.append("\n");
        } catch (SQLException e) {
            throw new BadRequestException("Failed to export table " + table + ": " + e.getMessage());
        }
    }

    private String formatValue(ResultSet rs, int index) throws SQLException {
        Object value = rs.getObject(index);
        if (value == null) {
            return "NULL";
        }
        if (value instanceof Number || value instanceof Boolean) {
            return value.toString();
        }
        String text = value.toString();
        if (value instanceof java.sql.Clob clob) {
            text = clob.getSubString(1, (int) clob.length());
        }
        return "'" + text.replace("'", "''") + "'";
    }

    /**
     * Restores from a previously exported file: validates the header, clears
     * existing domain tables, then replays the INSERT statements — all
     * inside a single transaction. Any failure rolls back the entire
     * operation, leaving prior data untouched (FR-011, FR-012).
     */
    public int restoreFromSql(byte[] fileContent) {
        List<String> statements = parseAndValidate(fileContent);

        em.getTransaction().begin();
        try {
            Session session = em.unwrap(Session.class);
            int[] patientCount = {0};
            session.doWork(connection -> {
                try (Statement stmt = connection.createStatement()) {
                    for (int i = TABLES_IN_ORDER.length - 1; i >= 0; i--) {
                        stmt.execute("DELETE FROM " + TABLES_IN_ORDER[i]);
                    }
                    for (String statement : statements) {
                        stmt.execute(statement);
                        if (statement.toUpperCase(Locale.ROOT).startsWith("INSERT INTO PATIENT ")) {
                            patientCount[0]++;
                        }
                    }
                }
            });
            em.getTransaction().commit();
            return patientCount[0];
        } catch (RuntimeException e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw new BadRequestException("Invalid or corrupted backup file");
        }
    }

    private List<String> parseAndValidate(byte[] fileContent) {
        List<String> statements = new ArrayList<>();
        boolean headerFound = false;
        StringBuilder current = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new ByteArrayInputStream(fileContent), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.startsWith(BACKUP_HEADER)) {
                    headerFound = true;
                    continue;
                }
                if (trimmed.isEmpty() || trimmed.startsWith("--")) {
                    continue;
                }
                current.append(line).append("\n");
                if (trimmed.endsWith(";")) {
                    String statement = current.toString().trim();
                    // Only INSERT statements are ever accepted from a restore file — anything
                    // else (DROP/DELETE/arbitrary SQL) is rejected as not a valid backup export.
                    if (!statement.toUpperCase(Locale.ROOT).startsWith("INSERT INTO")) {
                        throw new BadRequestException("Invalid or corrupted backup file");
                    }
                    statements.add(statement.substring(0, statement.length() - 1));
                    current.setLength(0);
                }
            }
        } catch (IOException e) {
            throw new BadRequestException("Invalid or corrupted backup file");
        }

        if (!headerFound || statements.isEmpty()) {
            throw new BadRequestException("Invalid or corrupted backup file");
        }
        return statements;
    }
}
