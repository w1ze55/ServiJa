package com.servija.servija.model;

import java.util.Locale;

public enum NotificationCategory {
    MESSAGES,
    PROJECTS,
    PAYMENTS;

    public String getValue() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static NotificationCategory fromValue(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
