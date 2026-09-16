package com.clinic.pms;

import com.clinic.pms.config.AppContext;
import com.clinic.pms.view.AlertHelper;
import com.clinic.pms.view.LoginController;
import com.clinic.pms.view.LogoutHelper;
import com.clinic.pms.view.SceneNavigator;
import com.clinic.pms.view.SetupController;
import javafx.application.Application;
import javafx.geometry.Rectangle2D;
import javafx.scene.image.Image;
import javafx.stage.Screen;
import javafx.stage.Stage;

/**
 * Entry point (constitution Principle VII): launches directly as a native
 * desktop window — no server process, browser, or URL is ever involved.
 */
public class PatientManagementApp extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.getIcons().add(new Image(
            getClass().getResourceAsStream("/icons/pulse-cross.png")));
        AppContext context = new AppContext();
        SceneNavigator navigator = new SceneNavigator(primaryStage, context);

        // Sizing the stage explicitly takes it out of JavaFX auto-size mode, otherwise every
        // setScene() would trigger an implicit sizeToScene() and undo the maximized state.
        Rectangle2D visualBounds = Screen.getPrimary().getVisualBounds();
        primaryStage.setWidth(visualBounds.getWidth());
        primaryStage.setHeight(visualBounds.getHeight());
        primaryStage.setX(visualBounds.getMinX());
        primaryStage.setY(visualBounds.getMinY());
        primaryStage.setMinWidth(Math.min(1024, visualBounds.getWidth()));
        primaryStage.setMinHeight(Math.min(700, visualBounds.getHeight()));
        primaryStage.setMaximized(true);

        primaryStage.setOnCloseRequest(event -> {
            if (!AlertHelper.confirmWarning(primaryStage, "Confirm Exit",
                    "Are you sure you want to close the application?")) {
                event.consume();
                return;
            }
            LogoutHelper.showBackupReminderIfDue(primaryStage, context);
        });

        if (context.authService.isInitialized()) {
            navigator.show("/fxml/Login.fxml", "Patient Management System - Log In",
                    (LoginController controller) -> controller.setNavigator(navigator));
        } else {
            navigator.show("/fxml/Setup.fxml", "Patient Management System - First-Time Setup",
                    (SetupController controller) -> controller.setNavigator(navigator));
        }
    }
}
