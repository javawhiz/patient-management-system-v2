package com.clinic.pms.view;

import javafx.stage.Stage;

/** Implemented by controllers opened via {@link ModalWindowHelper} to receive their own modal Stage. */
public interface WindowAware {
    void setWindow(Stage stage);
}
