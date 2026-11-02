package com.expensesplitter;

import com.expensesplitter.controller.OtpVerifyController;
import com.expensesplitter.database.DatabaseInitializer;
import com.expensesplitter.database.DatabaseManager;
import com.expensesplitter.model.PendingOtp;
import com.expensesplitter.util.CurrencyFormatter;
import com.expensesplitter.util.SessionManager;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class Main extends Application {

    private static Stage primaryStage;

    @Override
    public void start(Stage stage) {
        primaryStage = stage;

        // Initialize Database & Seed data
        DatabaseInitializer.initializeDatabase();
        loadSavedSettings();

        stage.setTitle("Expense Splitter - Smart Group Expense Management System");
        stage.setMinWidth(1080);
        stage.setMinHeight(700);

        showLoginView();
        stage.show();
    }

    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    public static void showLoginView() {
        loadRootView("/fxml/login.fxml", 1000, 680);
    }

    public static void showRegisterView() {
        loadRootView("/fxml/register.fxml", 1000, 700);
    }

    public static void showMainLayout() {
        loadRootView("/fxml/main_layout.fxml", 1250, 800);
    }

    /**
     * Navigates to the OTP verification screen.
     * Passes email, fullName, and purpose to the controller after loading.
     */
    public static void showOtpView(String email, String fullName, PendingOtp.OtpPurpose purpose) {
        try {
            FXMLLoader loader = new FXMLLoader(Main.class.getResource("/fxml/otp_verify.fxml"));
            Parent root = loader.load();
            OtpVerifyController ctrl = loader.getController();
            ctrl.initContext(email, fullName, purpose);
            Scene scene = new Scene(root, 920, 660);
            applyCurrentTheme(scene);
            primaryStage.setScene(scene);
            primaryStage.centerOnScreen();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void loadRootView(String fxmlPath, double width, double height) {
        try {
            FXMLLoader loader = new FXMLLoader(Main.class.getResource(fxmlPath));
            Parent root = loader.load();
            Scene scene = new Scene(root, width, height);
            applyCurrentTheme(scene);
            primaryStage.setScene(scene);
            primaryStage.centerOnScreen();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void applyCurrentTheme(Scene scene) {
        if (scene == null) return;
        scene.getStylesheets().clear();
        String theme = SessionManager.getActiveTheme();
        String cssFile = "/css/" + ("light".equalsIgnoreCase(theme) ? "light-theme.css" : "dark-theme.css");
        var cssUrl = Main.class.getResource(cssFile);
        if (cssUrl != null) {
            scene.getStylesheets().add(cssUrl.toExternalForm());
        }
    }

    public static void toggleTheme() {
        String current = SessionManager.getActiveTheme();
        SessionManager.setActiveTheme("light".equalsIgnoreCase(current) ? "dark" : "light");
        if (primaryStage != null && primaryStage.getScene() != null) {
            applyCurrentTheme(primaryStage.getScene());
        }
    }

    private void loadSavedSettings() {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT key, value FROM app_settings")) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String k = rs.getString("key");
                String v = rs.getString("value");
                if ("theme".equalsIgnoreCase(k)) {
                    SessionManager.setActiveTheme(v);
                } else if ("currency".equalsIgnoreCase(k)) {
                    CurrencyFormatter.setCurrencySymbol(v);
                }
            }
        } catch (Exception ignored) {
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}

