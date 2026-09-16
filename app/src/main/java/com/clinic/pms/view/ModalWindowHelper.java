package com.clinic.pms.view;

import com.clinic.pms.config.AppContext;
import java.io.IOException;
import java.util.function.Consumer;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

/**
 * Opens FXML views as owned modal child windows (FR-023, FR-024, FR-025): the
 * owner window remains visible behind the modal and cannot be interacted with
 * until the modal closes, letting the doctor stack patient profile, print
 * preview, and medical-history-edit windows without losing the window behind.
 */
public final class ModalWindowHelper {

    private ModalWindowHelper() {}

    /** Loads {@code fxmlPath} into a new owned modal {@link Stage}, blocks until it closes, then returns the controller. */
    public static <C> C open(Window owner, AppContext context, String fxmlPath, String title,
            Consumer<C> configureController) {
        try {
            FXMLLoader loader = new FXMLLoader(ModalWindowHelper.class.getResource(fxmlPath));
            Parent root = loader.load();
            Object controller = loader.getController();
            if (controller instanceof AppAware aware) {
                aware.setContext(context);
            }

            Stage modalStage = new Stage();
            if (owner != null) {
                modalStage.initOwner(owner);
            }
            modalStage.initModality(Modality.WINDOW_MODAL);
            modalStage.setTitle(title);
            Scene scene = new Scene(root);
            scene.getStylesheets().add(ModalWindowHelper.class.getResource("/css/application.css").toExternalForm());
            modalStage.setScene(scene);

            if (controller instanceof WindowAware windowAware) {
                windowAware.setWindow(modalStage);
            }

            @SuppressWarnings("unchecked")
            C typedController = (C) controller;
            if (configureController != null && controller != null) {
                configureController.accept(typedController);
            }

            sizeRelativeToOwner(modalStage, owner);
            modalStage.showAndWait();
            return typedController;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load modal view: " + fxmlPath, e);
        }
    }

    /** Sizes the modal smaller than its owner so the owner window stays visible behind it. */
    private static void sizeRelativeToOwner(Stage modalStage, Window owner) {
        if (owner instanceof Stage ownerStage && ownerStage.getWidth() > 0 && ownerStage.getHeight() > 0) {
            modalStage.setWidth(Math.max(640, ownerStage.getWidth() * 0.82));
            modalStage.setHeight(Math.max(480, ownerStage.getHeight() * 0.82));
        } else {
            modalStage.setWidth(900);
            modalStage.setHeight(680);
        }
        modalStage.centerOnScreen();
    }
}
