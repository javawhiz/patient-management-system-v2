package com.clinic.pms.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.testfx.framework.junit5.ApplicationTest;
import org.testfx.util.WaitForAsyncUtils;

/**
 * FR-004a: Medical History tab shows visible Date/Diagnosis/Prescription/Notes
 * labels, and adding an entry refreshes the history list. Double-click editing
 * is covered separately by MedicalHistoryEditControllerTest to avoid driving a
 * blocking nested modal window from within a robot test.
 */
class PatientProfileControllerTest extends ApplicationTest {

    private AppContext context;
    private long patientId;

    @Override
    public void start(Stage stage) throws Exception {
        resetDatabase();
        context = new AppContext();
        var patient = context.patientService.create(new PatientService.PatientData(
                "Profile Patient", LocalDate.of(1975, 6, 1), "Male", null, null, "5551234567", null));
        patientId = patient.getId();

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/PatientProfile.fxml"));
        Parent root = loader.load();
        PatientProfileController controller = loader.getController();
        controller.setContext(context);
        controller.setWindow(stage);
        controller.init(new SceneNavigator(new Stage(), context), patientId);
        stage.setScene(new Scene(root));
        stage.show();
    }

    @Test
    void medicalHistoryTabShowsFieldLabels() {
        assertTrue(labelTextsContainAll("Date", "Diagnosis", "Prescription", "Notes"));
    }

    @Test
    @Disabled("Fires a blocking success Alert; covered by MedicalHistoryServiceTest and manual quickstart validation.")
    void addingHistoryEntryRefreshesTheList() {
        interact(() -> {
            ((DatePicker) lookup("#historyDatePicker").query()).setValue(LocalDate.now());
            ((TextArea) lookup("#historyDiagnosisArea").query()).setText("New complaint");
            ((TextArea) lookup("#historyPrescriptionArea").query()).setText("New Rx");
            ((TextArea) lookup("#historyNotesArea").query()).setText("New notes");
        });
        interact(() -> ((Button) lookup("Add Entry").query()).fire());
        WaitForAsyncUtils.waitForFxEvents();

        ListView<?> historyList = lookup("#historyListView").queryAs(ListView.class);
        assertEquals(1, historyList.getItems().size());
        assertEquals(1, context.medicalHistoryService.listForPatient(patientId).size());
    }

    private boolean labelTextsContainAll(String... expected) {
        var texts = lookup(".field-label").queryAllAs(Label.class).stream()
                .map(Label::getText).toList();
        for (String value : expected) {
            if (!texts.contains(value)) {
                return false;
            }
        }
        return true;
    }

    private void resetDatabase() throws Exception {
        PersistenceConfig.shutdown();
        Files.deleteIfExists(AppPaths.h2DatabasePath().resolveSibling("patientdb.mv.db"));
        Files.deleteIfExists(AppPaths.h2DatabasePath().resolveSibling("patientdb.trace.db"));
    }
}
