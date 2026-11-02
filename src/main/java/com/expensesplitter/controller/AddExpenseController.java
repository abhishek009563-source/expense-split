package com.expensesplitter.controller;

import com.expensesplitter.model.*;
import com.expensesplitter.service.ExpenseService;
import com.expensesplitter.service.GroupService;
import com.expensesplitter.util.AlertHelper;
import com.expensesplitter.util.CurrencyFormatter;
import com.expensesplitter.util.SessionManager;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.TextFieldTableCell;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class AddExpenseController {

    @FXML private TextField titleField;
    @FXML private TextField amountField;
    @FXML private ComboBox<ExpenseCategory> categoryComboBox;
    @FXML private ComboBox<Group> groupComboBox;
    @FXML private ComboBox<GroupMember> paidByComboBox;
    @FXML private DatePicker datePicker;
    @FXML private TextArea descriptionArea;

    @FXML private ToggleGroup splitMethodToggleGroup;
    @FXML private RadioButton splitEqualRadio;
    @FXML private RadioButton splitExactRadio;
    @FXML private RadioButton splitPercentageRadio;

    @FXML private TableView<ParticipantRow> participantsTable;
    @FXML private TableColumn<ParticipantRow, Boolean> colInclude;
    @FXML private TableColumn<ParticipantRow, String> colMember;
    @FXML private TableColumn<ParticipantRow, String> colCustomInput;
    @FXML private TableColumn<ParticipantRow, String> colCalculatedShare;

    @FXML private Label splitValidationSummaryLabel;

    private final GroupService groupService = new GroupService();
    private final ExpenseService expenseService = new ExpenseService();
    private final ObservableList<ParticipantRow> participantRows = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        categoryComboBox.setItems(FXCollections.observableArrayList(ExpenseCategory.values()));
        categoryComboBox.getSelectionModel().select(ExpenseCategory.FOOD);
        datePicker.setValue(LocalDate.now());

        setupGroupAndPayerSelectors();
        setupParticipantsTable();
        setupLiveCalculations();

        loadUserGroups();
    }

    private void setupGroupAndPayerSelectors() {
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
                setText(empty || item == null ? "Select Group..." : item.getName());
            }
        });

        groupComboBox.getSelectionModel().selectedItemProperty().addListener((obs, oldG, newG) -> {
            if (newG != null) {
                loadGroupMembers(newG.getId());
            }
        });

        paidByComboBox.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(GroupMember item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getUserFullName() + " (@" + item.getUserName() + ")");
            }
        });
        paidByComboBox.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(GroupMember item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "Select Payer..." : item.getUserFullName() + " (@" + item.getUserName() + ")");
            }
        });
    }

    private void setupParticipantsTable() {
        participantsTable.setEditable(true);

        colInclude.setCellValueFactory(cell -> cell.getValue().selectedProperty());
        colInclude.setCellFactory(CheckBoxTableCell.forTableColumn(colInclude));

        colMember.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getName()));

        colCustomInput.setCellValueFactory(cell -> cell.getValue().customInputProperty());
        colCustomInput.setCellFactory(TextFieldTableCell.forTableColumn());
        colCustomInput.setOnEditCommit(event -> {
            event.getRowValue().setCustomInput(event.getNewValue());
            recalculateShares();
        });

        colCalculatedShare.setCellValueFactory(cell -> cell.getValue().calculatedShareTextProperty());
        participantsTable.setItems(participantRows);
    }

    private void setupLiveCalculations() {
        amountField.textProperty().addListener((obs, oldVal, newVal) -> recalculateShares());
        splitEqualRadio.selectedProperty().addListener((obs, o, n) -> {
            updateColumnHeadings();
            recalculateShares();
        });
        splitExactRadio.selectedProperty().addListener((obs, o, n) -> {
            updateColumnHeadings();
            recalculateShares();
        });
        splitPercentageRadio.selectedProperty().addListener((obs, o, n) -> {
            updateColumnHeadings();
            recalculateShares();
        });
    }

    private void updateColumnHeadings() {
        if (splitExactRadio.isSelected()) {
            colCustomInput.setText("Exact Amount (" + CurrencyFormatter.getCurrencySymbol() + ")");
            colCustomInput.setEditable(true);
        } else if (splitPercentageRadio.isSelected()) {
            colCustomInput.setText("Percentage (%)");
            colCustomInput.setEditable(true);
        } else {
            colCustomInput.setText("Custom (N/A)");
            colCustomInput.setEditable(false);
        }
    }

    private void loadUserGroups() {
        User user = SessionManager.getCurrentUser();
        if (user == null) return;
        List<Group> groups = groupService.getUserGroups(user.getId());
        groupComboBox.setItems(FXCollections.observableArrayList(groups));
        if (!groups.isEmpty()) {
            groupComboBox.getSelectionModel().selectFirst();
        }
    }

    private void loadGroupMembers(int groupId) {
        List<GroupMember> members = groupService.getGroupMembers(groupId);
        paidByComboBox.setItems(FXCollections.observableArrayList(members));

        User currentUser = SessionManager.getCurrentUser();
        if (currentUser != null) {
            for (GroupMember m : members) {
                if (m.getUserId() == currentUser.getId()) {
                    paidByComboBox.getSelectionModel().select(m);
                    break;
                }
            }
        }
        if (paidByComboBox.getSelectionModel().getSelectedItem() == null && !members.isEmpty()) {
            paidByComboBox.getSelectionModel().selectFirst();
        }

        participantRows.clear();
        for (GroupMember m : members) {
            ParticipantRow row = new ParticipantRow(m.getUserId(), m.getUserFullName() + " (@" + m.getUserName() + ")");
            row.selectedProperty().addListener((obs, o, n) -> recalculateShares());
            participantRows.add(row);
        }

        recalculateShares();
    }

    private void recalculateShares() {
        long amountPaise = 0;
        try {
            amountPaise = CurrencyFormatter.parseToPaise(amountField.getText());
        } catch (Exception ignored) {
        }

        List<ParticipantRow> selected = new ArrayList<>();
        for (ParticipantRow row : participantRows) {
            if (row.isSelected()) selected.add(row);
            else {
                row.setCalculatedShareText("₹0.00");
            }
        }

        if (selected.isEmpty() || amountPaise <= 0) {
            splitValidationSummaryLabel.setText("Enter amount and select at least one participant.");
            splitValidationSummaryLabel.setStyle("-fx-text-fill: -color-text-secondary;");
            return;
        }

        if (splitEqualRadio.isSelected()) {
            int count = selected.size();
            long baseShare = amountPaise / count;
            long remainder = amountPaise % count;
            for (int i = 0; i < count; i++) {
                long share = baseShare + (i < remainder ? 1 : 0);
                selected.get(i).setCalculatedShareText(CurrencyFormatter.format(share));
            }
            splitValidationSummaryLabel.setText("✓ Split equally among " + count + " members (" + CurrencyFormatter.format(amountPaise) + ")");
            splitValidationSummaryLabel.setStyle("-fx-text-fill: #10b981; -fx-font-weight: bold;");

        } else if (splitExactRadio.isSelected()) {
            long sum = 0;
            for (ParticipantRow row : selected) {
                try {
                    long p = CurrencyFormatter.parseToPaise(row.getCustomInput());
                    sum += p;
                    row.setCalculatedShareText(CurrencyFormatter.format(p));
                } catch (Exception e) {
                    row.setCalculatedShareText("₹0.00");
                }
            }
            long diff = amountPaise - sum;
            if (diff == 0) {
                splitValidationSummaryLabel.setText("✓ Exact amounts match total: " + CurrencyFormatter.format(amountPaise));
                splitValidationSummaryLabel.setStyle("-fx-text-fill: #10b981; -fx-font-weight: bold;");
            } else if (diff > 0) {
                splitValidationSummaryLabel.setText("⚠ " + CurrencyFormatter.format(diff) + " remaining to be allocated.");
                splitValidationSummaryLabel.setStyle("-fx-text-fill: #f59e0b; -fx-font-weight: bold;");
            } else {
                splitValidationSummaryLabel.setText("⚠ Sum exceeds total by " + CurrencyFormatter.format(-diff));
                splitValidationSummaryLabel.setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold;");
            }

        } else if (splitPercentageRadio.isSelected()) {
            double percentSum = 0;
            for (ParticipantRow row : selected) {
                try {
                    double pct = Double.parseDouble(row.getCustomInput().trim());
                    percentSum += pct;
                    long share = Math.round((pct / 100.0) * amountPaise);
                    row.setCalculatedShareText(CurrencyFormatter.format(share));
                } catch (Exception e) {
                    row.setCalculatedShareText("₹0.00");
                }
            }
            if (Math.abs(percentSum - 100.0) < 0.01) {
                splitValidationSummaryLabel.setText("✓ Total: 100.0% (" + CurrencyFormatter.format(amountPaise) + ")");
                splitValidationSummaryLabel.setStyle("-fx-text-fill: #10b981; -fx-font-weight: bold;");
            } else {
                splitValidationSummaryLabel.setText("⚠ Current total is " + String.format("%.2f", percentSum) + "%. Must equal 100.00%");
                splitValidationSummaryLabel.setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold;");
            }
        }
    }

    @FXML
    public void handleSaveExpense() {
        String title = titleField.getText().trim();
        if (title.isEmpty()) {
            AlertHelper.showWarning("Validation", "Please enter an expense title.");
            return;
        }

        long amountPaise;
        try {
            amountPaise = CurrencyFormatter.parseToPaise(amountField.getText());
            if (amountPaise <= 0) throw new IllegalArgumentException();
        } catch (Exception e) {
            AlertHelper.showWarning("Validation", "Please enter a valid positive amount.");
            return;
        }

        Group group = groupComboBox.getSelectionModel().getSelectedItem();
        if (group == null) {
            AlertHelper.showWarning("Validation", "Please select a group.");
            return;
        }

        GroupMember payer = paidByComboBox.getSelectionModel().getSelectedItem();
        if (payer == null) {
            AlertHelper.showWarning("Validation", "Please select who paid for this expense.");
            return;
        }

        List<Integer> participantIds = new ArrayList<>();
        List<String> participantNames = new ArrayList<>();
        List<Long> exactShares = new ArrayList<>();
        List<Double> percentages = new ArrayList<>();

        for (ParticipantRow r : participantRows) {
            if (r.isSelected()) {
                participantIds.add(r.getUserId());
                participantNames.add(r.getName());
                if (splitExactRadio.isSelected()) {
                    try {
                        exactShares.add(CurrencyFormatter.parseToPaise(r.getCustomInput()));
                    } catch (Exception e) {
                        AlertHelper.showWarning("Validation", "Invalid exact amount for " + r.getName());
                        return;
                    }
                } else if (splitPercentageRadio.isSelected()) {
                    try {
                        percentages.add(Double.parseDouble(r.getCustomInput().trim()));
                    } catch (Exception e) {
                        AlertHelper.showWarning("Validation", "Invalid percentage for " + r.getName());
                        return;
                    }
                }
            }
        }

        if (participantIds.isEmpty()) {
            AlertHelper.showWarning("Validation", "Please select at least one participating member.");
            return;
        }

        SplitMethod splitMethod = SplitMethod.EQUAL;
        if (splitExactRadio.isSelected()) splitMethod = SplitMethod.EXACT;
        else if (splitPercentageRadio.isSelected()) splitMethod = SplitMethod.PERCENTAGE;

        try {
            boolean success = expenseService.addExpense(
                    group.getId(),
                    payer.getUserId(),
                    title,
                    descriptionArea.getText().trim(),
                    categoryComboBox.getSelectionModel().getSelectedItem(),
                    splitMethod,
                    amountPaise,
                    datePicker.getValue(),
                    participantIds,
                    participantNames,
                    exactShares,
                    percentages
            );

            if (success) {
                AlertHelper.showInformation("Expense Saved", "Expense '" + title + "' of " + CurrencyFormatter.format(amountPaise) + " added successfully!");
                if (MainLayoutController.getInstance() != null) {
                    MainLayoutController.getInstance().showExpenses();
                }
            } else {
                AlertHelper.showError("Save Error", "Failed to save expense. Please verify all inputs.");
            }
        } catch (IllegalArgumentException e) {
            AlertHelper.showWarning("Validation Error", e.getMessage());
        } catch (Exception e) {
            AlertHelper.showError("System Error", e.getMessage());
        }
    }

    @FXML
    public void handleResetForm() {
        titleField.clear();
        amountField.clear();
        descriptionArea.clear();
        categoryComboBox.getSelectionModel().select(ExpenseCategory.FOOD);
        datePicker.setValue(LocalDate.now());
        splitEqualRadio.setSelected(true);
        if (groupComboBox.getSelectionModel().getSelectedItem() != null) {
            loadGroupMembers(groupComboBox.getSelectionModel().getSelectedItem().getId());
        }
    }

    public static class ParticipantRow {
        private final int userId;
        private final String name;
        private final BooleanProperty selected = new SimpleBooleanProperty(true);
        private final StringProperty customInput = new SimpleStringProperty("");
        private final StringProperty calculatedShareText = new SimpleStringProperty("₹0.00");

        public ParticipantRow(int userId, String name) {
            this.userId = userId;
            this.name = name;
        }

        public int getUserId() { return userId; }
        public String getName() { return name; }

        public BooleanProperty selectedProperty() { return selected; }
        public boolean isSelected() { return selected.get(); }
        public void setSelected(boolean val) { selected.set(val); }

        public StringProperty customInputProperty() { return customInput; }
        public String getCustomInput() { return customInput.get(); }
        public void setCustomInput(String val) { customInput.set(val); }

        public StringProperty calculatedShareTextProperty() { return calculatedShareText; }
        public String getCalculatedShareText() { return calculatedShareText.get(); }
        public void setCalculatedShareText(String val) { calculatedShareText.set(val); }
    }
}

