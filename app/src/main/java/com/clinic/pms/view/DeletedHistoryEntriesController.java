package com.clinic.pms.view;

import com.clinic.pms.config.AppContext;
import com.clinic.pms.entity.MedicalHistoryEntry;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import javafx.util.Callback;

/** Modal listing a patient's soft-deleted history entries with a per-row Restore action. */
public class DeletedHistoryEntriesController implements AppAware, WindowAware {

    @FXML private TableView<MedicalHistoryEntry> entryTable;
    @FXML private TableColumn<MedicalHistoryEntry, String> dateColumn;
    @FXML private TableColumn<MedicalHistoryEntry, String> diagnosisColumn;
    @FXML private TableColumn<MedicalHistoryEntry, String> deletedAtColumn;
    @FXML private TableColumn<MedicalHistoryEntry, Void> restoreColumn;

    private AppContext context;
    private long patientId;
    private Stage ownStage;
    private boolean changed;

    @Override
    public void setContext(AppContext context) {
        this.context = context;
    }

    @Override
    public void setWindow(Stage stage) {
        this.ownStage = stage;
    }

    public void init(SceneNavigator navigator, long patientId) {
        this.patientId = patientId;
        setupTable();
        reload();
    }

    public boolean isChanged() {
        return changed;
    }

    private void setupTable() {
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("entryDate"));
        diagnosisColumn.setCellValueFactory(new PropertyValueFactory<>("diagnosis"));
        deletedAtColumn.setCellValueFactory(new PropertyValueFactory<>("deletedAt"));
        restoreColumn.setCellFactory(restoreButtonFactory());
    }

    private Callback<TableColumn<MedicalHistoryEntry, Void>, TableCell<MedicalHistoryEntry, Void>> restoreButtonFactory() {
        return column -> new TableCell<>() {
            private final Button button = new Button("Restore");

            {
                button.getStyleClass().add("button-restore");
                button.setOnAction(event -> {
                    MedicalHistoryEntry entry = getTableView().getItems().get(getIndex());
                    context.medicalHistoryService.restore(patientId, entry.getId());
                    changed = true;
                    AlertHelper.showSuccess(ownStage, "History entry restored");
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
        entryTable.setItems(FXCollections.observableArrayList(
                context.medicalHistoryService.listDeletedForPatient(patientId)));
    }

    @FXML
    private void handleClose() {
        if (ownStage != null) {
            ownStage.close();
        }
    }
}
