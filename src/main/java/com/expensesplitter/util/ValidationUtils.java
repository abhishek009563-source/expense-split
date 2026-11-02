package com.expensesplitter.util;

import java.util.regex.Pattern;

public class ValidationUtils {

    // RFC 5322 compliant simplified email regex
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$"
    );

    // International E.164 phone number pattern: + followed by country code (1-3 digits) and subscriber number (4-12 digits)
    // Minimum 7 digits, maximum 15 digits total.
    // Examples: +919876543210, +14155552671, +447911123456
    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "^\\+[1-9]\\d{6,14}$"
    );

    /**
     * Validates whether an email address has a valid syntax and domain.
     */
    public static boolean isValidEmail(String email) {
        if (email == null) return false;
        String trimmed = email.trim();
        return !trimmed.isEmpty() && trimmed.length() <= 254 && EMAIL_PATTERN.matcher(trimmed).matches();
    }

    /**
     * Normalizes a phone number by stripping whitespace, hyphens, and brackets.
     */
    public static String normalizePhoneNumber(String phone) {
        if (phone == null) return "";
        return phone.replaceAll("[\\s\\-\\(\\)]", "").trim();
    }

    /**
     * Validates that the phone number contains a valid country code (starting with +)
     * and adheres to international E.164 standards.
     */
    public static boolean isValidPhoneNumber(String phone) {
        if (phone == null) return false;
        String normalized = normalizePhoneNumber(phone);
        return PHONE_PATTERN.matcher(normalized).matches();
    }

    /**
     * Validates contact details and throws an IllegalArgumentException if invalid.
     */
    public static void validateContactDetails(String fullName, String email, String phone) {
        if (fullName == null || fullName.trim().isEmpty()) {
            throw new IllegalArgumentException("Full name is required.");
        }
        if (!isValidEmail(email)) {
            throw new IllegalArgumentException("Invalid email address format. Example: user@example.com");
        }
        if (phone != null && !phone.trim().isEmpty()) {
            if (!isValidPhoneNumber(phone)) {
                throw new IllegalArgumentException("Mobile number must include a valid country code starting with '+' (e.g. +91 9876543210 or +1 4155552671).");
            }
        }
    }
}

