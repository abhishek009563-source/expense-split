package com.expensesplitter.model;

public enum SplitMethod {
    EQUAL("Equally Divided"),
    EXACT("Exact Amounts"),
    PERCENTAGE("Custom Percentage");

    private final String displayName;

    SplitMethod(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }

    public static SplitMethod fromString(String text) {
        if (text == null) return EQUAL;
        for (SplitMethod m : SplitMethod.values()) {
            if (m.name().equalsIgnoreCase(text) || m.displayName.equalsIgnoreCase(text)) {
                return m;
            }
        }
        return EQUAL;
    }
}

