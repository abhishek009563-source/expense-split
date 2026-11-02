package com.expensesplitter.controller;

import com.expensesplitter.Main;
import com.expensesplitter.model.PendingOtp;
import com.expensesplitter.service.OtpService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

/**
 * Registration controller – collects Full Name + Email,
 * initiates OTP flow, then navigates to OTP verification screen.
 */
public class RegisterController {

    @FXML private TextField fullNameField;
    @FXML private TextField emailField;
    @FXML private Button createAccountButton;
    @FXML private Label errorLabel;
    @FXML private Label statusLabel;

    private final OtpService otpService = new OtpService();

    @FXML
    public void initialize() {
        errorLabel.setVisible(false);
        statusLabel.setVisible(false);
    }

    @FXML
    public void handleRegister() {
        clearMessages();
        String fullName = fullNameField.getText().trim();
        String email    = emailField.getText().trim();

        if (fullName.isEmpty()) { showError("Please enter your full name."); return; }
        if (email.isEmpty())    { showError("Please enter your email address."); return; }

        createAccountButton.setDisable(true);
        createAccountButton.setText("Sending OTP…");
        showStatus("Sending verification code to " + email + "…");

        new Thread(() -> {
            try {
                otpService.initiateRegistration(fullName, email);
                Platform.runLater(() -> {
                    createAccountButton.setDisable(false);
                    createAccountButton.setText("Create Account →");
                    Main.showOtpView(email, fullName, PendingOtp.OtpPurpose.REGISTER);
                });
            } catch (IllegalArgumentException e) {
                Platform.runLater(() -> {
                    createAccountButton.setDisable(false);
                    createAccountButton.setText("Create Account →");
                    showError(e.getMessage());
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    createAccountButton.setDisable(false);
                    createAccountButton.setText("Create Account →");
                    showError("Error: " + e.getMessage());
                });
            }
        }, "register-otp-thread").start();
    }

    @FXML
    public void handleBackToLogin() {
        Main.showLoginView();
    }

    private void showError(String msg) {
        errorLabel.setText("⚠ " + msg);
        errorLabel.setVisible(true);
        statusLabel.setVisible(false);
    }

    private void showStatus(String msg) {
        statusLabel.setText("⏳ " + msg);
        statusLabel.setVisible(true);
        errorLabel.setVisible(false);
    }

    private void clearMessages() {
        errorLabel.setVisible(false);
        statusLabel.setVisible(false);
    }
}
