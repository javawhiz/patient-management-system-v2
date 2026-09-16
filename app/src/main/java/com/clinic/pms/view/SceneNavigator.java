package com.clinic.pms.view;

import com.clinic.pms.config.AppContext;
import java.io.IOException;
import java.util.function.Consumer;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.Window;

/** Loads FXML views onto the primary Stage and injects the shared {@link AppContext}. */
public class SceneNavigator {

    private final Stage primaryStage;
    private final AppContext context;

    public SceneNavigator(Stage primaryStage, AppContext context) {
        this.primaryStage = primaryStage;
        this.context = context;
    }

    public void show(String fxmlPath, String title) {
        show(fxmlPath, title, null);
    }

    public <C> void show(String fxmlPath, String title, Consumer<C> configureController) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            Object controller = loader.getController();
            if (controller instanceof AppAware aware) {
                aware.setContext(context);
            }
            if (configureController != null && controller != null) {
                @SuppressWarnings("unchecked")
                C typed = (C) controller;
                configureController.accept(typed);
            }
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/css/application.css").toExternalForm());
            boolean wasMaximized = primaryStage.isMaximized();
            primaryStage.setScene(scene);
            primaryStage.setTitle(title);
            primaryStage.show();
            // Swapping the scene can clear the maximized flag; restore it so navigation never shrinks the window.
            if (wasMaximized && !primaryStage.isMaximized()) {
                primaryStage.setMaximized(true);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load view: " + fxmlPath, e);
        }
    }

    public Stage stage() {
        return primaryStage;
    }

    /** Opens {@code fxmlPath} as an owned modal window stacked above {@code owner} (FR-023, FR-024, FR-025). */
    public <C> C showModal(Window owner, String fxmlPath, String title, Consumer<C> configureController) {
        return ModalWindowHelper.open(owner, context, fxmlPath, title, configureController);
    }
}
