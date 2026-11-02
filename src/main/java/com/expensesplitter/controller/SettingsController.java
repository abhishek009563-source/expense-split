package com.expensesplitter.controller;

import com.expensesplitter.Main;
import com.expensesplitter.database.DatabaseManager;
import com.expensesplitter.model.User;
import com.expensesplitter.service.AuthService;
import com.expensesplitter.service.NotificationConfig;
import com.expensesplitter.util.AlertHelper;
import com.expensesplitter.util.CurrencyFormatter;
import com.expensesplitter.util.SessionManager;
import com.expensesplitter.util.ValidationUtils;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class SettingsController {

    // Profile & Contact Fields
    @FXML private TextField fullNameField;
    @FXML private TextField emailField;
    @FXML private TextField phoneField;
    @FXML private TextField usernameDisplayField;
    @FXML private CheckBox emailNotifCheckBox;
    @FXML private CheckBox smsNotifCheckBox;

    // Password Fields
    @FXML private PasswordField oldPasswordField;
    @FXML private PasswordField newPasswordField;
    @FXML private PasswordField confirmNewPasswordField;

    // System Preferences
    @FXML private ComboBox<String> currencyComboBox;
    @FXML private ToggleGroup themeToggleGroup;
    @FXML private RadioButton darkThemeRadio;
    @FXML private RadioButton lightThemeRadio;

    // Gateway Configuration
    @FXML private ComboBox<String> notifModeComboBox;
    @FXML private ComboBox<String> smsProviderComboBox;
    @FXML private TextField twilioSidField;
    @FXML private PasswordField twilioTokenField;
    @FXML private TextField twilioFromField;
    @FXML private PasswordField msg91KeyField;
    @FXML private TextField msg91SenderField;
    @FXML private TextField smtpHostField;
    @FXML private TextField smtpPortField;
    @FXML private TextField smtpUserField;
    @FXML private PasswordField smtpPassField;

    private final AuthService authService = new AuthService();

    @FXML
    public void initialize() {
        loadUserProfile();
        loadPreferences();
        loadGatewayConfig();
    }

    private void loadUserProfile() {
        User user = SessionManager.getCurrentUser();
        if (user != null) {
            fullNameField.setText(user.getFullName());
            emailField.setText(user.getEmail());
            phoneField.setText(user.getPhone() != null ? user.getPhone() : "");
            usernameDisplayField.setText(user.getUsername());
            emailNotifCheckBox.setSelected(user.isEmailNotificationsEnabled());
            smsNotifCheckBox.setSelected(user.isSmsNotificationsEnabled());
        }
    }

    private void loadPreferences() {
        currencyComboBox.setItems(FXCollections.observableArrayList(
                "₹ - Indian Rupee (INR)",
                "$ - US Dollar (USD)",
                "€ - Euro (EUR)",
                "£ - British Pound (GBP)"
        ));

        String currentSym = CurrencyFormatter.getCurrencySymbol();
        if ("$".equals(currentSym)) currencyComboBox.getSelectionModel().select(1);
        else if ("€".equals(currentSym)) currencyComboBox.getSelectionModel().select(2);
        else if ("£".equals(currentSym)) currencyComboBox.getSelectionModel().select(3);
        else currencyComboBox.getSelectionModel().select(0);

        if ("light".equalsIgnoreCase(SessionManager.getActiveTheme())) {
            lightThemeRadio.setSelected(true);
        } else {
            darkThemeRadio.setSelected(true);
        }
    }

    private void loadGatewayConfig() {
        notifModeComboBox.setItems(FXCollections.observableArrayList(
                "SIMULATION (Safe Mode - No API keys required)",
                "LIVE (Send Real Emails & SMS Alerts)"
        ));
        notifModeComboBox.getSelectionModel().select(
                NotificationConfig.isLiveMode() ? 1 : 0
        );

        smsProviderComboBox.setItems(FXCollections.observableArrayList("TWILIO", "MSG91"));
        smsProviderComboBox.getSelectionModel().select(
                NotificationConfig.getOrDefault("SMS_GATEWAY_PROVIDER", "TWILIO")
        );

        twilioSidField.setText(NotificationConfig.getOrDefault("TWILIO_ACCOUNT_SID", ""));
        twilioTokenField.setText(NotificationConfig.getOrDefault("TWILIO_AUTH_TOKEN", ""));
        twilioFromField.setText(NotificationConfig.getOrDefault("TWILIO_PHONE_NUMBER", ""));

        msg91KeyField.setText(NotificationConfig.getOrDefault("MSG91_AUTH_KEY", ""));
        msg91SenderField.setText(NotificationConfig.getOrDefault("MSG91_SENDER_ID", "EXPSPL"));

        smtpHostField.setText(NotificationConfig.getOrDefault("SMTP_HOST", "smtp.gmail.com"));
        smtpPortField.setText(NotificationConfig.getOrDefault("SMTP_PORT", "587"));
        smtpUserField.setText(NotificationConfig.getOrDefault("SMTP_USERNAME", ""));
        smtpPassField.setText(NotificationConfig.getOrDefault("SMTP_PASSWORD", ""));
    }

    @FXML
    public void handleSaveProfile() {
        User user = SessionManager.getCurrentUser();
        if (user == null) return;

        String fullName = fullNameField.getText().trim();
        String email = emailField.getText().trim();
        String phone = phoneField.getText().trim();
        boolean emailConsent = emailNotifCheckBox.isSelected();
        boolean smsConsent = smsNotifCheckBox.isSelected();

        try {
            ValidationUtils.validateContactDetails(fullName, email, phone);
            boolean updated = authService.updateProfile(user.getId(), fullName, email, phone, emailConsent, smsConsent);
            if (updated) {
                user.setFullName(fullName);
                user.setEmail(email);
                user.setPhone(ValidationUtils.normalizePhoneNumber(phone));
                user.setEmailNotificationsEnabled(emailConsent);
                user.setSmsNotificationsEnabled(smsConsent);
                AlertHelper.showInformation("Contact Details Saved", "Your profile, contact details, and notification consent preferences have been updated.");
            } else {
                AlertHelper.showError("Update Failed", "Could not update profile information.");
            }
        } catch (IllegalArgumentException e) {
            AlertHelper.showWarning("Validation Error", e.getMessage());
        } catch (Exception e) {
            AlertHelper.showError("Error", e.getMessage());
        }
    }

    @FXML
    public void handleChangePassword() {
        User user = SessionManager.getCurrentUser();
        if (user == null) return;

        String oldPass = oldPasswordField.getText();
        String newPass = newPasswordField.getText();
        String confirm = confirmNewPasswordField.getText();

        if (oldPass.isEmpty() || newPass.isEmpty() || confirm.isEmpty()) {
            AlertHelper.showWarning("Validation", "Please fill in all password fields.");
            return;
        }

        if (!newPass.equals(confirm)) {
            AlertHelper.showWarning("Validation", "New passwords do not match.");
            return;
        }

        try {
            boolean changed = authService.changePassword(user.getId(), oldPass, newPass);
            if (changed) {
                AlertHelper.showInformation("Password Changed", "Your password has been changed successfully.");
                oldPasswordField.clear();
                newPasswordField.clear();
                confirmNewPasswordField.clear();
            } else {
                AlertHelper.showError("Error", "Old password does not match current password.");
            }
        } catch (Exception e) {
            AlertHelper.showError("Error", e.getMessage());
        }
    }

    @FXML
    public void handleSavePreferences() {
        // Currency
        String sel = currencyComboBox.getSelectionModel().getSelectedItem();
        String sym = "₹";
        if (sel != null) {
            if (sel.startsWith("$")) sym = "$";
            else if (sel.startsWith("€")) sym = "€";
            else if (sel.startsWith("£")) sym = "£";
        }
        CurrencyFormatter.setCurrencySymbol(sym);
        saveSetting("currency", sym);

        // Theme
        String theme = lightThemeRadio.isSelected() ? "light" : "dark";
        SessionManager.setActiveTheme(theme);
        saveSetting("theme", theme);
        Main.applyCurrentTheme(Main.getPrimaryStage().getScene());

        AlertHelper.showInformation("Preferences Saved", "Display preferences updated successfully!");
    }

    @FXML
    public void handleSaveGateways() {
        boolean isLive = notifModeComboBox.getSelectionModel().getSelectedIndex() == 1;
        saveSetting("notification_mode", isLive ? "LIVE" : "SIMULATION");

        saveSetting("sms_gateway_provider", smsProviderComboBox.getSelectionModel().getSelectedItem());

        // Twilio
        saveSetting("notif_twilio_account_sid", twilioSidField.getText().trim());
        saveSetting("notif_twilio_auth_token", twilioTokenField.getText().trim());
        saveSetting("notif_twilio_phone_number", twilioFromField.getText().trim());

        // MSG91
        saveSetting("notif_msg91_auth_key", msg91KeyField.getText().trim());
        saveSetting("notif_msg91_sender_id", msg91SenderField.getText().trim());

        // SMTP
        saveSetting("notif_smtp_host", smtpHostField.getText().trim());
        saveSetting("notif_smtp_port", smtpPortField.getText().trim());
        saveSetting("notif_smtp_username", smtpUserField.getText().trim());
        saveSetting("notif_smtp_password", smtpPassField.getText().trim());

        NotificationConfig.load(); // Refresh active configuration

        AlertHelper.showInformation("Gateway Configured",
                isLive ? "Live Email & SMS Gateways are active! Outgoing notifications will use your configured credentials."
                       : "Simulation Mode is active. Notifications will generate complete simulated audit logs without requiring API keys.");
    }

    private void saveSetting(String key, String value) {
        String sql = "INSERT OR REPLACE INTO app_settings (key, value) VALUES (?, ?);";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, key.toLowerCase());
            stmt.setString(2, value != null ? value.trim() : "");
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
