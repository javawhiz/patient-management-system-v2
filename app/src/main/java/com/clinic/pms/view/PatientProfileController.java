package com.clinic.pms.view;

import com.clinic.pms.config.AppContext;
import com.clinic.pms.entity.MedicalHistoryEntry;
import com.clinic.pms.entity.Patient;
import com.clinic.pms.service.PatientService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

/**
 * Patient profile: view/edit personal info (FR-002), soft-delete (FR-020),
 * medical history entries with prescription and notes captured together.
 * Opened as an owned modal window above the landing page (FR-023); print
 * preview and medical-history edit open as further modals above this window
 * (FR-024, FR-025).
 */
public class PatientProfileController implements AppAware, WindowAware {

    @FXML private Label titleLabel;
    @FXML private TextField fullNameField;
    @FXML private DatePicker dobPicker;
    @FXML private ChoiceBox<String> genderField;
    @FXML private TextField phoneField;
    @FXML private TextField emailField;
    @FXML private TextArea addressField;
    @FXML private TextField emergencyContactField;

    @FXML private DatePicker historyDatePicker;
    @FXML private TextArea historyDiagnosisArea;
    @FXML private TextArea historyPrescriptionArea;
    @FXML private TextArea historyNotesArea;
    @FXML private ListView<MedicalHistoryEntry> historyListView;

    private AppContext context;
    private SceneNavigator navigator;
    private Stage ownStage;
    private long patientId;

    @Override
    public void setWindow(Stage stage) {
        this.ownStage = stage;
    }

    private Stage windowOwner() {
        return ownStage != null ? ownStage : navigator.stage();
    }

    @FXML
    private void initialize() {
        genderField.setItems(FXCollections.observableArrayList("Male", "Female", "Undisclosed"));
        historyListView.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(MedicalHistoryEntry entry, boolean empty) {
                super.updateItem(entry, empty);
                setText(empty || entry == null ? null : formatHistoryEntry(entry));
            }
        });
        historyListView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                MedicalHistoryEntry selected = historyListView.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    openHistoryEdit(selected);
                }
            }
        });
    }

    @Override
    public void setContext(AppContext context) {
        this.context = context;
    }

    /** Called by the navigator's configureController callback after setContext(). */
    public void init(SceneNavigator navigator, long patientId) {
        this.navigator = navigator;
        this.patientId = patientId;
        loadAll();
    }

    private void loadAll() {
        Patient patient = context.patientService.getById(patientId);
        titleLabel.setText(patient.getFullName());
        fullNameField.setText(patient.getFullName());
        dobPicker.setValue(patient.getDateOfBirth());
        if (patient.getGender() != null && !genderField.getItems().contains(patient.getGender())) {
            genderField.getItems().add(patient.getGender());
        }
        genderField.setValue(patient.getGender());
        phoneField.setText(patient.getPhoneNumber());
        emailField.setText(patient.getEmail());
        addressField.setText(patient.getAddress());
        emergencyContactField.setText(patient.getEmergencyContact());

        reloadHistory();
    }

    private void reloadHistory() {
        var entries = context.medicalHistoryService.listForPatient(patientId);
        historyListView.setItems(FXCollections.observableArrayList(entries));
    }

    private String formatHistoryEntry(MedicalHistoryEntry e) {
        StringBuilder text = new StringBuilder("Date: ").append(e.getEntryDate())
                .append("\nDiagnosis:\n").append(e.getDiagnosis());
        if (e.getPrescription() != null && !e.getPrescription().isBlank()) {
            text.append("\nPrescription:\n").append(e.getPrescription());
        }
        if (e.getNotes() != null && !e.getNotes().isBlank()) {
            text.append("\nNotes:\n").append(e.getNotes());
        }
        return text.toString();
    }

    @FXML
    private void handleSave() {
        try {
            var data = new PatientService.PatientData(
                    fullNameField.getText(), dobPicker.getValue(), genderField.getValue(),
                    blankToNull(addressField.getText()), blankToNull(emergencyContactField.getText()),
                    phoneField.getText(), blankToNull(emailField.getText()));
            context.patientService.update(patientId, data);
            titleLabel.setText(fullNameField.getText());
            AlertHelper.showSuccess(windowOwner(), "Patient details updated");
        } catch (RuntimeException e) {
            AlertHelper.showError(windowOwner(), "Could not update patient: " + e.getMessage());
        }
    }

    @FXML
    private void handleDelete() {
        boolean confirmed = AlertHelper.confirm(windowOwner(),
                "Delete " + titleLabel.getText() + "? This hides the patient from lists and search, "
                        + "but the record is never permanently erased and can be restored later.");
        if (!confirmed) {
            return;
        }
        context.patientService.softDelete(patientId);
        AlertHelper.showSuccess(windowOwner(), "Patient deleted");
        handleBack();
    }

    @FXML
    private void handleAddHistory() {
        if (historyDatePicker.getValue() == null || historyDiagnosisArea.getText().isBlank()) {
            AlertHelper.showError(windowOwner(), "Date and diagnosis are required");
            return;
        }
        context.medicalHistoryService.add(patientId, historyDatePicker.getValue(),
                historyDiagnosisArea.getText(), historyPrescriptionArea.getText(), historyNotesArea.getText());
        historyDatePicker.setValue(null);
        historyDiagnosisArea.clear();
        historyPrescriptionArea.clear();
        historyNotesArea.clear();
        reloadHistory();
        AlertHelper.showSuccess(windowOwner(), "History entry added");
    }

    private void openHistoryEdit(MedicalHistoryEntry entry) {
        MedicalHistoryEditController controller = navigator.showModal(windowOwner(),
                "/fxml/MedicalHistoryEdit.fxml", "Edit Medical History",
                (MedicalHistoryEditController c) -> c.init(navigator, patientId, entry.getId()));
        if (controller.isChanged()) {
            reloadHistory();
        }
    }

    @FXML
    private void handleViewDeletedHistory() {
        DeletedHistoryEntriesController controller = navigator.showModal(windowOwner(),
                "/fxml/DeletedHistoryEntries.fxml", "Deleted History Entries",
                (DeletedHistoryEntriesController c) -> c.init(navigator, patientId));
        if (controller.isChanged()) {
            reloadHistory();
        }
    }

    @FXML
    private void handlePrint() {
        navigator.showModal(windowOwner(), "/fxml/PrintPreview.fxml", "Print Preview",
                (PrintPreviewController controller) -> controller.init(navigator, patientId));
    }

    @FXML
    private void handleBack() {
        // Close this modal window; the landing page underneath is already visible (FR-023, FR-025).
        if (ownStage != null) {
            ownStage.close();
        }
    }

    private String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }
}
