package com.expensesplitter.model;

import java.time.LocalDateTime;

public class Notification {
    private int id;
    private int userId;
    private String recipientName;
    private String recipientContact; // Email address or mobile number
    private NotificationChannel channel = NotificationChannel.IN_APP;
    private String title;
    private String message;
    private String type = "SYSTEM"; // "EXPENSE_SPLIT", "SETTLEMENT_CONFIRMATION", "SETTLEMENT_RECEIVED", "GROUP_INVITE", "REMINDER"
    private NotificationStatus status = NotificationStatus.PENDING;
    private String errorMessage;
    private int retryCount = 0;
    private String idempotencyKey;
    private boolean isRead;
    private LocalDateTime sentAt;
    private LocalDateTime createdAt;

    public Notification() {}

    public Notification(int id, int userId, String title, String message, String type, boolean isRead) {
        this.id = id;
        this.userId = userId;
        this.title = title;
        this.message = message;
        this.type = type;
        this.isRead = isRead;
        this.channel = NotificationChannel.IN_APP;
        this.status = NotificationStatus.SENT;
    }

    public Notification(int userId, String recipientName, String recipientContact,
                        NotificationChannel channel, String title, String message,
                        String type, String idempotencyKey) {
        this.userId = userId;
        this.recipientName = recipientName;
        this.recipientContact = recipientContact;
        this.channel = channel;
        this.title = title;
        this.message = message;
        this.type = type;
        this.idempotencyKey = idempotencyKey;
        this.status = NotificationStatus.PENDING;
        this.createdAt = LocalDateTime.now();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getRecipientName() {
        return recipientName;
    }

    public void setRecipientName(String recipientName) {
        this.recipientName = recipientName;
    }

    public String getRecipientContact() {
        return recipientContact;
    }

    public void setRecipientContact(String recipientContact) {
        this.recipientContact = recipientContact;
    }

    public NotificationChannel getChannel() {
        return channel;
    }

    public void setChannel(NotificationChannel channel) {
        this.channel = channel;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public NotificationStatus getStatus() {
        return status;
    }

    public void setStatus(NotificationStatus status) {
        this.status = status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean read) {
        isRead = read;
    }

    public LocalDateTime getSentAt() {
        return sentAt;
    }

    public void setSentAt(LocalDateTime sentAt) {
        this.sentAt = sentAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
