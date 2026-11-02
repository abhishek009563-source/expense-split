package com.expensesplitter.model;

public enum NotificationChannel {
    EMAIL("Email", "✉️"),
    SMS("SMS", "📱"),
    IN_APP("In-App", "🔔");

    private final String displayName;
    private final String icon;

    NotificationChannel(String displayName, String icon) {
        this.displayName = displayName;
        this.icon = icon;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getIcon() {
        return icon;
    }

    public static NotificationChannel fromString(String val) {
        if (val == null) return IN_APP;
        for (NotificationChannel c : values()) {
            if (c.name().equalsIgnoreCase(val) || c.displayName.equalsIgnoreCase(val)) {
                return c;
            }
        }
        return IN_APP;
    }
}

