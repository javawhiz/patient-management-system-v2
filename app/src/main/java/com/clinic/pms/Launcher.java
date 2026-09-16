package com.clinic.pms;

/**
 * Does not extend {@link javafx.application.Application}: the JDK launcher
 * refuses to start an Application subclass directly from a classpath-only
 * (non-modular) jar, which is how the shaded jar/jpackage image runs.
 */
public final class Launcher {

    public static void main(String[] args) {
        PatientManagementApp.main(args);
    }
}
