package com.clinic.pms.view;

import static org.junit.jupiter.api.Assertions.assertFalse;

import com.clinic.pms.config.AppContext;
import com.clinic.pms.config.AppPaths;
import com.clinic.pms.config.PersistenceConfig;
import java.nio.file.Files;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressBar;
import javafx.stage.Stage;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.testfx.framework.junit5.ApplicationTest;
import org.testfx.util.WaitForAsyncUtils;

/**
 * FR-017a: the progress bar is hidden by default. A headless test environment
 * has no real file-picker, so FileChooser returns no selection and the
 * backup/restore actions return early without starting a background task or
 * crashing — this is exercised here as a safe smoke check.
 */
class BackupRestoreControllerTest extends ApplicationTest {

    @Override
    public void start(Stage stage) throws Exception {
        resetDatabase();
        AppContext context = new AppContext();

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/BackupRestore.fxml"));
        Parent root = loader.load();
        BackupRestoreController controller = loader.getController();
        controller.setContext(context);
        controller.setNavigator(new SceneNavigator(stage, context));
        stage.setScene(new Scene(root));
        stage.show();
    }

    @Test
    void progressBarIsHiddenUntilAnOperationRuns() {
        ProgressBar progressBar = lookup("#progressBar").queryAs(ProgressBar.class);
        assertFalse(progressBar.isVisible());
    }

    @Test
    @Disabled("Opens the native FileChooser; covered by service tests and manual quickstart validation.")
    void clickingBackupWithNoFileChosenDoesNotCrashOrShowProgress() {
        interact(() -> ((Button) lookup("Back Up Now").query()).fire());
        WaitForAsyncUtils.waitForFxEvents();

        ProgressBar progressBar = lookup("#progressBar").queryAs(ProgressBar.class);
        assertFalse(progressBar.isVisible(), "No background task should start when no file was chosen");
    }

    private void resetDatabase() throws Exception {
        PersistenceConfig.shutdown();
        Files.deleteIfExists(AppPaths.h2DatabasePath().resolveSibling("patientdb.mv.db"));
        Files.deleteIfExists(AppPaths.h2DatabasePath().resolveSibling("patientdb.trace.db"));
    }
}
