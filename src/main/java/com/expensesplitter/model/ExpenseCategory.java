package com.expensesplitter.model;

public enum ExpenseCategory {
    FOOD("Food & Dining", "#FF6B6B"),
    TRAVEL("Travel & Transport", "#4D96FF"),
    SHOPPING("Shopping", "#9B51E0"),
    RENT("House Rent", "#6BCB77"),
    ELECTRICITY("Electricity & Utilities", "#FFD93D"),
    ENTERTAINMENT("Entertainment", "#FF76CE"),
    GROCERIES("Groceries", "#20BF6B"),
    OTHER("Other Miscellaneous", "#A4B0BE");

    private final String displayName;
    private final String colorHex;

    ExpenseCategory(String displayName, String colorHex) {
        this.displayName = displayName;
        this.colorHex = colorHex;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getColorHex() {
        return colorHex;
    }

    @Override
    public String toString() {
        return displayName;
    }

    public static ExpenseCategory fromString(String text) {
        if (text == null) return OTHER;
        for (ExpenseCategory c : ExpenseCategory.values()) {
            if (c.name().equalsIgnoreCase(text) || c.displayName.equalsIgnoreCase(text)) {
                return c;
            }
        }
        return OTHER;
    }
}

