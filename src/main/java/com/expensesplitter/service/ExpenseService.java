package com.expensesplitter.service;

import com.expensesplitter.dao.ExpenseDao;
import com.expensesplitter.dao.GroupDao;
import com.expensesplitter.dao.NotificationDao;
import com.expensesplitter.dao.UserDao;
import com.expensesplitter.engine.DebtSimplifier;
import com.expensesplitter.engine.SplitCalculator;
import com.expensesplitter.model.*;
import com.expensesplitter.util.CurrencyFormatter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ExpenseService {

    private final ExpenseDao expenseDao = new ExpenseDao();
    private final GroupDao groupDao = new GroupDao();
    private final UserDao userDao = new UserDao();
    private final NotificationDao notificationDao = new NotificationDao();
    private final NotificationDispatcher notificationDispatcher = new NotificationDispatcher();

    public boolean addExpense(int groupId, int paidByUserId, String title, String description,
                              ExpenseCategory category, SplitMethod splitMethod, long amountPaise,
                              LocalDate date, List<Integer> participantIds, List<String> participantNames,
                              List<Long> exactShares, List<Double> percentages) {

        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Expense title is required.");
        }
        if (amountPaise <= 0) {
            throw new IllegalArgumentException("Expense amount must be greater than zero.");
        }
        if (participantIds == null || participantIds.isEmpty()) {
            throw new IllegalArgumentException("At least one participant must be selected.");
        }

        List<ExpenseParticipant> calculatedParticipants;
        if (splitMethod == SplitMethod.EQUAL) {
            calculatedParticipants = SplitCalculator.calculateEqualSplit(amountPaise, participantIds, participantNames);
        } else if (splitMethod == SplitMethod.EXACT) {
            calculatedParticipants = SplitCalculator.calculateExactSplit(amountPaise, participantIds, participantNames, exactShares);
        } else if (splitMethod == SplitMethod.PERCENTAGE) {
            calculatedParticipants = SplitCalculator.calculatePercentageSplit(amountPaise, participantIds, participantNames, percentages);
        } else {
            throw new IllegalArgumentException("Unsupported split method: " + splitMethod);
        }

        Expense expense = new Expense();
        expense.setGroupId(groupId);
        expense.setPaidByUserId(paidByUserId);
        expense.setTitle(title.trim());
        expense.setDescription(description != null ? description.trim() : "");
        expense.setCategory(category != null ? category : ExpenseCategory.OTHER);
        expense.setSplitMethod(splitMethod);
        expense.setAmountPaise(amountPaise);
        expense.setExpenseDate(date != null ? date : LocalDate.now());

        boolean created = expenseDao.create(expense, calculatedParticipants);
        if (created) {
            Optional<Group> g = groupDao.findById(groupId);
            String groupName = g.map(Group::getName).orElse("Group");
            Optional<User> payer = userDao.findById(paidByUserId);
            String payerName = payer.map(User::getFullName).orElse("Someone");

            // Dispatch automatic multi-channel (In-App, Email, SMS) notifications asynchronously
            notificationDispatcher.dispatchExpenseNotificationsAsync(expense, calculatedParticipants, groupName, payerName);
        }
        return created;
    }

    public boolean updateExpense(Expense expense, List<ExpenseParticipant> participants) {
        return expenseDao.update(expense, participants);
    }

    public boolean deleteExpense(int expenseId) {
        return expenseDao.delete(expenseId);
    }

    public Optional<Expense> getExpenseById(int id) {
        return expenseDao.findById(id);
    }

    public List<Expense> getGroupExpenses(int groupId) {
        return expenseDao.findByGroupId(groupId);
    }

    public List<Expense> getUserExpenses(int userId) {
        return expenseDao.findByUserId(userId);
    }

    public List<Expense> getRecentExpenses(int userId, int limit) {
        return expenseDao.findRecentByUserId(userId, limit);
    }

    public List<Balance> getGroupBalances(int groupId) {
        return expenseDao.getBalancesForGroup(groupId);
    }

    public List<SimplifiedDebt> getSimplifiedDebtsForGroup(int groupId) {
        List<Balance> balances = expenseDao.getBalancesForGroup(groupId);
        return DebtSimplifier.simplifyDebts(balances);
    }

    public List<SimplifiedDebt> getAllSimplifiedDebtsForUser(int userId) {
        List<Group> userGroups = groupDao.findGroupsByUserId(userId);
        List<SimplifiedDebt> userDebts = new ArrayList<>();

        for (Group g : userGroups) {
            List<SimplifiedDebt> groupDebts = getSimplifiedDebtsForGroup(g.getId());
            for (SimplifiedDebt d : groupDebts) {
                if (d.getFromUserId() == userId || d.getToUserId() == userId) {
                    userDebts.add(d);
                }
            }
        }
        return userDebts;
    }

    public long[] getUserOverview(int userId) {
        return expenseDao.getUserFinancialOverview(userId);
    }
}
