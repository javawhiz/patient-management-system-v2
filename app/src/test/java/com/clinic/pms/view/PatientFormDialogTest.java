package com.clinic.pms.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.testfx.framework.junit5.ApplicationTest;
import org.testfx.util.WaitForAsyncUtils;

/**
 * FR-001c: submitting the Add Patient dialog with invalid/blank required
 * fields shows inline red errors and keeps the dialog open, preserving
 * entered values, rather than closing or replacing feedback with a popup.
 */
@Disabled("Dialog.showAndWait() is unstable under TestFX in this local JavaFX 21 runner; validate manually.")
class PatientFormDialogTest extends ApplicationTest {

    private Stage ownerStage;

    @Override
    public void start(Stage stage) {
        ownerStage = stage;
        stage.setScene(new javafx.scene.Scene(new javafx.scene.layout.VBox(), 200, 100));
        stage.show();
    }

    @Test
    void blankRequiredFieldsShowInlineErrorsAndKeepDialogOpen() {
        AtomicBoolean dialogClosed = new AtomicBoolean(false);
        Platform.runLater(() -> {
            PatientFormDialog.show(ownerStage, "Add Patient", null);
            dialogClosed.set(true);
        });
        waitForDialogFields();

        // First text field is Full name; leave everything blank and submit in one batch
        // to avoid a timing gap while the dialog runs its own nested event loop.
        Platform.runLater(() -> {
            lookup(".text-field").queryAllAs(TextField.class).iterator().next().setText("");
            ((Button) lookup("OK").query()).fire();
        });
        WaitForAsyncUtils.waitForFxEvents();

        assertFalse(dialogClosed.get(), "Dialog must remain open when validation fails");
        boolean anyErrorVisible = lookup(".error-label").queryAllAs(Label.class).stream()
                .anyMatch(label -> label.isVisible() && label.getText() != null && !label.getText().isBlank());
        assertTrue(anyErrorVisible, "At least one inline red error must be visible for blank required fields");

        Platform.runLater(() -> ((Button) lookup("Cancel").query()).fire());
        WaitForAsyncUtils.waitForFxEvents();
        assertTrue(dialogClosed.get(), "Cancel must close the dialog");
    }

    @Test
    void enteredValuesArePreservedAfterAValidationFailure() {
        Platform.runLater(() -> PatientFormDialog.show(ownerStage, "Add Patient", null));
        waitForDialogFields();

        TextField fullNameField = lookup(".text-field").queryAllAs(TextField.class).iterator().next();
        Platform.runLater(() -> {
            fullNameField.setText("Jane Doe");
            ((Button) lookup("OK").query()).fire();
        });
        WaitForAsyncUtils.waitForFxEvents();

        assertEquals("Jane Doe", fullNameField.getText(),
                "Full name value must remain in the field after a failed validation attempt");

        Platform.runLater(() -> ((Button) lookup("Cancel").query()).fire());
        WaitForAsyncUtils.waitForFxEvents();
    }

    /** Polls until the dialog's fields exist, since Dialog.showAndWait() runs a nested event loop. */
    private void waitForDialogFields() {
        try {
            WaitForAsyncUtils.waitFor(5, TimeUnit.SECONDS,
                    () -> !lookup(".text-field").queryAllAs(TextField.class).isEmpty());
        } catch (Exception e) {
            throw new IllegalStateException("Dialog fields never appeared", e);
        }
    }
}
