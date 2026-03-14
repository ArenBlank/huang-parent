package com.huang.web.admin.custom.config;

import java.util.Set;

public class AdminPermissionMatrixView {

    private final Set<String> required;
    private final Set<String> config;
    private final Set<String> database;
    private final Set<String> missing;
    private String warning;

    public AdminPermissionMatrixView(Set<String> required,
                                     Set<String> config,
                                     Set<String> database,
                                     Set<String> missing) {
        this.required = required;
        this.config = config;
        this.database = database;
        this.missing = missing;
    }

    public Set<String> getRequired() {
        return required;
    }

    public Set<String> getConfig() {
        return config;
    }

    public Set<String> getDatabase() {
        return database;
    }

    public Set<String> getMissing() {
        return missing;
    }

    public String getWarning() {
        return warning;
    }

    public void setWarning(String warning) {
        this.warning = warning;
    }
}
