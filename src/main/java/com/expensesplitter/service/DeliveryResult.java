package com.expensesplitter.service;

public class DeliveryResult {
    private final boolean success;
    private final String messageId;
    private final String errorMessage;
    private final boolean simulated;

    public DeliveryResult(boolean success, String messageId, String errorMessage, boolean simulated) {
        this.success = success;
        this.messageId = messageId;
        this.errorMessage = errorMessage;
        this.simulated = simulated;
    }

    public static DeliveryResult success(String messageId, boolean simulated) {
        return new DeliveryResult(true, messageId, null, simulated);
    }

    public static DeliveryResult failure(String errorMessage) {
        return new DeliveryResult(false, null, errorMessage, false);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessageId() {
        return messageId;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public boolean isSimulated() {
        return simulated;
    }
}

