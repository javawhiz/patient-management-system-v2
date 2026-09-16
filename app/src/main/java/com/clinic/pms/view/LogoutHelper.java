package com.clinic.pms.view;

import com.clinic.pms.config.AppContext;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Window;

/** Logout with a confirmation prompt and a non-blocking backup reminder shown when overdue (FR-019a). */
public final class LogoutHelper {

    private LogoutHelper() {}

    public static void logout(Window owner, AppContext context, SceneNavigator navigator) {
        if (!AlertHelper.confirmWarning(owner, "Confirm Logout", "Are you sure you want to log out?")) {
            return;
        }
        showBackupReminderIfDue(owner, context);
        navigator.show("/fxml/Login.fxml", "Patient Management System - Log In",
                (LoginController controller) -> controller.setNavigator(navigator));
    }

    /** Shown after logout/close is confirmed so it never overlaps with the confirmation prompt. */
    public static void showBackupReminderIfDue(Window owner, AppContext context) {
        if (context.backupReminderService.isReminderDue()) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION,
                    "It has been more than 5 days since your last backup (or you have never taken one). "
                            + "We recommend backing up your patient data regularly.",
                    ButtonType.OK);
            alert.setTitle("Back up your data?");
            alert.setHeaderText(null);
            if (owner != null) {
                alert.initOwner(owner);
            }
            alert.showAndWait();
        }
    }
}
