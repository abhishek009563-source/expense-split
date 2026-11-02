package com.expensesplitter.model;

import java.time.LocalDateTime;

public class Group {
    private int id;
    private String name;
    private String description;
    private int createdByUserId;
    private String createdByUserName;
    private LocalDateTime createdAt;
    private int memberCount;
    private long totalExpensePaise;

    public Group() {}

    public Group(int id, String name, String description, int createdByUserId) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.createdByUserId = createdByUserId;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getCreatedByUserId() {
        return createdByUserId;
    }

    public void setCreatedByUserId(int createdByUserId) {
        this.createdByUserId = createdByUserId;
    }

    public String getCreatedByUserName() {
        return createdByUserName;
    }

    public void setCreatedByUserName(String createdByUserName) {
        this.createdByUserName = createdByUserName;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public int getMemberCount() {
        return memberCount;
    }

    public void setMemberCount(int memberCount) {
        this.memberCount = memberCount;
    }

    public long getTotalExpensePaise() {
        return totalExpensePaise;
    }

    public void setTotalExpensePaise(long totalExpensePaise) {
        this.totalExpensePaise = totalExpensePaise;
    }

    @Override
    public String toString() {
        return name;
    }
}

