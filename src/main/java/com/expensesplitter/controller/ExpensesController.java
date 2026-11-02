package com.expensesplitter.controller;

import com.expensesplitter.dao.ExpenseDao;
import com.expensesplitter.model.Expense;
import com.expensesplitter.model.ExpenseCategory;
import com.expensesplitter.model.ExpenseParticipant;
import com.expensesplitter.model.Group;
import com.expensesplitter.model.User;
import com.expensesplitter.service.ExpenseService;
import com.expensesplitter.service.GroupService;
import com.expensesplitter.util.AlertHelper;
import com.expensesplitter.util.CurrencyFormatter;
import com.expensesplitter.util.SessionManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

public class ExpensesController {

    @FXML private TextField searchField;
    @FXML private ComboBox<Group> groupFilterComboBox;
    @FXML private ComboBox<String> categoryFilterComboBox;

    @FXML private TableView<Expense> expensesTable;
    @FXML private TableColumn<Expense, String> colDate;
    @FXML private TableColumn<Expense, String> colTitle;
    @FXML private TableColumn<Expense, String> colGroup;
    @FXML private TableColumn<Expense, String> colCategory;
    @FXML private TableColumn<Expense, String> colPayer;
    @FXML private TableColumn<Expense, String> colSplitMethod;
    @FXML private TableColumn<Expense, String> colAmount;

    // Details Modal / Panel
    @FXML private VBox expenseDetailPanel;
    @FXML private Label detailTitleLabel;
    @FXML private Label detailGroupAndDateLabel;
    @FXML private Label detailTotalAmountLabel;
    @FXML private Label detailPayerLabel;
    @FXML private Label detailSplitMethodLabel;
    @FXML private Label detailDescriptionLabel;
    @FXML private ListView<String> participantsListView;

    private final ExpenseService expenseService = new ExpenseService();
    private final GroupService groupService = new GroupService();
    private final ExpenseDao expenseDao = new ExpenseDao();

    private List<Expense> allUserExpenses = List.of();

    @FXML
    public void initialize() {
        expenseDetailPanel.setVisible(false);
        expenseDetailPanel.setManaged(false);

        setupFilters();
        setupTable();
        loadExpenses();
    }

    private void setupFilters() {
        User user = SessionManager.getCurrentUser();
        if (user == null) return;

        List<Group> userGroups = groupService.getUserGroups(user.getId());
        Group allGroupsDummy = new Group();
        allGroupsDummy.setId(-1);
        allGroupsDummy.setName("All Groups");

        ObservableList<Group> gOptions = FXCollections.observableArrayList();
        gOptions.add(allGroupsDummy);
        gOptions.addAll(userGroups);
        groupFilterComboBox.setItems(gOptions);
        groupFilterComboBox.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Group item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getName());
            }
        });
        groupFilterComboBox.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Group item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "All Groups" : item.getName());
            }
        });
        groupFilterComboBox.getSelectionModel().selectFirst();
        groupFilterComboBox.getSelectionModel().selectedItemProperty().addListener((o, ov, nv) -> applyFilters());

        ObservableList<String> catOptions = FXCollections.observableArrayList("All Categories");
        for (ExpenseCategory c : ExpenseCategory.values()) {
            catOptions.add(c.getDisplayName());
        }
        categoryFilterComboBox.setItems(catOptions);
        categoryFilterComboBox.getSelectionModel().selectFirst();
        categoryFilterComboBox.getSelectionModel().selectedItemProperty().addListener((o, ov, nv) -> applyFilters());

        searchField.textProperty().addListener((o, ov, nv) -> applyFilters());
    }

    private void setupTable() {
        colDate.setCellValueFactory(cell -> new SimpleStringProperty(
                cell.getValue().getExpenseDate() != null ? cell.getValue().getExpenseDate().format(DateTimeFormatter.ofPattern("MMM dd, yyyy")) : ""
        ));
        colTitle.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getTitle()));
        colGroup.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getGroupName()));
        colCategory.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getCategory() != null ? cell.getValue().getCategory().getDisplayName() : "Other"));
        colPayer.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getPaidByUserName()));
        colSplitMethod.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getSplitMethod() != null ? cell.getValue().getSplitMethod().getDisplayName() : "Equal"));
        colAmount.setCellValueFactory(cell -> new SimpleStringProperty(CurrencyFormatter.format(cell.getValue().getAmountPaise())));

        expensesTable.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            if (newV != null) {
                showExpenseDetails(newV);
            }
        });
    }

    public void loadExpenses() {
        User user = SessionManager.getCurrentUser();
        if (user == null) return;
        allUserExpenses = expenseService.getUserExpenses(user.getId());
        applyFilters();
    }

    private void applyFilters() {
        String query = searchField.getText() != null ? searchField.getText().trim().toLowerCase() : "";
        Group selectedGroup = groupFilterComboBox.getSelectionModel().getSelectedItem();
        String selectedCategory = categoryFilterComboBox.getSelectionModel().getSelectedItem();

        List<Expense> filtered = allUserExpenses.stream().filter(exp -> {
            // Group filter
            if (selectedGroup != null && selectedGroup.getId() != -1 && exp.getGroupId() != selectedGroup.getId()) {
                return false;
            }
            // Category filter
            if (selectedCategory != null && !selectedCategory.equals("All Categories")) {
                if (exp.getCategory() == null || !exp.getCategory().getDisplayName().equalsIgnoreCase(selectedCategory)) {
                    return false;
                }
            }
            // Text search
            if (!query.isEmpty()) {
                boolean matchesTitle = exp.getTitle() != null && exp.getTitle().toLowerCase().contains(query);
                boolean matchesDesc = exp.getDescription() != null && exp.getDescription().toLowerCase().contains(query);
                boolean matchesPayer = exp.getPaidByUserName() != null && exp.getPaidByUserName().toLowerCase().contains(query);
                boolean matchesGroup = exp.getGroupName() != null && exp.getGroupName().toLowerCase().contains(query);
                return matchesTitle || matchesDesc || matchesPayer || matchesGroup;
            }
            return true;
        }).collect(Collectors.toList());

        expensesTable.setItems(FXCollections.observableArrayList(filtered));
        if (!filtered.isEmpty()) {
            expensesTable.getSelectionModel().selectFirst();
        } else {
            expenseDetailPanel.setVisible(false);
            expenseDetailPanel.setManaged(false);
        }
    }

    private void showExpenseDetails(Expense exp) {
        expenseDetailPanel.setVisible(true);
        expenseDetailPanel.setManaged(true);

        detailTitleLabel.setText(exp.getTitle());
        detailGroupAndDateLabel.setText(exp.getGroupName() + " • " + (exp.getExpenseDate() != null ? exp.getExpenseDate().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy")) : ""));
        detailTotalAmountLabel.setText(CurrencyFormatter.format(exp.getAmountPaise()));
        detailPayerLabel.setText("Paid by: " + exp.getPaidByUserName());
        detailSplitMethodLabel.setText("Split Method: " + (exp.getSplitMethod() != null ? exp.getSplitMethod().getDisplayName() : "Equal"));
        detailDescriptionLabel.setText(exp.getDescription() != null && !exp.getDescription().isEmpty() ? exp.getDescription() : "No notes attached.");

        List<ExpenseParticipant> parts = expenseDao.getParticipants(exp.getId());
        ObservableList<String> items = FXCollections.observableArrayList();
        for (ExpenseParticipant p : parts) {
            items.add(p.getUserFullName() + " (@" + p.getUserName() + ") : " + CurrencyFormatter.format(p.getShareAmountPaise()));
        }
        participantsListView.setItems(items);
    }

    @FXML
    public void handleDeleteSelectedExpense() {
        Expense selected = expensesTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertHelper.showWarning("Select Expense", "Please select an expense to delete.");
            return;
        }

        if (AlertHelper.showConfirmation("Delete Expense", "Are you sure you want to delete '" + selected.getTitle() + "' (" + CurrencyFormatter.format(selected.getAmountPaise()) + ")? Group balances will be automatically recalculated.")) {
            boolean deleted = expenseService.deleteExpense(selected.getId());
            if (deleted) {
                AlertHelper.showInformation("Deleted", "Expense was successfully deleted.");
                loadExpenses();
            } else {
                AlertHelper.showError("Error", "Could not delete expense.");
            }
        }
    }

    @FXML
    public void handleAddNewExpense() {
        if (MainLayoutController.getInstance() != null) {
            MainLayoutController.getInstance().showAddExpense();
        }
    }

    @FXML
    public void handleCloseDetailPanel() {
        expenseDetailPanel.setVisible(false);
        expenseDetailPanel.setManaged(false);
    }
}

