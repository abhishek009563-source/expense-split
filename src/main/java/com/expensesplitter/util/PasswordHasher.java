package com.expensesplitter.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class PasswordHasher {

    private static final String SALT = "ExpenseSplitter$SecureSalt#2026!";

    /**
     * Hashes a plain password using salted SHA-256.
     */
    public static String hash(String password) {
        if (password == null) password = "";
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String salted = password + SALT;
            byte[] hash = digest.digest(salted.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Verifies if a raw password matches the stored hash.
     */
    public static boolean verify(String rawPassword, String storedHash) {
        if (rawPassword == null || storedHash == null) return false;
        return hash(rawPassword).equalsIgnoreCase(storedHash);
    }
}

