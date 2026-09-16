package com.clinic.pms.view;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.clinic.pms.config.AppContext;
import com.clinic.pms.config.AppPaths;
import com.clinic.pms.config.PersistenceConfig;
import java.nio.file.Files;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.testfx.framework.junit5.ApplicationTest;

/** US1.2: a wrong username or password shows one generic error, never revealing which field was wrong. */
class LoginControllerTest extends ApplicationTest {

    @Override
    public void start(Stage stage) throws Exception {
        resetDatabase();
        AppContext context = new AppContext();
        context.authService.createInitialCredential("doctor", "Secret123");

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Login.fxml"));
        Parent root = loader.load();
        LoginController controller = loader.getController();
        controller.setContext(context);
        controller.setNavigator(new SceneNavigator(stage, context));
        stage.setScene(new Scene(root));
        stage.show();
    }

    @Test
    void showsGenericErrorForWrongPassword() {
        interact(() -> {
            ((TextField) lookup("#usernameField").query()).setText("doctor");
            ((PasswordField) lookup("#passwordField").query()).setText("wrong-password");
        });
        interact(() -> ((Button) lookup("Log In").query()).fire());

        Label errorLabel = lookup("#errorLabel").queryAs(Label.class);
        assertEquals("Invalid username or password", errorLabel.getText());
    }

    @Test
    void showsGenericErrorForUnknownUsername() {
        interact(() -> {
            ((TextField) lookup("#usernameField").query()).setText("someone-else");
            ((PasswordField) lookup("#passwordField").query()).setText("Secret123");
        });
        interact(() -> ((Button) lookup("Log In").query()).fire());

        Label errorLabel = lookup("#errorLabel").queryAs(Label.class);
        assertEquals("Invalid username or password", errorLabel.getText());
    }

    @Test
    void forgotPasswordControlIsPresent() {
        Button button = (Button) lookup("Forgot Password?").query();
        assertEquals("Forgot Password?", button.getText());
    }

    private void resetDatabase() throws Exception {
        PersistenceConfig.shutdown();
        Files.deleteIfExists(AppPaths.h2DatabasePath().resolveSibling("patientdb.mv.db"));
        Files.deleteIfExists(AppPaths.h2DatabasePath().resolveSibling("patientdb.trace.db"));
    }
}
