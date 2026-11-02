package com.expensesplitter.model;

public enum NotificationStatus {
    SENT("Sent", "#10b981"),
    FAILED("Failed", "#ef4444"),
    PENDING("Pending", "#f59e0b");

    private final String displayName;
    private final String colorHex;

    NotificationStatus(String displayName, String colorHex) {
        this.displayName = displayName;
        this.colorHex = colorHex;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getColorHex() {
        return colorHex;
    }

    public static NotificationStatus fromString(String val) {
        if (val == null) return PENDING;
        for (NotificationStatus s : values()) {
            if (s.name().equalsIgnoreCase(val) || s.displayName.equalsIgnoreCase(val)) {
                return s;
            }
        }
        return PENDING;
    }
}

