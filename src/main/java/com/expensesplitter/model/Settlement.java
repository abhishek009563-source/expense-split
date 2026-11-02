package com.expensesplitter.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class Settlement {
    private int id;
    private int groupId;
    private String groupName;
    private int payerUserId;
    private String payerName;
    private int receiverUserId;
    private String receiverName;
    private long amountPaise;
    private LocalDate settlementDate;
    private String notes;
    private LocalDateTime createdAt;

    public Settlement() {}

    public Settlement(int id, int groupId, int payerUserId, int receiverUserId, long amountPaise,
                      LocalDate settlementDate, String notes) {
        this.id = id;
        this.groupId = groupId;
        this.payerUserId = payerUserId;
        this.receiverUserId = receiverUserId;
        this.amountPaise = amountPaise;
        this.settlementDate = settlementDate;
        this.notes = notes;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getGroupId() {
        return groupId;
    }

    public void setGroupId(int groupId) {
        this.groupId = groupId;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public int getPayerUserId() {
        return payerUserId;
    }

    public void setPayerUserId(int payerUserId) {
        this.payerUserId = payerUserId;
    }

    public String getPayerName() {
        return payerName;
    }

    public String getPayerUserName() {
        return payerName;
    }

    public void setPayerName(String payerName) {
        this.payerName = payerName;
    }

    public int getReceiverUserId() {
        return receiverUserId;
    }

    public void setReceiverUserId(int receiverUserId) {
        this.receiverUserId = receiverUserId;
    }

    public String getReceiverName() {
        return receiverName;
    }

    public String getReceiverUserName() {
        return receiverName;
    }

    public void setReceiverName(String receiverName) {
        this.receiverName = receiverName;
    }

    public long getAmountPaise() {
        return amountPaise;
    }

    public void setAmountPaise(long amountPaise) {
        this.amountPaise = amountPaise;
    }

    public LocalDate getSettlementDate() {
        return settlementDate;
    }

    public void setSettlementDate(LocalDate settlementDate) {
        this.settlementDate = settlementDate;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
