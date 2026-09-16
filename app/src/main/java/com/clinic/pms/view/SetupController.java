package com.clinic.pms.view;

import com.clinic.pms.config.AppContext;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

/** First-run setup (FR-021): doctor sets initial credentials + clinic profile. No defaults ship. */
public class SetupController implements AppAware {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private TextField doctorNameField;
    @FXML private TextField clinicNameField;
    @FXML private TextField contactDetailsField;
    @FXML private TextField registrationNumberField;
    @FXML private Label errorLabel;
    @FXML private Button submitButton;

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
    private void handleSubmit() {
        errorLabel.setText(null);
        try {
            context.authService.createInitialCredential(usernameField.getText(), passwordField.getText());
            context.clinicProfileService.saveInitialProfile(
                    doctorNameField.getText(), clinicNameField.getText(),
                    contactDetailsField.getText(), registrationNumberField.getText());
            navigator.show("/fxml/Login.fxml", "Patient Management System - Log In",
                    (LoginController controller) -> controller.setNavigator(navigator));
        } catch (RuntimeException e) {
            errorLabel.setText(e.getMessage());
        }
    }
}
