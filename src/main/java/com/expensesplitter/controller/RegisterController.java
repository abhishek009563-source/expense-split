package com.expensesplitter.controller;

import com.expensesplitter.Main;
import com.expensesplitter.service.AuthService;
import com.expensesplitter.util.AlertHelper;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class RegisterController {

    @FXML private TextField fullNameField;
    @FXML private TextField usernameField;
    @FXML private TextField emailField;
    @FXML private TextField phoneField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Label errorLabel;

    private final AuthService authService = new AuthService();

    @FXML
    public void initialize() {
        errorLabel.setVisible(false);
    }

    @FXML
    public void handleRegister() {
        String fullName = fullNameField.getText().trim();
        String username = usernameField.getText().trim();
        String email = emailField.getText().trim();
        String phone = phoneField.getText().trim();
        String password = passwordField.getText();
        String confirm = confirmPasswordField.getText();

        if (fullName.isEmpty() || username.isEmpty() || email.isEmpty() || password.isEmpty()) {
            showError("Please fill in all required fields.");
            return;
        }

        if (!password.equals(confirm)) {
            showError("Passwords do not match. Please verify.");
            return;
        }

        try {
            boolean created = authService.register(username, fullName, email, password, phone);
            if (created) {
                AlertHelper.showInformation("Account Created", "Your account @" + username + " has been registered successfully! Please log in.");
                Main.showLoginView();
            } else {
                showError("Unable to create account. Please check your information.");
            }
        } catch (IllegalArgumentException e) {
            showError(e.getMessage());
        } catch (Exception e) {
            showError("Registration error: " + e.getMessage());
        }
    }

    @FXML
    public void handleBackToLogin() {
        Main.showLoginView();
    }

    private void showError(String msg) {
        errorLabel.setText(msg);
        errorLabel.setVisible(true);
    }
}

