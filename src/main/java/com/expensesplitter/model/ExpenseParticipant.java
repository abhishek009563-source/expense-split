package com.expensesplitter.model;

public class ExpenseParticipant {
    private int id;
    private int expenseId;
    private int userId;
    private String userName;
    private String userFullName;
    private long shareAmountPaise;
    private double percentage;

    public ExpenseParticipant() {}

    public ExpenseParticipant(int userId, String userName, long shareAmountPaise) {
        this.userId = userId;
        this.userName = userName;
        this.shareAmountPaise = shareAmountPaise;
    }

    public ExpenseParticipant(int userId, String userName, long shareAmountPaise, double percentage) {
        this.userId = userId;
        this.userName = userName;
        this.shareAmountPaise = shareAmountPaise;
        this.percentage = percentage;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getExpenseId() {
        return expenseId;
    }

    public void setExpenseId(int expenseId) {
        this.expenseId = expenseId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserFullName() {
        return userFullName != null && !userFullName.isBlank() ? userFullName : userName;
    }

    public void setUserFullName(String userFullName) {
        this.userFullName = userFullName;
    }

    public long getShareAmountPaise() {
        return shareAmountPaise;
    }

    public void setShareAmountPaise(long shareAmountPaise) {
        this.shareAmountPaise = shareAmountPaise;
    }

    public double getPercentage() {
        return percentage;
    }

    public void setPercentage(double percentage) {
        this.percentage = percentage;
    }
}

