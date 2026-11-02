package com.expensesplitter.model;

public class Balance {
    private int userId;
    private String userName;
    private String userFullName;
    private long totalPaidPaise;
    private long totalSharePaise;
    private long netBalancePaise; // positive = to receive (gets back), negative = owes

    public Balance() {}

    public Balance(int userId, String userName, String userFullName, long totalPaidPaise, long totalSharePaise) {
        this.userId = userId;
        this.userName = userName;
        this.userFullName = userFullName;
        this.totalPaidPaise = totalPaidPaise;
        this.totalSharePaise = totalSharePaise;
        this.netBalancePaise = totalPaidPaise - totalSharePaise;
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

    public long getTotalPaidPaise() {
        return totalPaidPaise;
    }

    public void setTotalPaidPaise(long totalPaidPaise) {
        this.totalPaidPaise = totalPaidPaise;
        this.netBalancePaise = this.totalPaidPaise - this.totalSharePaise;
    }

    public long getTotalSharePaise() {
        return totalSharePaise;
    }

    public void setTotalSharePaise(long totalSharePaise) {
        this.totalSharePaise = totalSharePaise;
        this.netBalancePaise = this.totalPaidPaise - this.totalSharePaise;
    }

    public long getNetBalancePaise() {
        return netBalancePaise;
    }

    public void setNetBalancePaise(long netBalancePaise) {
        this.netBalancePaise = netBalancePaise;
    }
}

