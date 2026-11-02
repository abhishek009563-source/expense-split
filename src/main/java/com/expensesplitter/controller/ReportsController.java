package com.expensesplitter.controller;

import com.expensesplitter.Main;
import com.expensesplitter.model.Expense;
import com.expensesplitter.model.Group;
import com.expensesplitter.model.Settlement;
import com.expensesplitter.model.User;
import com.expensesplitter.service.ExpenseService;
import com.expensesplitter.service.GroupService;
import com.expensesplitter.service.SettlementService;
import com.expensesplitter.util.AlertHelper;
import com.expensesplitter.util.CsvExporter;
import com.expensesplitter.util.CurrencyFormatter;
import com.expensesplitter.util.SessionManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.stage.FileChooser;

import java.io.File;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

public class ReportsController {

    @FXML private ComboBox<Group> groupComboBox;
    @FXML private DatePicker startDatePicker;
    @FXML private DatePicker endDatePicker;

    @FXML private Label totalSpentLabel;
    @FXML private Label totalPaidByUserLabel;
    @FXML private Label totalSettledLabel;
    @FXML private Label totalExpenseCountLabel;

    @FXML private PieChart categoryPieChart;
    @FXML private BarChart<String, Number> monthlySpendingChart;

    private final ExpenseService expenseService = new ExpenseService();
    private final GroupService groupService = new GroupService();
    private final SettlementService settlementService = new SettlementService();

    private List<Expense> currentFilteredExpenses = new ArrayList<>();
    private List<Settlement> currentFilteredSettlements = new ArrayList<>();

    @FXML
    public void initialize() {
        startDatePicker.setValue(LocalDate.now().minusMonths(6));
        endDatePicker.setValue(LocalDate.now().plusDays(1));

        setupGroupSelector();
        loadGroups();
        generateReport();
    }

    private void setupGroupSelector() {
        groupComboBox.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Group item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getName());
            }
        });
        groupComboBox.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Group item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "All Groups" : item.getName());
            }
        });
        groupComboBox.getSelectionModel().selectedItemProperty().addListener((o, ov, nv) -> generateReport());
        startDatePicker.valueProperty().addListener((o, ov, nv) -> generateReport());
        endDatePicker.valueProperty().addListener((o, ov, nv) -> generateReport());
    }

    private void loadGroups() {
        User user = SessionManager.getCurrentUser();
        if (user == null) return;
        List<Group> userGroups = groupService.getUserGroups(user.getId());

        Group allDummy = new Group();
        allDummy.setId(-1);
        allDummy.setName("All Groups Combined");

        ObservableList<Group> list = FXCollections.observableArrayList();
        list.add(allDummy);
        list.addAll(userGroups);
        groupComboBox.setItems(list);
        groupComboBox.getSelectionModel().selectFirst();
    }

    @FXML
    public void generateReport() {
        User user = SessionManager.getCurrentUser();
        if (user == null) return;

        LocalDate start = startDatePicker.getValue();
        LocalDate end = endDatePicker.getValue();
        Group selectedGroup = groupComboBox.getSelectionModel().getSelectedItem();

        List<Expense> allExpenses;
        if (selectedGroup != null && selectedGroup.getId() != -1) {
            allExpenses = expenseService.getGroupExpenses(selectedGroup.getId());
        } else {
            allExpenses = expenseService.getUserExpenses(user.getId());
        }

        // Filter by Date
        currentFilteredExpenses = allExpenses.stream().filter(e -> {
            if (e.getExpenseDate() == null) return true;
            if (start != null && e.getExpenseDate().isBefore(start)) return false;
            if (end != null && e.getExpenseDate().isAfter(end)) return false;
            return true;
        }).collect(Collectors.toList());

        // Settlements
        List<Settlement> allSettlements;
        if (selectedGroup != null && selectedGroup.getId() != -1) {
            allSettlements = settlementService.getGroupSettlements(selectedGroup.getId());
        } else {
            allSettlements = settlementService.getUserSettlements(user.getId());
        }

        currentFilteredSettlements = allSettlements.stream().filter(s -> {
            if (s.getSettlementDate() == null) return true;
            if (start != null && s.getSettlementDate().isBefore(start)) return false;
            if (end != null && s.getSettlementDate().isAfter(end)) return false;
            return true;
        }).collect(Collectors.toList());

        // Calculate Metrics
        long totalSpent = 0;
        long totalPaidByUser = 0;
        for (Expense e : currentFilteredExpenses) {
            totalSpent += e.getAmountPaise();
            if (e.getPaidByUserId() == user.getId()) {
                totalPaidByUser += e.getAmountPaise();
            }
        }

        long totalSettled = 0;
        for (Settlement s : currentFilteredSettlements) {
            totalSettled += s.getAmountPaise();
        }

        totalSpentLabel.setText(CurrencyFormatter.format(totalSpent));
        totalPaidByUserLabel.setText(CurrencyFormatter.format(totalPaidByUser));
        totalSettledLabel.setText(CurrencyFormatter.format(totalSettled));
        totalExpenseCountLabel.setText(String.valueOf(currentFilteredExpenses.size()));

        // Update Charts
        updateCharts(currentFilteredExpenses);
    }

    private void updateCharts(List<Expense> expenses) {
        // Pie Chart: Category
        Map<String, Long> catMap = new HashMap<>();
        Map<String, Long> monthMap = new LinkedHashMap<>();

        for (Expense e : expenses) {
            String cat = e.getCategory() != null ? e.getCategory().getDisplayName() : "Other";
            catMap.put(cat, catMap.getOrDefault(cat, 0L) + e.getAmountPaise());

            if (e.getExpenseDate() != null) {
                String monthKey = e.getExpenseDate().getMonth().name().substring(0, 3) + " " + e.getExpenseDate().getYear();
                monthMap.put(monthKey, monthMap.getOrDefault(monthKey, 0L) + e.getAmountPaise());
            }
        }

        ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
        for (Map.Entry<String, Long> entry : catMap.entrySet()) {
            pieData.add(new PieChart.Data(entry.getKey(), entry.getValue() / 100.0));
        }
        categoryPieChart.setData(pieData);

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Spending Trend");
        for (Map.Entry<String, Long> entry : monthMap.entrySet()) {
            series.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue() / 100.0));
        }
        monthlySpendingChart.getData().clear();
        monthlySpendingChart.getData().add(series);
        monthlySpendingChart.setLegendVisible(false);
    }

    @FXML
    public void handleExportExpensesCsv() {
        if (currentFilteredExpenses.isEmpty()) {
            AlertHelper.showWarning("No Data", "There are no expenses to export for the selected filter.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Expenses CSV");
        fileChooser.setInitialFileName("expenses_report_" + LocalDate.now() + ".csv");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files (*.csv)", "*.csv"));

        File file = fileChooser.showSaveDialog(Main.getPrimaryStage());
        if (file != null) {
            try {
                CsvExporter.exportExpensesToCsv(file, currentFilteredExpenses);
                AlertHelper.showInformation("Export Successful", "Saved " + currentFilteredExpenses.size() + " expenses to:\n" + file.getAbsolutePath());
            } catch (Exception e) {
                AlertHelper.showError("Export Error", "Failed to write CSV file: " + e.getMessage());
            }
        }
    }

    @FXML
    public void handleExportSettlementsCsv() {
        if (currentFilteredSettlements.isEmpty()) {
            AlertHelper.showWarning("No Data", "There are no settlements to export for the selected filter.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Settlements CSV");
        fileChooser.setInitialFileName("settlements_report_" + LocalDate.now() + ".csv");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files (*.csv)", "*.csv"));

        File file = fileChooser.showSaveDialog(Main.getPrimaryStage());
        if (file != null) {
            try {
                CsvExporter.exportSettlementsToCsv(file, currentFilteredSettlements);
                AlertHelper.showInformation("Export Successful", "Saved " + currentFilteredSettlements.size() + " settlements to:\n" + file.getAbsolutePath());
            } catch (Exception e) {
                AlertHelper.showError("Export Error", "Failed to write CSV file: " + e.getMessage());
            }
        }
    }
}

