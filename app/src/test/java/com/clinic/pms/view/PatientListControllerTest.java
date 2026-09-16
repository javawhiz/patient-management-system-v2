package com.clinic.pms.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.clinic.pms.config.AppContext;
import com.clinic.pms.config.AppPaths;
import com.clinic.pms.config.PersistenceConfig;
import com.clinic.pms.entity.Patient;
import com.clinic.pms.service.PatientService;
import java.nio.file.Files;
import java.time.LocalDate;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.testfx.framework.junit5.ApplicationTest;

/**
 * FR-003a: the landing list pre-fills the last-7-days range and searches
 * automatically. Modal patient-opening is covered by PatientProfileControllerTest
 * directly, since driving it here would require a live double-click into a
 * blocking nested modal window.
 */
class PatientListControllerTest extends ApplicationTest {

    private AppContext context;

    @Override
    public void start(Stage stage) throws Exception {
        resetDatabase();
        context = new AppContext();

        var recent = context.patientService.create(new PatientService.PatientData(
                "Recent Patient", LocalDate.of(1990, 1, 1), "Male", null, null, "5551111111", null));
        context.medicalHistoryService.add(recent.getId(), LocalDate.now().minusDays(2), "Recent visit", null, null);

        var old = context.patientService.create(new PatientService.PatientData(
                "Old Patient", LocalDate.of(1990, 1, 1), "Female", null, null, "5552222222", null));
        context.medicalHistoryService.add(old.getId(), LocalDate.now().minusDays(20), "Old visit", null, null);

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/PatientList.fxml"));
        Parent root = loader.load();
        PatientListController controller = loader.getController();
        controller.setContext(context);
        controller.setNavigator(new SceneNavigator(stage, context));
        stage.setScene(new Scene(root));
        stage.show();
    }

    @Test
    void defaultsToLastSevenDaysAndSearchesAutomatically() {
        DatePicker from = lookup("#fromDatePicker").queryAs(DatePicker.class);
        DatePicker to = lookup("#toDatePicker").queryAs(DatePicker.class);
        assertEquals(LocalDate.now().minusDays(6), from.getValue());
        assertEquals(LocalDate.now(), to.getValue());

        @SuppressWarnings("unchecked")
        TableView<Patient> table = (TableView<Patient>) lookup("#patientTable").queryAs(TableView.class);
        assertEquals(1, table.getItems().size());
        assertEquals("Recent Patient", table.getItems().get(0).getFullName());

        Label noResults = lookup("#noResultsLabel").queryAs(Label.class);
        assertFalse(noResults.isVisible());
    }

    @Test
    void showsNoResultsStateWhenNothingMatches() {
        interact(() -> ((TextField) lookup("#queryField").query()).setText("Nobody Matches This Name"));
        interact(() -> ((Button) lookup("Search").query()).fire());

        Label noResults = lookup("#noResultsLabel").queryAs(Label.class);
        assertTrue(noResults.isVisible());
    }

    private void resetDatabase() throws Exception {
        PersistenceConfig.shutdown();
        Files.deleteIfExists(AppPaths.h2DatabasePath().resolveSibling("patientdb.mv.db"));
        Files.deleteIfExists(AppPaths.h2DatabasePath().resolveSibling("patientdb.trace.db"));
    }
}
