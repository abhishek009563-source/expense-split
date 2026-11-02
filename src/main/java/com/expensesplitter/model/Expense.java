package com.expensesplitter.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Expense {
    private int id;
    private int groupId;
    private String groupName;
    private int paidByUserId;
    private String paidByUserName;
    private String title;
    private String description;
    private ExpenseCategory category;
    private SplitMethod splitMethod;
    private long amountPaise;
    private LocalDate expenseDate;
    private LocalDateTime createdAt;
    private List<ExpenseParticipant> participants = new ArrayList<>();

    public Expense() {}

    public Expense(int id, int groupId, int paidByUserId, String title, ExpenseCategory category,
                   SplitMethod splitMethod, long amountPaise, LocalDate expenseDate) {
        this.id = id;
        this.groupId = groupId;
        this.paidByUserId = paidByUserId;
        this.title = title;
        this.category = category;
        this.splitMethod = splitMethod;
        this.amountPaise = amountPaise;
        this.expenseDate = expenseDate;
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

    public int getPaidByUserId() {
        return paidByUserId;
    }

    public void setPaidByUserId(int paidByUserId) {
        this.paidByUserId = paidByUserId;
    }

    public String getPaidByUserName() {
        return paidByUserName;
    }

    public void setPaidByUserName(String paidByUserName) {
        this.paidByUserName = paidByUserName;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ExpenseCategory getCategory() {
        return category;
    }

    public void setCategory(ExpenseCategory category) {
        this.category = category;
    }

    public SplitMethod getSplitMethod() {
        return splitMethod;
    }

    public void setSplitMethod(SplitMethod splitMethod) {
        this.splitMethod = splitMethod;
    }

    public long getAmountPaise() {
        return amountPaise;
    }

    public void setAmountPaise(long amountPaise) {
        this.amountPaise = amountPaise;
    }

    public LocalDate getExpenseDate() {
        return expenseDate;
    }

    public void setExpenseDate(LocalDate expenseDate) {
        this.expenseDate = expenseDate;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public List<ExpenseParticipant> getParticipants() {
        return participants;
    }

    public void setParticipants(List<ExpenseParticipant> participants) {
        this.participants = participants;
    }

    @Override
    public String toString() {
        return title;
    }
}

