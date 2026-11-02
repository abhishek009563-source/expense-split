package com.expensesplitter.controller;

import com.expensesplitter.Main;
import com.expensesplitter.model.User;
import com.expensesplitter.service.NotificationService;
import com.expensesplitter.util.AlertHelper;
import com.expensesplitter.util.SessionManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;

import java.io.IOException;

public class MainLayoutController {

    private static MainLayoutController instance;

    @FXML private StackPane contentArea;
    @FXML private Label userFullNameLabel;
    @FXML private Label usernameLabel;
    @FXML private Label notificationBadgeLabel;

    @FXML private Button navDashboard;
    @FXML private Button navGroups;
    @FXML private Button navAddExpense;
    @FXML private Button navExpenses;
    @FXML private Button navBalances;
    @FXML private Button navSettlements;
    @FXML private Button navReports;
    @FXML private Button navNotifications;
    @FXML private Button navSettings;

    private final NotificationService notificationService = new NotificationService();
    private Button activeButton;

    public static MainLayoutController getInstance() {
        return instance;
    }

    @FXML
    public void initialize() {
        instance = this;
        User user = SessionManager.getCurrentUser();
        if (user != null) {
            userFullNameLabel.setText(user.getFullName());
            usernameLabel.setText("@" + user.getUsername());
        }

        updateNotificationBadge();
        showDashboard();
    }

    public void updateNotificationBadge() {
        User user = SessionManager.getCurrentUser();
        if (user != null) {
            int unread = notificationService.getUnreadCount(user.getId());
            if (unread > 0) {
                notificationBadgeLabel.setText(String.valueOf(unread));
                notificationBadgeLabel.setVisible(true);
                notificationBadgeLabel.setManaged(true);
            } else {
                notificationBadgeLabel.setVisible(false);
                notificationBadgeLabel.setManaged(false);
            }
        }
    }

    private void setActiveNav(Button button) {
        if (activeButton != null) {
            activeButton.getStyleClass().remove("active");
        }
        activeButton = button;
        if (activeButton != null && !activeButton.getStyleClass().contains("active")) {
            activeButton.getStyleClass().add("active");
        }
    }

    public void loadContent(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Node node = loader.load();
            contentArea.getChildren().setAll(node);
        } catch (IOException e) {
            e.printStackTrace();
            AlertHelper.showError("Navigation Error", "Failed to load view: " + fxmlPath + "\n" + e.getMessage());
        }
    }

    @FXML
    public void showDashboard() {
        setActiveNav(navDashboard);
        loadContent("/fxml/dashboard.fxml");
    }

    @FXML
    public void showGroups() {
        setActiveNav(navGroups);
        loadContent("/fxml/groups.fxml");
    }

    @FXML
    public void showAddExpense() {
        setActiveNav(navAddExpense);
        loadContent("/fxml/add_expense.fxml");
    }

    @FXML
    public void showExpenses() {
        setActiveNav(navExpenses);
        loadContent("/fxml/expenses.fxml");
    }

    @FXML
    public void showBalances() {
        setActiveNav(navBalances);
        loadContent("/fxml/balances.fxml");
    }

    @FXML
    public void showSettlements() {
        setActiveNav(navSettlements);
        loadContent("/fxml/settlements.fxml");
    }

    @FXML
    public void showReports() {
        setActiveNav(navReports);
        loadContent("/fxml/reports.fxml");
    }

    @FXML
    public void showNotifications() {
        setActiveNav(navNotifications);
        loadContent("/fxml/notifications.fxml");
        updateNotificationBadge();
    }

    @FXML
    public void showSettings() {
        setActiveNav(navSettings);
        loadContent("/fxml/settings.fxml");
    }

    @FXML
    public void handleToggleTheme() {
        Main.toggleTheme();
    }

    @FXML
    public void handleLogout() {
        if (AlertHelper.showConfirmation("Confirm Logout", "Are you sure you want to log out of your account?")) {
            SessionManager.logout();
            Main.showLoginView();
        }
    }
}

