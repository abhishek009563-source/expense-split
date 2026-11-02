package com.expensesplitter.service;

import com.expensesplitter.database.DatabaseManager;

import java.io.File;
import java.io.FileInputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class NotificationConfig {

    private static final Map<String, String> configMap = new HashMap<>();
    private static boolean initialized = false;

    public synchronized static void load() {
        configMap.clear();

        // 1. Load from .env file if available in working directory or parent
        loadDotEnvFile(new File(".env"));
        loadDotEnvFile(new File("expense_splitter/.env"));

        // 2. Load from System Environment variables (highest priority)
        copyEnvVar("TWILIO_ACCOUNT_SID");
        copyEnvVar("TWILIO_AUTH_TOKEN");
        copyEnvVar("TWILIO_PHONE_NUMBER");
        copyEnvVar("MSG91_AUTH_KEY");
        copyEnvVar("MSG91_SENDER_ID");
        copyEnvVar("MSG91_TEMPLATE_ID");
        copyEnvVar("SMTP_HOST");
        copyEnvVar("SMTP_PORT");
        copyEnvVar("SMTP_USERNAME");
        copyEnvVar("SMTP_PASSWORD");
        copyEnvVar("SMTP_FROM_EMAIL");
        copyEnvVar("NOTIFICATION_MODE"); // "LIVE" or "SIMULATION"
        copyEnvVar("SMS_GATEWAY_PROVIDER"); // "TWILIO" or "MSG91"

        // 3. Load from SQLite app_settings
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT key, value FROM app_settings WHERE key LIKE 'notif_%' OR key = 'notification_mode'")) {
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String k = rs.getString("key").toUpperCase();
                    String v = rs.getString("value");
                    if (v != null && !v.isBlank()) {
                        configMap.put(k, v.trim());
                    }
                }
            }
        } catch (Exception ignored) {
        }

        initialized = true;
    }

    private static void copyEnvVar(String key) {
        String val = System.getenv(key);
        if (val != null && !val.isBlank()) {
            configMap.put(key, val.trim());
        }
    }

    private static void loadDotEnvFile(File file) {
        if (!file.exists() || !file.isFile()) return;
        try (FileInputStream fis = new FileInputStream(file)) {
            Properties props = new Properties();
            props.load(fis);
            for (String name : props.stringPropertyNames()) {
                String val = props.getProperty(name);
                if (val != null && !val.isBlank()) {
                    configMap.put(name.trim().toUpperCase(), val.trim());
                }
            }
        } catch (Exception ignored) {
        }
    }

    public static String get(String key) {
        if (!initialized) load();
        return configMap.get(key.toUpperCase());
    }

    public static String getOrDefault(String key, String def) {
        String val = get(key);
        return val != null && !val.isBlank() ? val : def;
    }

    public static void setSetting(String key, String value) {
        if (!initialized) load();
        configMap.put(key.toUpperCase(), value != null ? value.trim() : "");
        // Save to SQLite
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement("INSERT OR REPLACE INTO app_settings (key, value) VALUES (?, ?);")) {
            stmt.setString(1, key.toLowerCase());
            stmt.setString(2, value != null ? value.trim() : "");
            stmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean isLiveMode() {
        return "LIVE".equalsIgnoreCase(getOrDefault("NOTIFICATION_MODE", "SIMULATION"));
    }

    public static boolean isTwilioConfigured() {
        return get("TWILIO_ACCOUNT_SID") != null && get("TWILIO_AUTH_TOKEN") != null && get("TWILIO_PHONE_NUMBER") != null;
    }

    public static boolean isMsg91Configured() {
        return get("MSG91_AUTH_KEY") != null;
    }

    public static boolean isSmtpConfigured() {
        return get("SMTP_HOST") != null && get("SMTP_USERNAME") != null && get("SMTP_PASSWORD") != null;
    }
}

