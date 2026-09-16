package com.clinic.pms.view;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Window;
import java.util.Optional;

/** Shared success/failure/confirmation dialogs (FR-017) — every operation gives clear feedback. */
public final class AlertHelper {

    private AlertHelper() {}

    public static void showSuccess(Window owner, String message) {
        show(owner, Alert.AlertType.INFORMATION, "Success", message);
    }

    public static void showError(Window owner, String message) {
        show(owner, Alert.AlertType.ERROR, "Error", message);
    }

    public static boolean confirm(Window owner, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, message, ButtonType.OK, ButtonType.CANCEL);
        alert.setHeaderText(null);
        if (owner != null) {
            alert.initOwner(owner);
        }
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    /** Confirmation dialog with a warning icon, used for destructive/session-ending actions. */
    public static boolean confirmWarning(Window owner, String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING, message, ButtonType.OK, ButtonType.CANCEL);
        alert.setTitle(title);
        alert.setHeaderText(null);
        if (owner != null) {
            alert.initOwner(owner);
        }
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    private static void show(Window owner, Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type, message, ButtonType.OK);
        alert.setTitle(title);
        alert.setHeaderText(null);
        if (owner != null) {
            alert.initOwner(owner);
        }
        alert.showAndWait();
    }
}
