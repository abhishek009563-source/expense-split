package com.expensesplitter.controller;

import com.expensesplitter.Main;
import com.expensesplitter.service.AuthService;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;

    private final AuthService authService = new AuthService();

    @FXML
    public void initialize() {
        errorLabel.setVisible(false);
        // Pre-fill demo username for ease of evaluation
        usernameField.setText("rahul");
        passwordField.setText("rahul123");
    }

    @FXML
    public void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

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
        usernameField.setText("rahul");
        passwordField.setText("rahul123");
    }

    @FXML
    public void handleFillDemoAman() {
        usernameField.setText("aman");
        passwordField.setText("aman123");
    }

    private void showError(String msg) {
        errorLabel.setText(msg);
        errorLabel.setVisible(true);
    }
}
