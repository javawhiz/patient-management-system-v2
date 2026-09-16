package com.clinic.pms.view;

import com.clinic.pms.config.AppContext;
import com.clinic.pms.entity.Patient;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

/** Search/browse patients (FR-003) and add new ones (FR-001). Never shows soft-deleted patients. */
public class PatientListController implements AppAware {

    @FXML private TextField queryField;
    @FXML private TextField phoneField;
    @FXML private DatePicker fromDatePicker;
    @FXML private DatePicker toDatePicker;
    @FXML private Label noResultsLabel;
    @FXML private TableView<Patient> patientTable;
    @FXML private TableColumn<Patient, String> nameColumn;
    @FXML private TableColumn<Patient, String> phoneColumn;
    @FXML private TableColumn<Patient, LocalDate> dobColumn;
    @FXML private TableColumn<Patient, LocalDate> lastVisitColumn;

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy");

    private AppContext context;
    private SceneNavigator navigator;

    @Override
    public void setContext(AppContext context) {
        this.context = context;
    }

    public void setNavigator(SceneNavigator navigator) {
        this.navigator = navigator;
        setupTable();
        // Landing default: last 7 days (today and previous 6 days), searched automatically (FR-003a).
        fromDatePicker.setValue(LocalDate.now().minusDays(6));
        toDatePicker.setValue(LocalDate.now());
        handleSearch();
    }

    private void setupTable() {
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        phoneColumn.setCellValueFactory(new PropertyValueFactory<>("phoneNumber"));
        dobColumn.setCellValueFactory(new PropertyValueFactory<>("dateOfBirth"));
        dobColumn.setCellFactory(column -> dateCell());
        lastVisitColumn.setCellValueFactory(new PropertyValueFactory<>("lastAppointmentDate"));
        lastVisitColumn.setCellFactory(column -> dateCell());
        patientTable.setRowFactory(tv -> {
            TableRow<Patient> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    openProfile(row.getItem());
                }
            });
            return row;
        });
    }

    private TableCell<Patient, LocalDate> dateCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                setText(empty || date == null ? "" : DATE_FORMAT.format(date));
            }
        };
    }

    @FXML
    private void handleSearch() {
        List<Patient> results = context.patientService.search(
                blankToNull(queryField.getText()), blankToNull(phoneField.getText()),
                fromDatePicker.getValue(), toDatePicker.getValue());
        patientTable.setItems(FXCollections.observableArrayList(results));
        noResultsLabel.setVisible(results.isEmpty());
        noResultsLabel.setManaged(results.isEmpty());
    }

    @FXML
    private void handleAddPatient() {
        try {
            PatientFormDialog.show(navigator.stage(), "Add Patient", null).ifPresent(data -> {
                Patient created = context.patientService.create(data);
                AlertHelper.showSuccess(navigator.stage(), "Patient added");
                handleSearch();
                openProfile(created);
            });
        } catch (RuntimeException e) {
            AlertHelper.showError(navigator.stage(), "Could not add patient: " + e.getMessage());
        }
    }

    private void openProfile(Patient patient) {
        navigator.showModal(navigator.stage(), "/fxml/PatientProfile.fxml", "Patient Profile - " + patient.getFullName(),
                (PatientProfileController controller) -> controller.init(navigator, patient.getId()));
        // The modal has closed (soft-deleted, edited, or otherwise) — refresh the landing list.
        handleSearch();
    }

    @FXML
    private void handleOpenDeleted() {
        navigator.show("/fxml/DeletedPatients.fxml", "Deleted Patients",
                (DeletedPatientsController controller) -> controller.setNavigator(navigator));
    }

    @FXML
    private void handleOpenBackupRestore() {
        navigator.show("/fxml/BackupRestore.fxml", "Backup & Restore",
                (BackupRestoreController controller) -> controller.setNavigator(navigator));
    }

    @FXML
    private void handleLogout() {
        LogoutHelper.logout(navigator.stage(), context, navigator);
    }

    private String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }
}
