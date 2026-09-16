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
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.testfx.framework.junit5.ApplicationTest;

/**
 * FR-014/FR-081: Current Treatment Plan, the dependent Include Medical
 * History checkbox, and the Include Notes checkbox. Only refreshPreview()
 * (checkbox toggling) is exercised here; the native print dialog itself is
 * not testable headlessly.
 */
class PrintPreviewControllerTest extends ApplicationTest {

    private long patientId;

    @Override
    public void start(Stage stage) throws Exception {
        resetDatabase();
        AppContext context = new AppContext();
        context.clinicProfileService.saveInitialProfile("Dr Test", "Test Clinic", "123 Main St", "REG1");
        var patient = context.patientService.create(new PatientService.PatientData(
                "Print Patient", LocalDate.of(1985, 1, 1), "Male", null, null, "5551234567", null));
        patientId = patient.getId();
        context.medicalHistoryService.add(patientId, LocalDate.of(2026, 1, 1), "Checkup", "Take rest", "Feeling better");
        context.medicalHistoryService.add(patientId, LocalDate.of(2025, 6, 1), "Old checkup", "Old rest", "Old note");

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/PrintPreview.fxml"));
        Parent root = loader.load();
        PrintPreviewController controller = loader.getController();
        controller.setContext(context);
        controller.init(new SceneNavigator(stage, context), patientId);
        stage.setScene(new Scene(root));
        stage.show();
    }

    @Test
    void historyCheckBoxIsDependentOnCurrentTreatmentCheckBox() {
        CheckBox currentTreatment = lookup("#currentTreatmentCheckBox").queryAs(CheckBox.class);
        CheckBox history = lookup("#historyCheckBox").queryAs(CheckBox.class);
        assertTrue(history.isDisabled(), "Include Medical History must start disabled");

        interact(currentTreatment::fire);
        assertFalse(history.isDisabled(), "Include Medical History must enable once Current Treatment Plan is checked");
    }

    @Test
    void currentTreatmentPrintsOnlyLatestVisitAndHistoryPrintsThePriorOnes() {
        CheckBox currentTreatment = lookup("#currentTreatmentCheckBox").queryAs(CheckBox.class);
        CheckBox history = lookup("#historyCheckBox").queryAs(CheckBox.class);

        interact(currentTreatment::fire);
        assertTrue(anyLabelContains("Checkup"));
        assertFalse(anyLabelContains("Old checkup"), "Prior visits must not print unless Include Medical History is checked");

        interact(history::fire);
        assertTrue(anyLabelContains("Old checkup"), "Prior visits must print once Include Medical History is checked");
    }

    @Test
    void notesPrintOnlyWhenNotesCheckBoxSelected() {
        CheckBox currentTreatment = lookup("#currentTreatmentCheckBox").queryAs(CheckBox.class);
        CheckBox notes = lookup("#notesCheckBox").queryAs(CheckBox.class);

        interact(currentTreatment::fire);
        assertFalse(anyLabelContains("Feeling better"), "Notes must not print unless Include Notes is checked");

        interact(notes::fire);
        assertTrue(anyLabelContains("Feeling better"), "Notes must print once Include Notes is checked");
    }

    private boolean anyLabelContains(String needle) {
        return lookup(".label").queryAllAs(Label.class).stream()
                .anyMatch(label -> label.getText() != null && label.getText().contains(needle));
    }

    private void resetDatabase() throws Exception {
        PersistenceConfig.shutdown();
        Files.deleteIfExists(AppPaths.h2DatabasePath().resolveSibling("patientdb.mv.db"));
        Files.deleteIfExists(AppPaths.h2DatabasePath().resolveSibling("patientdb.trace.db"));
    }
}
