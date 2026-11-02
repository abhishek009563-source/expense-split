package com.expensesplitter.util;

import java.text.DecimalFormat;

public class CurrencyFormatter {

    private static String currencySymbol = "₹";
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#,##0.00");

    public static String getCurrencySymbol() {
        return currencySymbol;
    }

    public static void setCurrencySymbol(String symbol) {
        if (symbol != null && !symbol.isBlank()) {
            currencySymbol = symbol;
        }
    }

    /**
     * Formats integer paise to string with currency symbol (e.g. ₹900.50).
     */
    public static String format(long paise) {
        double amount = paise / 100.0;
        return currencySymbol + " " + DECIMAL_FORMAT.format(amount);
    }

    /**
     * Formats integer paise to string without currency symbol (e.g. 900.50).
     */
    public static String formatPlain(long paise) {
        double amount = paise / 100.0;
        return DECIMAL_FORMAT.format(amount);
    }

    /**
     * Converts rupees/dollars input string to integer paise/cents.
     * Throws IllegalArgumentException on invalid input.
     */
    public static long parseToPaise(String input) {
        if (input == null || input.isBlank()) {
            throw new IllegalArgumentException("Amount cannot be empty.");
        }
        String clean = input.replaceAll("[^0-9.]", "").trim();
        if (clean.isEmpty()) {
            throw new IllegalArgumentException("Invalid amount entered.");
        }
        try {
            double value = Double.parseDouble(clean);
            if (value < 0) {
                throw new IllegalArgumentException("Amount cannot be negative.");
            }
            if (value > 100000000.0) { // 10 Crore limit
                throw new IllegalArgumentException("Amount exceeds maximum allowed limit.");
            }
            return Math.round(value * 100.0);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid number format: " + input);
        }
    }
}

