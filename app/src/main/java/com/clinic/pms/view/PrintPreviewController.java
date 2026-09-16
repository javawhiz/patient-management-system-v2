package com.clinic.pms.view;

import com.clinic.pms.config.AppContext;
import com.clinic.pms.entity.DoctorClinicProfile;
import com.clinic.pms.entity.MedicalHistoryEntry;
import com.clinic.pms.entity.Patient;
import javafx.fxml.FXML;
import javafx.print.Printer;
import javafx.print.PrinterJob;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

/**
 * Renders the patient handout (patient + clinic info always included; Current
 * Treatment Plan, Medical History, and Notes optional, unchecked by default
 * per FR-014) and prints it via the native OS print dialog (Java
 * PrinterJob) — no PDF library. Opens as an owned modal window above the
 * patient profile (FR-024).
 */
public class PrintPreviewController implements AppAware, WindowAware {

    @FXML private CheckBox currentTreatmentCheckBox;
    @FXML private CheckBox historyCheckBox;
    @FXML private CheckBox notesCheckBox;
    @FXML private VBox printRoot;

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

    @Override
    public void setContext(AppContext context) {
        this.context = context;
    }

    public void init(SceneNavigator navigator, long patientId) {
        this.navigator = navigator;
        this.patientId = patientId;
        historyCheckBox.disableProperty().bind(currentTreatmentCheckBox.selectedProperty().not());
        currentTreatmentCheckBox.selectedProperty().addListener((observable, oldValue, selected) -> {
            if (!selected) {
                historyCheckBox.setSelected(false);
            }
        });
        refreshPreview();
    }

    @FXML
    private void refreshPreview() {
        printRoot.getChildren().clear();

        Patient patient = context.patientService.getById(patientId);
        DoctorClinicProfile clinic = context.clinicProfileService.get();

        printRoot.getChildren().add(heading(clinic.getClinicName(), 18));
        printRoot.getChildren().add(new Label(clinic.getDoctorName() + " - Reg. No. " + clinic.getRegistrationNumber()));
        printRoot.getChildren().add(new Label(clinic.getContactDetails()));

        printRoot.getChildren().add(heading("Patient Information", 14));
        printRoot.getChildren().add(new Label("Name: " + patient.getFullName()));
        printRoot.getChildren().add(new Label("Date of birth: " + patient.getDateOfBirth()));
        printRoot.getChildren().add(new Label("Gender: " + patient.getGender()));
        printRoot.getChildren().add(new Label("Phone: " + patient.getPhoneNumber()));

        boolean includeNotes = notesCheckBox.isSelected();

        if (currentTreatmentCheckBox.isSelected()) {
            addSeparator();
            printRoot.getChildren().add(heading("Current Treatment Plan", 14));
            var entries = context.medicalHistoryService.listForPatient(patientId);
            if (entries.isEmpty()) {
                printRoot.getChildren().add(wrapLabel("No visits recorded."));
            } else {
                addVisitBlock(entries.get(0), includeNotes);
            }

            if (historyCheckBox.isSelected()) {
                addSeparator();
                printRoot.getChildren().add(heading("Medical History", 14));
                if (entries.size() <= 1) {
                    printRoot.getChildren().add(wrapLabel("No prior visits recorded."));
                } else {
                    var priorEntries = entries.stream().skip(1).toList();
                    for (int i = 0; i < priorEntries.size(); i++) {
                        addVisitBlock(priorEntries.get(i), includeNotes);
                        if (i < priorEntries.size() - 1) {
                            addSeparator();
                        }
                    }
                }
            }
        }
    }

    private void addVisitBlock(MedicalHistoryEntry entry, boolean includeNotes) {
        printRoot.getChildren().add(boldLabel("Visit Date: " + entry.getEntryDate()));
        printRoot.getChildren().add(fieldBlock("Diagnosis:", entry.getDiagnosis()));
        if (hasText(entry.getPrescription())) {
            printRoot.getChildren().add(fieldBlock("Prescription:", entry.getPrescription()));
        }
        if (includeNotes && hasText(entry.getNotes())) {
            printRoot.getChildren().add(fieldBlock("Notes:", entry.getNotes()));
        }
    }

    // A plain Region (rather than Separator) keeps the rule solid black so it survives printing regardless of skin CSS.
    private void addSeparator() {
        Region rule = new Region();
        rule.setStyle("-fx-background-color: black;");
        rule.setPrefHeight(1);
        rule.setMinHeight(1);
        rule.setMaxHeight(1);
        rule.prefWidthProperty().bind(printRoot.widthProperty());
        printRoot.getChildren().add(rule);
        printRoot.getChildren().add(new Label(" "));
    }

    private VBox fieldBlock(String label, String value) {
        return new VBox(2, boldLabel(label), wrapLabel(value));
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private Label wrapLabel(String text) {
        Label label = new Label(text);
        label.setWrapText(true);
        return label;
    }

    private Label boldLabel(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");
        return label;
    }

    private Label heading(String text, double size) {
        Label label = new Label(text);
        label.setStyle("-fx-font-weight: bold; -fx-font-size: " + size + "px;");
        return label;
    }

    @FXML
    private void handlePrint() {
        Printer printer = Printer.getDefaultPrinter();
        if (printer == null) {
            AlertHelper.showError(windowOwner(), "No printer is configured on this machine.");
            return;
        }
        PrinterJob job = PrinterJob.createPrinterJob(printer);
        if (job == null) {
            AlertHelper.showError(windowOwner(), "Could not start a print job.");
            return;
        }
        boolean proceed = job.showPrintDialog(windowOwner());
        if (!proceed) {
            return;
        }
        boolean success = printAllPages(job);
        if (success) {
            job.endJob();
            AlertHelper.showSuccess(windowOwner(), "Sent to printer");
        } else {
            AlertHelper.showError(windowOwner(), "Printing failed");
        }
    }

    // Splits printRoot into page-sized vertical slices so content taller than one page prints in full (FR-024).
    private boolean printAllPages(PrinterJob job) {
        double pageHeight = job.getJobSettings().getPageLayout().getPrintableHeight();
        double contentHeight = printRoot.getBoundsInLocal().getHeight();
        double contentWidth = printRoot.getBoundsInLocal().getWidth();
        int totalPages = Math.max(1, (int) Math.ceil(contentHeight / pageHeight));

        boolean allSucceeded = true;
        try {
            for (int page = 0; page < totalPages; page++) {
                double sliceY = page * pageHeight;
                Rectangle clip = new Rectangle(0, sliceY, contentWidth, pageHeight);
                printRoot.setClip(clip);
                printRoot.setTranslateY(-sliceY);
                if (!job.printPage(printRoot)) {
                    allSucceeded = false;
                    break;
                }
            }
        } finally {
            printRoot.setClip(null);
            printRoot.setTranslateY(0);
        }
        return allSucceeded;
    }

    @FXML
    private void handleBack() {
        // Close this modal window; the patient profile underneath is already visible (FR-024, FR-025).
        if (ownStage != null) {
            ownStage.close();
        }
    }
}
