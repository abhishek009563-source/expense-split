package com.expensesplitter.controller;

import com.expensesplitter.model.Expense;
import com.expensesplitter.model.Group;
import com.expensesplitter.model.SimplifiedDebt;
import com.expensesplitter.model.User;
import com.expensesplitter.service.ExpenseService;
import com.expensesplitter.service.GroupService;
import com.expensesplitter.util.CurrencyFormatter;
import com.expensesplitter.util.SessionManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.time.format.DateTimeFormatter;
import java.util.*;

public class DashboardController {

    @FXML private Label welcomeHeadingLabel;
    @FXML private Label totalPaidLabel;
    @FXML private Label totalGroupSpendingLabel;
    @FXML private Label youOweLabel;
    @FXML private Label youAreOwedLabel;
    @FXML private Label activeGroupsCountLabel;

    @FXML private BarChart<String, Number> monthlySpendingChart;
    @FXML private PieChart categoryPieChart;

    @FXML private TableView<Expense> recentExpensesTable;
    @FXML private TableColumn<Expense, String> colDate;
    @FXML private TableColumn<Expense, String> colTitle;
    @FXML private TableColumn<Expense, String> colGroup;
    @FXML private TableColumn<Expense, String> colPayer;
    @FXML private TableColumn<Expense, String> colAmount;

    @FXML private TableView<SimplifiedDebt> pendingDebtsTable;
    @FXML private TableColumn<SimplifiedDebt, String> colFromUser;
    @FXML private TableColumn<SimplifiedDebt, String> colToUser;
    @FXML private TableColumn<SimplifiedDebt, String> colDebtAmount;

    private final ExpenseService expenseService = new ExpenseService();
    private final GroupService groupService = new GroupService();

    @FXML
    public void initialize() {
        User user = SessionManager.getCurrentUser();
        if (user == null) return;

        welcomeHeadingLabel.setText("Welcome back, " + user.getFullName() + "!");

        loadKpis(user.getId());
        loadCharts(user.getId());
        loadRecentExpenses(user.getId());
        loadPendingDebts(user.getId());
    }

    private void loadKpis(int userId) {
        long[] overview = expenseService.getUserOverview(userId);
        // [0] = totalPaid, [1] = totalShare, [2] = youAreOwed, [3] = youOwe
        totalPaidLabel.setText(CurrencyFormatter.format(overview[0]));
        youAreOwedLabel.setText(CurrencyFormatter.format(overview[2]));
        youOweLabel.setText(CurrencyFormatter.format(overview[3]));

        List<Group> userGroups = groupService.getUserGroups(userId);
        activeGroupsCountLabel.setText(userGroups.size() + " active groups");

        long totalGroupSpending = 0;
        for (Group g : userGroups) {
            List<Expense> gExpenses = expenseService.getGroupExpenses(g.getId());
            for (Expense exp : gExpenses) {
                totalGroupSpending += exp.getAmountPaise();
            }
        }
        totalGroupSpendingLabel.setText(CurrencyFormatter.format(totalGroupSpending));
    }

    private void loadCharts(int userId) {
        List<Expense> expenses = expenseService.getUserExpenses(userId);

        // 1. Category Pie Chart
        Map<String, Long> categoryMap = new HashMap<>();
        // 2. Monthly Bar Chart
        Map<String, Long> monthlyMap = new LinkedHashMap<>();

        for (Expense exp : expenses) {
            String catName = exp.getCategory() != null ? exp.getCategory().getDisplayName() : "Other";
            categoryMap.put(catName, categoryMap.getOrDefault(catName, 0L) + exp.getAmountPaise());

            if (exp.getExpenseDate() != null) {
                String monthKey = exp.getExpenseDate().getMonth().name().substring(0, 3) + " " + exp.getExpenseDate().getYear();
                monthlyMap.put(monthKey, monthlyMap.getOrDefault(monthKey, 0L) + exp.getAmountPaise());
            }
        }

        // Populate Pie Chart
        ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
        for (Map.Entry<String, Long> entry : categoryMap.entrySet()) {
            pieData.add(new PieChart.Data(entry.getKey() + " (" + CurrencyFormatter.format(entry.getValue()) + ")", entry.getValue() / 100.0));
        }
        categoryPieChart.setData(pieData);
        categoryPieChart.setLegendVisible(false);

        // Populate Bar Chart
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Monthly Spending");
        for (Map.Entry<String, Long> entry : monthlyMap.entrySet()) {
            series.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue() / 100.0));
        }
        monthlySpendingChart.getData().clear();
        monthlySpendingChart.getData().add(series);
        monthlySpendingChart.setLegendVisible(false);
    }

    private void loadRecentExpenses(int userId) {
        colDate.setCellValueFactory(cell -> new SimpleStringProperty(
                cell.getValue().getExpenseDate() != null ? cell.getValue().getExpenseDate().format(DateTimeFormatter.ofPattern("MMM dd, yyyy")) : ""
        ));
        colTitle.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getTitle()));
        colGroup.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getGroupName()));
        colPayer.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getPaidByUserName()));
        colAmount.setCellValueFactory(cell -> new SimpleStringProperty(CurrencyFormatter.format(cell.getValue().getAmountPaise())));

        List<Expense> recent = expenseService.getRecentExpenses(userId, 8);
        recentExpensesTable.setItems(FXCollections.observableArrayList(recent));
    }

    private void loadPendingDebts(int userId) {
        colFromUser.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getFromUserName()));
        colToUser.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getToUserName()));
        colDebtAmount.setCellValueFactory(cell -> new SimpleStringProperty(CurrencyFormatter.format(cell.getValue().getAmountPaise())));

        List<SimplifiedDebt> debts = expenseService.getAllSimplifiedDebtsForUser(userId);
        pendingDebtsTable.setItems(FXCollections.observableArrayList(debts));
    }

    @FXML
    public void handleQuickAddExpense() {
        if (MainLayoutController.getInstance() != null) {
            MainLayoutController.getInstance().showAddExpense();
        }
    }

    @FXML
    public void handleQuickSettleUp() {
        if (MainLayoutController.getInstance() != null) {
            MainLayoutController.getInstance().showSettlements();
        }
    }

    @FXML
    public void handleQuickViewGroups() {
        if (MainLayoutController.getInstance() != null) {
            MainLayoutController.getInstance().showGroups();
        }
    }
}

