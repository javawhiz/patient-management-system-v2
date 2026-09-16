package com.clinic.pms.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.clinic.pms.config.AppContext;
import com.clinic.pms.config.AppPaths;
import com.clinic.pms.config.PersistenceConfig;
import com.clinic.pms.service.PatientService;
import java.nio.file.Files;
import java.time.LocalDate;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.testfx.framework.junit5.ApplicationTest;
import org.testfx.util.WaitForAsyncUtils;

/**
 * FR-006a: edit popup is populated, saves edits, and discards on cancel. The
 * controller is shown non-modally here (stage.show(), not showAndWait()) so
 * the robot can interact with it directly without a blocking nested window.
 */
class MedicalHistoryEditControllerTest extends ApplicationTest {

    private AppContext context;
    private long patientId;
    private long entryId;
    private Stage editStage;
    private MedicalHistoryEditController controller;

    @Override
    public void start(Stage stage) throws Exception {
        resetDatabase();
        context = new AppContext();
        var patient = context.patientService.create(new PatientService.PatientData(
                "History Patient", LocalDate.of(1980, 1, 1), "Female", null, null, "5551234567", null));
        patientId = patient.getId();
        var entry = context.medicalHistoryService.add(patientId, LocalDate.of(2026, 1, 1),
                "Original diagnosis", "Original Rx", "Original notes");
        entryId = entry.getId();

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/MedicalHistoryEdit.fxml"));
        Parent root = loader.load();
        controller = loader.getController();
        controller.setContext(context);
        editStage = stage;
        controller.setWindow(editStage);
        controller.init(new SceneNavigator(new Stage(), context), patientId, entryId);
        editStage.setScene(new Scene(root));
        editStage.show();
    }

    @Test
    void populatesSelectedEntryFields() {
        assertEquals("Original diagnosis", lookup("#diagnosisArea").queryTextInputControl().getText());
        assertEquals("Original Rx", lookup("#prescriptionArea").queryTextInputControl().getText());
        assertEquals("Original notes", lookup("#notesArea").queryTextInputControl().getText());
    }

    @Test
    void saveUpdatesTheEntryAndClosesTheWindow() {
        interact(() -> ((TextArea) lookup("#diagnosisArea").query()).setText("Updated diagnosis"));
        interact(() -> ((Button) lookup("Save").query()).fire());
        WaitForAsyncUtils.waitForFxEvents();

        assertTrue(controller.isChanged());
        assertFalse(editStage.isShowing());
        assertEquals("Updated diagnosis",
                context.medicalHistoryService.getForEdit(patientId, entryId).getDiagnosis());
    }

    @Test
    void cancelDiscardsEditsAndClosesWithoutSaving() {
        interact(() -> ((TextArea) lookup("#diagnosisArea").query()).setText("Should not be saved"));
        interact(() -> ((Button) lookup("Cancel").query()).fire());
        WaitForAsyncUtils.waitForFxEvents();

        assertFalse(controller.isChanged());
        assertFalse(editStage.isShowing());
        assertEquals("Original diagnosis",
                context.medicalHistoryService.getForEdit(patientId, entryId).getDiagnosis());
    }

    private void resetDatabase() throws Exception {
        PersistenceConfig.shutdown();
        Files.deleteIfExists(AppPaths.h2DatabasePath().resolveSibling("patientdb.mv.db"));
        Files.deleteIfExists(AppPaths.h2DatabasePath().resolveSibling("patientdb.trace.db"));
    }
}
