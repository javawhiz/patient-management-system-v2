package com.clinic.pms.view;

import com.clinic.pms.config.AppContext;
import com.clinic.pms.entity.MedicalHistoryEntry;
import javafx.fxml.FXML;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;

public class MedicalHistoryEditController implements AppAware, WindowAware {

    @FXML private DatePicker entryDatePicker;
    @FXML private TextArea diagnosisArea;
    @FXML private TextArea prescriptionArea;
    @FXML private TextArea notesArea;

    private AppContext context;
    private SceneNavigator navigator;
    private Stage ownStage;
    private long patientId;
    private long entryId;
    private boolean changed;

    @Override
    public void setContext(AppContext context) {
        this.context = context;
    }

    @Override
    public void setWindow(Stage stage) {
        this.ownStage = stage;
    }

    private Stage windowOwner() {
        return ownStage != null ? ownStage : navigator.stage();
    }

    public void init(SceneNavigator navigator, long patientId, long entryId) {
        this.navigator = navigator;
        this.patientId = patientId;
        this.entryId = entryId;
        MedicalHistoryEntry entry = context.medicalHistoryService.getForEdit(patientId, entryId);
        entryDatePicker.setValue(entry.getEntryDate());
        diagnosisArea.setText(entry.getDiagnosis());
        prescriptionArea.setText(entry.getPrescription());
        notesArea.setText(entry.getNotes());
    }

    public boolean isChanged() {
        return changed;
    }

    @FXML
    private void handleSave() {
        try {
            context.medicalHistoryService.update(patientId, entryId, entryDatePicker.getValue(),
                    diagnosisArea.getText(), prescriptionArea.getText(), notesArea.getText());
            changed = true;
            close();
        } catch (RuntimeException e) {
            AlertHelper.showError(windowOwner(), e.getMessage());
        }
    }

    @FXML
    private void handleSoftDelete() {
        boolean confirmed = AlertHelper.confirm(windowOwner(),
                "Delete this history entry? It will be hidden from normal history and printing but kept in backups.");
        if (!confirmed) {
            return;
        }
        context.medicalHistoryService.softDelete(patientId, entryId);
        changed = true;
        close();
    }

    @FXML
    private void handleCancel() {
        close();
    }

    private void close() {
        if (ownStage != null) {
            ownStage.close();
        } else {
            ((Stage) entryDatePicker.getScene().getWindow()).close();
        }
    }
}