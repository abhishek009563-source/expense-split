package com.expensesplitter.controller;

import com.expensesplitter.model.Group;
import com.expensesplitter.model.GroupMember;
import com.expensesplitter.model.Settlement;
import com.expensesplitter.model.User;
import com.expensesplitter.service.GroupService;
import com.expensesplitter.service.SettlementService;
import com.expensesplitter.util.AlertHelper;
import com.expensesplitter.util.CurrencyFormatter;
import com.expensesplitter.util.SessionManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class SettlementsController {

    @FXML private ComboBox<Group> groupComboBox;
    @FXML private ComboBox<GroupMember> payerComboBox;
    @FXML private ComboBox<GroupMember> receiverComboBox;
    @FXML private TextField amountField;
    @FXML private DatePicker datePicker;
    @FXML private TextField notesField;

    @FXML private TableView<Settlement> settlementsTable;
    @FXML private TableColumn<Settlement, String> colDate;
    @FXML private TableColumn<Settlement, String> colGroup;
    @FXML private TableColumn<Settlement, String> colPayer;
    @FXML private TableColumn<Settlement, String> colReceiver;
    @FXML private TableColumn<Settlement, String> colAmount;
    @FXML private TableColumn<Settlement, String> colNotes;

    private final SettlementService settlementService = new SettlementService();
    private final GroupService groupService = new GroupService();

    @FXML
    public void initialize() {
        datePicker.setValue(LocalDate.now());

        setupSelectors();
        setupTable();
        loadUserGroups();
        loadSettlementsHistory();
    }

    private void setupSelectors() {
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
                loadMembersForGroup(newG.getId());
            }
        });

        payerComboBox.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(GroupMember item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getUserFullName() + " (@" + item.getUserName() + ")");
            }
        });
        payerComboBox.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(GroupMember item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "Select Payer..." : item.getUserFullName() + " (@" + item.getUserName() + ")");
            }
        });

        receiverComboBox.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(GroupMember item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getUserFullName() + " (@" + item.getUserName() + ")");
            }
        });
        receiverComboBox.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(GroupMember item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "Select Receiver..." : item.getUserFullName() + " (@" + item.getUserName() + ")");
            }
        });
    }

    private void setupTable() {
        colDate.setCellValueFactory(cell -> new SimpleStringProperty(
                cell.getValue().getSettlementDate() != null ? cell.getValue().getSettlementDate().format(DateTimeFormatter.ofPattern("MMM dd, yyyy")) : ""
        ));
        colGroup.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getGroupName()));
        colPayer.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getPayerUserName()));
        colReceiver.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getReceiverUserName()));
        colAmount.setCellValueFactory(cell -> new SimpleStringProperty(CurrencyFormatter.format(cell.getValue().getAmountPaise())));
        colNotes.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getNotes()));
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

    private void loadMembersForGroup(int groupId) {
        List<GroupMember> members = groupService.getGroupMembers(groupId);
        payerComboBox.setItems(FXCollections.observableArrayList(members));
        receiverComboBox.setItems(FXCollections.observableArrayList(members));

        User current = SessionManager.getCurrentUser();
        if (current != null) {
            for (GroupMember m : members) {
                if (m.getUserId() == current.getId()) {
                    payerComboBox.getSelectionModel().select(m);
                    break;
                }
            }
        }
    }

    private void loadSettlementsHistory() {
        User user = SessionManager.getCurrentUser();
        if (user == null) return;
        List<Settlement> settlements = settlementService.getUserSettlements(user.getId());
        settlementsTable.setItems(FXCollections.observableArrayList(settlements));
    }

    @FXML
    public void handleRecordSettlement() {
        Group group = groupComboBox.getSelectionModel().getSelectedItem();
        GroupMember payer = payerComboBox.getSelectionModel().getSelectedItem();
        GroupMember receiver = receiverComboBox.getSelectionModel().getSelectedItem();

        if (group == null || payer == null || receiver == null) {
            AlertHelper.showWarning("Validation", "Please select a group, payer, and receiver.");
            return;
        }

        if (payer.getUserId() == receiver.getUserId()) {
            AlertHelper.showWarning("Validation", "Payer and receiver cannot be the same person.");
            return;
        }

        long amountPaise;
        try {
            amountPaise = CurrencyFormatter.parseToPaise(amountField.getText());
            if (amountPaise <= 0) throw new IllegalArgumentException();
        } catch (Exception e) {
            AlertHelper.showWarning("Validation", "Please enter a valid settlement amount.");
            return;
        }

        try {
            boolean recorded = settlementService.recordSettlement(
                    group.getId(),
                    payer.getUserId(),
                    receiver.getUserId(),
                    amountPaise,
                    datePicker.getValue(),
                    notesField.getText().trim()
            );

            if (recorded) {
                AlertHelper.showInformation("Settlement Recorded", "Payment of " + CurrencyFormatter.format(amountPaise) + " from " + payer.getUserFullName() + " to " + receiver.getUserFullName() + " was successfully recorded!");
                amountField.clear();
                notesField.clear();
                loadSettlementsHistory();
            } else {
                AlertHelper.showError("Error", "Could not record settlement.");
            }
        } catch (Exception e) {
            AlertHelper.showError("System Error", e.getMessage());
        }
    }

    @FXML
    public void handleDeleteSettlement() {
        Settlement selected = settlementsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertHelper.showWarning("Select Settlement", "Please select a settlement from the table to delete.");
            return;
        }

        if (AlertHelper.showConfirmation("Delete Settlement", "Are you sure you want to delete this settlement record of " + CurrencyFormatter.format(selected.getAmountPaise()) + "? Balances will be updated accordingly.")) {
            boolean deleted = settlementService.deleteSettlement(selected.getId());
            if (deleted) {
                AlertHelper.showInformation("Deleted", "Settlement record was deleted.");
                loadSettlementsHistory();
            } else {
                AlertHelper.showError("Error", "Could not delete settlement.");
            }
        }
    }
}

