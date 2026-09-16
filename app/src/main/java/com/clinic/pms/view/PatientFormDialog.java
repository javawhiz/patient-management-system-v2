package com.clinic.pms.view;

import com.clinic.pms.service.PatientService;
import com.clinic.pms.service.PatientValidator;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import javafx.geometry.Insets;
import javafx.collections.FXCollections;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

/** Reusable add/edit dialog for a patient's personal-information fields (FR-001, FR-002). */
public final class PatientFormDialog {

    private PatientFormDialog() {}

    public static Optional<PatientService.PatientData> show(Window owner, String title, PatientService.PatientData initial) {
        Dialog<PatientService.PatientData> dialog = new Dialog<>();
        dialog.setTitle(title);
        if (owner != null) {
            dialog.initOwner(owner);
        }
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        TextField fullNameField = new TextField(initial != null ? initial.fullName() : "");
        DatePicker dobPicker = new DatePicker(initial != null ? initial.dateOfBirth() : null);
        ChoiceBox<String> genderField = new ChoiceBox<>(FXCollections.observableArrayList(
            "Male", "Female", "Undisclosed"));
        if (initial != null && initial.gender() != null && !genderField.getItems().contains(initial.gender())) {
            genderField.getItems().add(initial.gender());
        }
        genderField.setValue(initial != null ? initial.gender() : null);
        TextField phoneField = new TextField(initial != null ? initial.phoneNumber() : "");
        TextField emailField = new TextField(initial != null && initial.email() != null ? initial.email() : "");
        TextArea addressField = new TextArea(initial != null && initial.address() != null ? initial.address() : "");
        addressField.setPrefRowCount(4);
        addressField.setWrapText(true);
        TextField emergencyContactField = new TextField(
                initial != null && initial.emergencyContact() != null ? initial.emergencyContact() : "");

        Label fullNameError = errorLabel();
        Label dobError = errorLabel();
        Label genderError = errorLabel();
        Label phoneError = errorLabel();
        Label emailError = errorLabel();

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);
        grid.setPadding(new Insets(20));
        int row = 0;
        grid.addRow(row++, new Label("Full name"), fieldWithError(fullNameField, fullNameError));
        grid.addRow(row++, new Label("Date of birth"), fieldWithError(dobPicker, dobError));
        grid.addRow(row++, new Label("Gender"), fieldWithError(genderField, genderError));
        grid.addRow(row++, new Label("Phone number"), fieldWithError(phoneField, phoneError));
        grid.addRow(row++, new Label("Email (optional)"), fieldWithError(emailField, emailError));
        grid.addRow(row++, new Label("Address (optional)"), addressField);
        grid.addRow(row, new Label("Emergency contact (optional)"), emergencyContactField);
        dialog.getDialogPane().setContent(grid);

        Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okButton.getStyleClass().add("button-primary");
        Button cancelButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.CANCEL);
        cancelButton.getStyleClass().add("button-cancel");
        okButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
                Map<String, String> errors = initial == null
                    ? PatientValidator.errorsForCreate(formData(
                    fullNameField, dobPicker, genderField, addressField, emergencyContactField,
                    phoneField, emailField))
                    : PatientValidator.errorsForUpdate(formData(
                    fullNameField, dobPicker, genderField, addressField, emergencyContactField,
                    phoneField, emailField), initial.gender());
            clearErrors(fullNameError, dobError, genderError, phoneError, emailError);
            showError(fullNameError, errors.get("fullName"));
            showError(dobError, errors.get("dateOfBirth"));
            showError(genderError, errors.get("gender"));
            showError(phoneError, errors.get("phone"));
            showError(emailError, errors.get("email"));
            if (!errors.isEmpty()) {
                event.consume();
            }
        });

        dialog.setResultConverter(buttonType -> {
            if (buttonType != ButtonType.OK) {
                return null;
            }
            LocalDate dob = dobPicker.getValue();
                return formData(fullNameField, dobPicker, genderField, addressField,
                    emergencyContactField, phoneField, emailField);
        });

        return dialog.showAndWait();
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }

    private static PatientService.PatientData formData(
            TextField fullNameField, DatePicker dobPicker, ChoiceBox<String> genderField,
            TextArea addressField, TextField emergencyContactField, TextField phoneField,
            TextField emailField) {
        return new PatientService.PatientData(
                fullNameField.getText(), dobPicker.getValue(), genderField.getValue(),
                blankToNull(addressField.getText()), blankToNull(emergencyContactField.getText()),
                phoneField.getText(), blankToNull(emailField.getText()));
    }

    private static VBox fieldWithError(Control field, Label error) {
        VBox box = new VBox(2, field, error);
        return box;
    }

    private static Label errorLabel() {
        Label label = new Label();
        label.getStyleClass().add("error-label");
        label.setWrapText(true);
        return label;
    }

    private static void clearErrors(Label... labels) {
        for (Label label : labels) {
            label.setText("");
            label.setVisible(false);
            label.setManaged(false);
        }
    }

    private static void showError(Label label, String message) {
        if (message != null) {
            label.setText(message);
            label.setVisible(true);
            label.setManaged(true);
        }
    }
}
