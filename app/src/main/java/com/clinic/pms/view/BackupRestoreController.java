package com.clinic.pms.view;

import com.clinic.pms.config.AppContext;
import com.clinic.pms.config.PersistenceConfig;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.ProgressBar;
import javafx.stage.FileChooser;

/**
 * Export/restore via native FileChooser dialogs (constitution Principle VI)
 * — no browser API, no fallback logic needed.
 */
public class BackupRestoreController implements AppAware {

    private AppContext context;
    private SceneNavigator navigator;

    @FXML
    private ProgressBar progressBar;

    @Override
    public void setContext(AppContext context) {
        this.context = context;
    }

    public void setNavigator(SceneNavigator navigator) {
        this.navigator = navigator;
    }

    @FXML
    private void handleBackup() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save Backup");
        String defaultName = "PatientBackup_" + DateTimeFormatter.ISO_LOCAL_DATE.format(LocalDate.now()) + ".sql";
        chooser.setInitialFileName(defaultName);
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("SQL backup", "*.sql"));

        var file = chooser.showSaveDialog(navigator.stage());
        if (file == null) {
            return;
        }
        runInBackground("Backup saved to " + file.getName(), "Backup failed", () -> {
            try {
                String sql = context.backupService.exportSql();
                Files.writeString(file.toPath(), sql);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @FXML
    private void handleH2Backup() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save H2 Native Backup");
        chooser.setInitialFileName("PatientBackup_H2_" + DateTimeFormatter.ISO_LOCAL_DATE.format(LocalDate.now()) + ".h2.sql");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("H2 native backup", "*.h2.sql"));

        var file = chooser.showSaveDialog(navigator.stage());
        if (file == null) {
            return;
        }
        runInBackground("H2 native backup saved to " + file.getName(), "H2 backup failed",
                () -> context.h2NativeBackupService.exportScript(file.toPath()));
    }

    @FXML
    private void handleRestore() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select Backup File");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("SQL backup", "*.sql"));

        var file = chooser.showOpenDialog(navigator.stage());
        if (file == null) {
            return;
        }

        boolean confirmed = AlertHelper.confirm(navigator.stage(),
                "This will replace ALL current patient data with the contents of \""
                        + file.getName() + "\". This cannot be undone. Continue?");
        if (!confirmed) {
            return;
        }

        runInBackgroundWithResult("Restore complete", "Restore failed", () -> {
            try {
                byte[] content = Files.readAllBytes(file.toPath());
                int restored = context.backupService.restoreFromSql(content);
                return restored + " patient(s) restored";
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @FXML
    private void handleH2Restore() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select H2 Native Backup");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("H2 native backup", "*.h2.sql"));

        var file = chooser.showOpenDialog(navigator.stage());
        if (file == null) {
            return;
        }

        boolean confirmed = AlertHelper.confirm(navigator.stage(),
                "This will replace the current database with the H2 backup. "
                        + "An ANSI safety backup will be created automatically first. Continue?");
        if (!confirmed) {
            return;
        }

        runInBackground("H2 restore complete. The current data was replaced.", "H2 restore failed", () -> {
            context.h2NativeBackupService.restoreScript(file.toPath());
            PersistenceConfig.initialize();
            context.reloadServices();
        });
    }

    private void runInBackground(String successMessage, String failureTitle, Runnable operation) {
        runInBackgroundWithResult(successMessage, failureTitle, () -> {
            operation.run();
            return null;
        });
    }

    private void runInBackgroundWithResult(String successMessage, String failureTitle,
            java.util.concurrent.Callable<String> operation) {
        Task<String> task = new Task<>() {
            @Override
            protected String call() throws Exception {
                return operation.call();
            }
        };
        task.setOnSucceeded(event -> {
            setBusy(false, task);
            String detail = task.getValue();
            AlertHelper.showSuccess(navigator.stage(), detail == null ? successMessage : successMessage + " - " + detail);
        });
        task.setOnFailed(event -> {
            setBusy(false, task);
            Throwable failure = task.getException();
            String message = failure == null ? "Unknown error" : failure.getMessage();
            AlertHelper.showError(navigator.stage(), failureTitle + ": " + message);
        });
        setBusy(true, task);
        Thread worker = new Thread(task, "pms-backup-operation");
        worker.setDaemon(true);
        worker.start();
    }

    private void setBusy(boolean busy, Task<?> task) {
        if (busy) {
            progressBar.progressProperty().bind(task.progressProperty());
            progressBar.setManaged(true);
            progressBar.setVisible(true);
        } else {
            progressBar.progressProperty().unbind();
            progressBar.setProgress(0);
            progressBar.setManaged(false);
            progressBar.setVisible(false);
        }
    }

    @FXML
    private void handleBack() {
        navigator.show("/fxml/PatientList.fxml", "Patient Management System - Patients",
                (PatientListController controller) -> controller.setNavigator(navigator));
    }
}
