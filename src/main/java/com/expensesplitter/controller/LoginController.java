package com.expensesplitter.controller;

import com.expensesplitter.Main;
import com.expensesplitter.model.PendingOtp;
import com.expensesplitter.service.AuthService;
import com.expensesplitter.service.OtpService;
import com.expensesplitter.util.SessionManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

/**
 * Login controller – provides two flows:
 *  1. Classic password login (for existing seeded demo users)
 *  2. OTP-based passwordless login (new primary flow)
 */
public class LoginController {

    // OTP login panel
    @FXML private TextField emailOtpField;
    @FXML private Button sendOtpButton;
    @FXML private Label otpStatusLabel;

    // Classic password login panel
    @FXML private TextField usernameField;
    @FXML private javafx.scene.control.PasswordField passwordField;

    // Shared
    @FXML private Label errorLabel;

    private final OtpService otpService = new OtpService();
    private final AuthService authService = new AuthService();

    @FXML
    public void initialize() {
        errorLabel.setVisible(false);
        if (otpStatusLabel != null) otpStatusLabel.setVisible(false);
    }

    // ─── OTP Login Flow ───────────────────────────────────────────────────────

    @FXML
    public void handleSendLoginOtp() {
        clearMessages();
        String email = emailOtpField != null ? emailOtpField.getText().trim() : "";
        if (email.isEmpty()) { showError("Please enter your email address."); return; }

        sendOtpButton.setDisable(true);
        sendOtpButton.setText("Sending…");
        showOtpStatus("Sending login OTP to " + email + "…");

        new Thread(() -> {
            try {
                otpService.initiateLogin(email);
                Platform.runLater(() -> {
                    sendOtpButton.setDisable(false);
                    sendOtpButton.setText("Send Login OTP →");
                    Main.showOtpView(email, null, PendingOtp.OtpPurpose.LOGIN);
                });
            } catch (IllegalArgumentException e) {
                Platform.runLater(() -> {
                    sendOtpButton.setDisable(false);
                    sendOtpButton.setText("Send Login OTP →");
                    showError(e.getMessage());
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    sendOtpButton.setDisable(false);
                    sendOtpButton.setText("Send Login OTP →");
                    showError("Error: " + e.getMessage());
                });
            }
        }, "login-otp-thread").start();
    }

    // ─── Classic Password Login (for demo/seeded accounts) ───────────────────

    @FXML
    public void handleLogin() {
        clearMessages();
        String username = usernameField != null ? usernameField.getText().trim() : "";
        String password = passwordField != null ? passwordField.getText() : "";

        if (username.isEmpty() || password.isEmpty()) {
            showError("Please enter both username/email and password.");
            return;
        }

        try {
            boolean success = authService.login(username, password);
            if (success) {
                Main.showMainLayout();
            } else {
                showError("Invalid username or password. Please try again.");
            }
        } catch (Exception e) {
            showError("Login error: " + e.getMessage());
        }
    }

    @FXML
    public void handleGoToRegister() {
        Main.showRegisterView();
    }

    @FXML
    public void handleFillDemoRahul() {
        if (usernameField != null) usernameField.setText("rahul");
        if (passwordField != null) passwordField.setText("rahul123");
    }

    @FXML
    public void handleFillDemoAman() {
        if (usernameField != null) usernameField.setText("aman");
        if (passwordField != null) passwordField.setText("aman123");
    }

    // ─── UI helpers ───────────────────────────────────────────────────────────

    private void showError(String msg) {
        errorLabel.setText("⚠ " + msg);
        errorLabel.setVisible(true);
    }

    private void showOtpStatus(String msg) {
        if (otpStatusLabel != null) {
            otpStatusLabel.setText("⏳ " + msg);
            otpStatusLabel.setVisible(true);
        }
    }

    private void clearMessages() {
        errorLabel.setVisible(false);
        if (otpStatusLabel != null) otpStatusLabel.setVisible(false);
    }
}
