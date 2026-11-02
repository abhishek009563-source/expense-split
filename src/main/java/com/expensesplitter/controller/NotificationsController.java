package com.expensesplitter.controller;

import com.expensesplitter.model.Notification;
import com.expensesplitter.model.NotificationChannel;
import com.expensesplitter.model.NotificationStatus;
import com.expensesplitter.model.User;
import com.expensesplitter.service.NotificationService;
import com.expensesplitter.util.AlertHelper;
import com.expensesplitter.util.SessionManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

public class NotificationsController {

    @FXML private Label notificationCountLabel;
    @FXML private Label totalNotifLabel;
    @FXML private Label emailsSentLabel;
    @FXML private Label smsSentLabel;
    @FXML private Label failedNotifLabel;

    @FXML private ComboBox<String> channelFilterCombo;
    @FXML private ComboBox<String> statusFilterCombo;
    @FXML private TextField searchField;

    @FXML private TableView<Notification> notificationsTable;
    @FXML private TableColumn<Notification, String> colChannel;
    @FXML private TableColumn<Notification, String> colRecipient;
    @FXML private TableColumn<Notification, String> colContact;
    @FXML private TableColumn<Notification, String> colTitle;
    @FXML private TableColumn<Notification, String> colMessage;
    @FXML private TableColumn<Notification, String> colStatus;
    @FXML private TableColumn<Notification, String> colDate;

    private final NotificationService notificationService = new NotificationService();
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("MMM dd, HH:mm");

    @FXML
    public void initialize() {
        setupFilters();
        setupTable();
        loadNotifications();
    }

    private void setupFilters() {
        channelFilterCombo.setItems(FXCollections.observableArrayList("All Channels", "EMAIL", "SMS", "IN_APP"));
        channelFilterCombo.getSelectionModel().selectFirst();
        channelFilterCombo.getSelectionModel().selectedItemProperty().addListener((o, ov, nv) -> loadNotifications());

        statusFilterCombo.setItems(FXCollections.observableArrayList("All Statuses", "SENT", "FAILED", "PENDING"));
        statusFilterCombo.getSelectionModel().selectFirst();
        statusFilterCombo.getSelectionModel().selectedItemProperty().addListener((o, ov, nv) -> loadNotifications());

        searchField.textProperty().addListener((o, ov, nv) -> loadNotifications());
    }

    private void setupTable() {
        colChannel.setCellValueFactory(cell -> {
            NotificationChannel ch = cell.getValue().getChannel();
            return new SimpleStringProperty(ch != null ? ch.getIcon() + " " + ch.getDisplayName() : "🔔 In-App");
        });

        colRecipient.setCellValueFactory(cell -> new SimpleStringProperty(
                cell.getValue().getRecipientName() != null ? cell.getValue().getRecipientName() : "Participant"
        ));

        colContact.setCellValueFactory(cell -> new SimpleStringProperty(
                cell.getValue().getRecipientContact() != null ? cell.getValue().getRecipientContact() : "-"
        ));

        colTitle.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getTitle()));
        colMessage.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getMessage()));

        colStatus.setCellValueFactory(cell -> {
            NotificationStatus st = cell.getValue().getStatus();
            String err = cell.getValue().getErrorMessage();
            if (st == NotificationStatus.FAILED && err != null && !err.isBlank()) {
                return new SimpleStringProperty("❌ Failed (" + err + ")");
            } else if (st == NotificationStatus.SENT) {
                return new SimpleStringProperty("✓ Sent");
            } else {
                return new SimpleStringProperty("⏳ " + (st != null ? st.getDisplayName() : "Pending"));
            }
        });

        colDate.setCellValueFactory(cell -> {
            if (cell.getValue().getSentAt() != null) {
                return new SimpleStringProperty(cell.getValue().getSentAt().format(TIME_FORMAT));
            } else if (cell.getValue().getCreatedAt() != null) {
                return new SimpleStringProperty(cell.getValue().getCreatedAt().format(TIME_FORMAT));
            }
            return new SimpleStringProperty("-");
        });
    }

    public void loadNotifications() {
        User user = SessionManager.getCurrentUser();
        if (user == null) return;

        String selectedChannel = channelFilterCombo.getSelectionModel().getSelectedItem();
        String selectedStatus = statusFilterCombo.getSelectionModel().getSelectedItem();
        String search = searchField.getText();

        List<Notification> list = notificationService.getFilteredNotifications(user.getId(), selectedChannel, selectedStatus, search);
        notificationsTable.setItems(FXCollections.observableArrayList(list));

        // Delivery Stats
        int[] stats = notificationService.getDeliveryStats(user.getId());
        totalNotifLabel.setText(String.valueOf(stats[0]));
        emailsSentLabel.setText(String.valueOf(stats[1]));
        smsSentLabel.setText(String.valueOf(stats[2]));
        failedNotifLabel.setText(String.valueOf(stats[3]));

        int unread = notificationService.getUnreadCount(user.getId());
        notificationCountLabel.setText(unread + " unread notification" + (unread == 1 ? "" : "s"));

        if (MainLayoutController.getInstance() != null) {
            MainLayoutController.getInstance().updateNotificationBadge();
        }
    }

    @FXML
    public void handleRetrySelected() {
        Notification selected = notificationsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertHelper.showWarning("Select Notification", "Please select a notification to retry.");
            return;
        }

        boolean success = notificationService.retryNotification(selected.getId());
        if (success) {
            AlertHelper.showInformation("Delivery Success", "Notification delivery was retried successfully and marked as SENT!");
        } else {
            AlertHelper.showError("Delivery Failed", "Retry attempt failed. Please check network or gateway credentials.");
        }
        loadNotifications();
    }

    @FXML
    public void handleMarkAllRead() {
        User user = SessionManager.getCurrentUser();
        if (user == null) return;

        notificationService.markAllAsRead(user.getId());
        loadNotifications();
    }

    @FXML
    public void handleSendTestNotification() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Send Test Notification");
        dialog.setHeaderText("Test Email or SMS Gateway Delivery");

        ButtonType sendBtnType = new ButtonType("Send Test", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(sendBtnType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setStyle("-fx-padding: 20px;");

        ComboBox<NotificationChannel> channelCombo = new ComboBox<>(FXCollections.observableArrayList(NotificationChannel.EMAIL, NotificationChannel.SMS));
        channelCombo.getSelectionModel().selectFirst();

        TextField targetField = new TextField();
        targetField.setPromptText("e.g. user@example.com or +919876543210");
        targetField.setPrefWidth(260);

        TextField msgField = new TextField("Hello! This is a test notification from Expense Splitter.");

        grid.add(new Label("Channel:"), 0, 0);
        grid.add(channelCombo, 1, 0);
        grid.add(new Label("Recipient (Email/Phone):"), 0, 1);
        grid.add(targetField, 1, 1);
        grid.add(new Label("Test Message:"), 0, 2);
        grid.add(msgField, 1, 2);

        dialog.getDialogPane().setContent(grid);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == sendBtnType) {
            String target = targetField.getText().trim();
            String msg = msgField.getText().trim();
            NotificationChannel ch = channelCombo.getSelectionModel().getSelectedItem();

            if (target.isEmpty() || msg.isEmpty()) {
                AlertHelper.showWarning("Validation", "Please enter both recipient contact and message.");
                return;
            }

            User user = SessionManager.getCurrentUser();
            int uid = user != null ? user.getId() : 1;

            boolean sent = notificationService.sendTestNotification(uid, ch, target, msg);
            if (sent) {
                AlertHelper.showInformation("Test Sent", ch.getDisplayName() + " test dispatch completed successfully!");
            } else {
                AlertHelper.showWarning("Test Result", ch.getDisplayName() + " test delivery reported failure or simulation notice. Check notification history.");
            }
            loadNotifications();
        }
    }
}
