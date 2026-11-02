package com.expensesplitter.controller;

import com.expensesplitter.Main;
import com.expensesplitter.model.PendingOtp;
import com.expensesplitter.model.User;
import com.expensesplitter.service.OtpService;
import com.expensesplitter.util.SessionManager;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.util.Duration;

/**
 * Controller for the OTP verification screen.
 * Shared between registration and login flows.
 * Context is passed via {@link #initContext(String, String, PendingOtp.OtpPurpose)}.
 */
public class OtpVerifyController {

    @FXML private Label titleLabel;
    @FXML private Label subtitleLabel;
    @FXML private Label emailDisplayLabel;
    @FXML private TextField otpField;
    @FXML private Button verifyButton;
    @FXML private Button resendButton;
    @FXML private Label errorLabel;
    @FXML private Label successLabel;
    @FXML private Label timerLabel;
    @FXML private Label purposeHintLabel;

    private final OtpService otpService = new OtpService();

    private String email;
    private String fullName;
    private PendingOtp.OtpPurpose purpose;

    private Timeline resendCountdown;
    private int countdownSeconds = 60;

    @FXML
    public void initialize() {
        errorLabel.setVisible(false);
        successLabel.setVisible(false);
        timerLabel.setVisible(false);

        // Allow only digits in OTP field, max 6
        otpField.textProperty().addListener((obs, old, nv) -> {
            if (!nv.matches("\\d*")) otpField.setText(nv.replaceAll("[^\\d]", ""));
            if (otpField.getText().length() > 6) otpField.setText(otpField.getText().substring(0, 6));
            // Auto-submit when 6 digits entered
            if (otpField.getText().length() == 6) {
                handleVerify();
            }
        });
    }

    /**
     * Called after FXML load to inject context (email, name, purpose).
     */
    public void initContext(String email, String fullName, PendingOtp.OtpPurpose purpose) {
        this.email = email;
        this.fullName = fullName;
        this.purpose = purpose;

        emailDisplayLabel.setText(email);

        if (purpose == PendingOtp.OtpPurpose.REGISTER) {
            titleLabel.setText("Verify Your Email");
            subtitleLabel.setText("Enter the 6-digit code sent to create your account");
            purposeHintLabel.setText("👋 Welcome, " + fullName + "! Check your inbox.");
        } else {
            titleLabel.setText("Enter Login Code");
            subtitleLabel.setText("Enter the 6-digit code sent to sign in");
            purposeHintLabel.setText("🔐 Passwordless login — check your inbox.");
        }

        // Start 60s resend countdown immediately (OTP was just sent)
        startResendCountdown();
    }

    @FXML
    public void handleVerify() {
        clearMessages();
        String code = otpField.getText().trim();
        if (code.length() != 6) {
            showError("Please enter the full 6-digit OTP.");
            return;
        }

        verifyButton.setDisable(true);
        verifyButton.setText("Verifying…");

        // Run in background to avoid blocking UI thread during DB + email
        new Thread(() -> {
            try {
                OtpService.VerifyResult result = otpService.verifyOtp(email, code, purpose);
                Platform.runLater(() -> {
                    verifyButton.setDisable(false);
                    verifyButton.setText("Verify →");
                    if (result.success()) {
                        User user = result.createdOrLoggedInUser();
                        SessionManager.setCurrentUser(user);
                        showSuccess(purpose == PendingOtp.OtpPurpose.REGISTER
                                ? "✅ Account created! Redirecting…"
                                : "✅ Login successful! Redirecting…");
                        // Short delay then navigate
                        new Timeline(new KeyFrame(Duration.millis(900), e -> Main.showMainLayout())).play();
                    } else {
                        showError(result.errorMessage());
                        otpField.clear();
                        otpField.requestFocus();
                    }
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    verifyButton.setDisable(false);
                    verifyButton.setText("Verify →");
                    showError("Error: " + ex.getMessage());
                });
            }
        }, "otp-verify-thread").start();
    }

    @FXML
    public void handleResend() {
        if (resendCountdown != null) resendCountdown.stop();
        clearMessages();
        resendButton.setDisable(true);

        new Thread(() -> {
            try {
                otpService.resendOtp(email, purpose);
                Platform.runLater(() -> {
                    otpField.clear();
                    showSuccess("✉️ New OTP sent! Check your inbox.");
                    startResendCountdown();
                });
            } catch (IllegalArgumentException e) {
                Platform.runLater(() -> {
                    showError(e.getMessage());
                    resendButton.setDisable(false);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    showError("Failed to resend OTP: " + e.getMessage());
                    resendButton.setDisable(false);
                });
            }
        }, "otp-resend-thread").start();
    }

    @FXML
    public void handleBack() {
        if (purpose == PendingOtp.OtpPurpose.REGISTER) {
            Main.showRegisterView();
        } else {
            Main.showLoginView();
        }
    }

    // ─── Countdown timer ──────────────────────────────────────────────────────

    private void startResendCountdown() {
        countdownSeconds = 60;
        resendButton.setDisable(true);
        timerLabel.setVisible(true);
        updateTimerLabel();

        resendCountdown = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            countdownSeconds--;
            updateTimerLabel();
            if (countdownSeconds <= 0) {
                resendCountdown.stop();
                resendButton.setDisable(false);
                timerLabel.setVisible(false);
            }
        }));
        resendCountdown.setCycleCount(Timeline.INDEFINITE);
        resendCountdown.play();
    }

    private void updateTimerLabel() {
        timerLabel.setText("Resend available in " + countdownSeconds + "s");
    }

    // ─── UI helpers ───────────────────────────────────────────────────────────

    private void showError(String msg) {
        errorLabel.setText("⚠ " + msg);
        errorLabel.setVisible(true);
        successLabel.setVisible(false);
    }

    private void showSuccess(String msg) {
        successLabel.setText(msg);
        successLabel.setVisible(true);
        errorLabel.setVisible(false);
    }

    private void clearMessages() {
        errorLabel.setVisible(false);
        successLabel.setVisible(false);
    }
}
