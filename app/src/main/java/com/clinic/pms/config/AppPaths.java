package com.clinic.pms.config;

import java.io.File;
import java.nio.file.Path;

/** Resolves the app's per-user data directory (constitution: file-based H2 under the home dir). */
public final class AppPaths {

    private AppPaths() {}

    public static Path dataDir() {
        Path dir = Path.of(System.getProperty("user.home"), ".patient-management-system", "data");
        new File(dir.toUri()).mkdirs();
        return dir;
    }

    public static String h2JdbcUrl() {
        return "jdbc:h2:file:" + h2DatabasePath() + ";DB_CLOSE_ON_EXIT=FALSE";
    }

    public static Path h2DatabasePath() {
        return dataDir().resolve("patientdb");
    }
}
