package com.clinic.pms.view;

import com.clinic.pms.config.AppContext;
import com.clinic.pms.entity.Patient;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.Callback;

/**
 * Separate "Deleted Patients" view (FR-020a): hidden from the normal
 * PatientList by default; deleted patients are never shown greyed-out or
 * mixed into normal views. Restoring a patient makes it fully visible again.
 */
public class DeletedPatientsController implements AppAware {

    @FXML private TableView<Patient> patientTable;
    @FXML private TableColumn<Patient, String> nameColumn;
    @FXML private TableColumn<Patient, String> phoneColumn;
    @FXML private TableColumn<Patient, String> deletedAtColumn;
    @FXML private TableColumn<Patient, Void> restoreColumn;

    private AppContext context;
    private SceneNavigator navigator;

    @Override
    public void setContext(AppContext context) {
        this.context = context;
    }

    public void setNavigator(SceneNavigator navigator) {
        this.navigator = navigator;
        setupTable();
        reload();
    }

    private void setupTable() {
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        phoneColumn.setCellValueFactory(new PropertyValueFactory<>("phoneNumber"));
        deletedAtColumn.setCellValueFactory(new PropertyValueFactory<>("deletedAt"));
        restoreColumn.setCellFactory(restoreButtonFactory());
    }

    private Callback<TableColumn<Patient, Void>, TableCell<Patient, Void>> restoreButtonFactory() {
        return column -> new TableCell<>() {
            private final Button button = new Button("Restore");

            {
                button.getStyleClass().add("button-restore");
                button.setOnAction(event -> {
                    Patient patient = getTableView().getItems().get(getIndex());
                    context.patientService.restore(patient.getId());
                    AlertHelper.showSuccess(navigator.stage(), "Patient restored");
                    reload();
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : button);
            }
        };
    }

    private void reload() {
        patientTable.setItems(FXCollections.observableArrayList(context.patientService.listDeleted()));
    }

    @FXML
    private void handleBack() {
        navigator.show("/fxml/PatientList.fxml", "Patient Management System - Patients",
                (PatientListController controller) -> controller.setNavigator(navigator));
    }
}
