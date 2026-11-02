package com.expensesplitter.model;

public class SimplifiedDebt {
    private int fromUserId;
    private String fromUserName;
    private String fromUserFullName;
    private int toUserId;
    private String toUserName;
    private String toUserFullName;
    private long amountPaise;

    public SimplifiedDebt() {}

    public SimplifiedDebt(int fromUserId, String fromUserName, String fromUserFullName,
                          int toUserId, String toUserName, String toUserFullName, long amountPaise) {
        this.fromUserId = fromUserId;
        this.fromUserName = fromUserName;
        this.fromUserFullName = fromUserFullName;
        this.toUserId = toUserId;
        this.toUserName = toUserName;
        this.toUserFullName = toUserFullName;
        this.amountPaise = amountPaise;
    }

    public int getFromUserId() {
        return fromUserId;
    }

    public void setFromUserId(int fromUserId) {
        this.fromUserId = fromUserId;
    }

    public String getFromUserName() {
        return fromUserName;
    }

    public void setFromUserName(String fromUserName) {
        this.fromUserName = fromUserName;
    }

    public String getFromUserFullName() {
        return fromUserFullName != null && !fromUserFullName.isBlank() ? fromUserFullName : fromUserName;
    }

    public void setFromUserFullName(String fromUserFullName) {
        this.fromUserFullName = fromUserFullName;
    }

    public int getToUserId() {
        return toUserId;
    }

    public void setToUserId(int toUserId) {
        this.toUserId = toUserId;
    }

    public String getToUserName() {
        return toUserName;
    }

    public void setToUserName(String toUserName) {
        this.toUserName = toUserName;
    }

    public String getToUserFullName() {
        return toUserFullName != null && !toUserFullName.isBlank() ? toUserFullName : toUserName;
    }

    public void setToUserFullName(String toUserFullName) {
        this.toUserFullName = toUserFullName;
    }

    public long getAmountPaise() {
        return amountPaise;
    }

    public void setAmountPaise(long amountPaise) {
        this.amountPaise = amountPaise;
    }

    @Override
    public String toString() {
        return getFromUserFullName() + " owes " + getToUserFullName();
    }
}

