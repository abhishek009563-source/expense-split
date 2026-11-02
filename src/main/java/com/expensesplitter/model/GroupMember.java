package com.expensesplitter.model;

import java.time.LocalDateTime;

public class GroupMember {
    private int id;
    private int groupId;
    private int userId;
    private String userName;
    private String userFullName;
    private String userEmail;
    private String role; // "ADMIN", "MEMBER"
    private LocalDateTime joinedAt;

    public GroupMember() {}

    public GroupMember(int id, int groupId, int userId, String role) {
        this.id = id;
        this.groupId = groupId;
        this.userId = userId;
        this.role = role;
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

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public LocalDateTime getJoinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(LocalDateTime joinedAt) {
        this.joinedAt = joinedAt;
    }

    @Override
    public String toString() {
        return getUserFullName();
    }
}

