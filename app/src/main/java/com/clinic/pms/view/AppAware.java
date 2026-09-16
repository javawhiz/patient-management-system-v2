package com.clinic.pms.view;

import com.clinic.pms.config.AppContext;

/** Implemented by FXML controllers that need access to the shared {@link AppContext}. */
public interface AppAware {
    void setContext(AppContext context);
}
