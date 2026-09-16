package com.clinic.pms.view;

import com.clinic.pms.config.AppContext;
import com.clinic.pms.service.AuthService;
import javafx.geometry.Insets;
import javafx.fxml.FXML;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

/** Login screen (FR-019): single static credential, generic error on failure (US1.2). */
public class LoginController implements AppAware {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;

    private AppContext context;
    private SceneNavigator navigator;

    @Override
    public void setContext(AppContext context) {
        this.context = context;
    }

    public void setNavigator(SceneNavigator navigator) {
        this.navigator = navigator;
    }

    @FXML
    private void handleLogin() {
        boolean success = context.authService.login(usernameField.getText(), passwordField.getText());
        if (!success) {
            // Identical message regardless of which field was wrong (acceptance scenario US1.2).
            errorLabel.setText("Invalid username or password");
            return;
        }
        navigator.show("/fxml/PatientList.fxml", "Patient Management System - Patients",
                (PatientListController controller) -> controller.setNavigator(navigator));
    }

    @FXML
    private void handleResetPassword() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Reset Password");
        dialog.setHeaderText(AuthService.RECOVERY_QUESTION);

        TextField resetUsernameField = new TextField(usernameField.getText());
        TextField answerField = new TextField();
        PasswordField newPasswordField = new PasswordField();
        PasswordField confirmPasswordField = new PasswordField();
        VBox content = new VBox(8,
                new Label("Username"), resetUsernameField,
                new Label("Answer"), answerField,
                new Label("New password"), newPasswordField,
                new Label("Confirm new password"), confirmPasswordField);
        content.setPadding(new Insets(10));
        dialog.getDialogPane().setContent(content);
        ButtonType resetButton = new ButtonType("Reset Password", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(resetButton, ButtonType.CANCEL);
        dialog.setResultConverter(button -> button == resetButton ? resetButton : null);
        dialog.getDialogPane().lookupButton(resetButton).getStyleClass().add("button-primary");
        dialog.getDialogPane().lookupButton(ButtonType.CANCEL).getStyleClass().add("button-cancel");

        dialog.showAndWait().ifPresent(button -> {
            if (!newPasswordField.getText().equals(confirmPasswordField.getText())
                    || newPasswordField.getText().isBlank()) {
                AlertHelper.showError(navigator.stage(), "New passwords must match and cannot be blank");
                return;
            }
            boolean reset = context.authService.resetPassword(
                    resetUsernameField.getText(), answerField.getText(), newPasswordField.getText());
            if (reset) {
                usernameField.setText(resetUsernameField.getText());
                passwordField.clear();
                AlertHelper.showSuccess(navigator.stage(), "Password reset. You can now log in.");
            } else {
                AlertHelper.showError(navigator.stage(), "Password reset failed. Check the username and answer.");
            }
        });
    }
}
