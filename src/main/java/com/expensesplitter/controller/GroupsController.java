package com.expensesplitter.controller;

import com.expensesplitter.dao.UserDao;
import com.expensesplitter.model.Balance;
import com.expensesplitter.model.Expense;
import com.expensesplitter.model.Group;
import com.expensesplitter.model.GroupMember;
import com.expensesplitter.model.User;
import com.expensesplitter.service.ExpenseService;
import com.expensesplitter.service.GroupService;
import com.expensesplitter.util.AlertHelper;
import com.expensesplitter.util.CurrencyFormatter;
import com.expensesplitter.util.SessionManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class GroupsController {

    @FXML private ListView<Group> groupsListView;
    @FXML private Label selectedGroupNameLabel;
    @FXML private Label selectedGroupDescLabel;
    @FXML private Label selectedGroupCreatorLabel;
    @FXML private Label selectedGroupTotalSpentLabel;
    @FXML private Button deleteGroupBtn;

    // Members section
    @FXML private TableView<GroupMember> membersTable;
    @FXML private TableColumn<GroupMember, String> colMemberName;
    @FXML private TableColumn<GroupMember, String> colMemberEmail;
    @FXML private TableColumn<GroupMember, String> colMemberRole;
    @FXML private TableColumn<GroupMember, String> colMemberBalance;

    @FXML private ComboBox<User> addMemberComboBox;

    // Group Expenses table
    @FXML private TableView<Expense> groupExpensesTable;
    @FXML private TableColumn<Expense, String> colExpDate;
    @FXML private TableColumn<Expense, String> colExpTitle;
    @FXML private TableColumn<Expense, String> colExpCategory;
    @FXML private TableColumn<Expense, String> colExpPayer;
    @FXML private TableColumn<Expense, String> colExpAmount;

    // New Group Panel
    @FXML private VBox createGroupPanel;
    @FXML private TextField newGroupNameField;
    @FXML private TextField newGroupDescField;
    @FXML private ListView<User> selectMembersListView;

    private final GroupService groupService = new GroupService();
    private final ExpenseService expenseService = new ExpenseService();
    private final UserDao userDao = new UserDao();

    private Group currentSelectedGroup;

    @FXML
    public void initialize() {
        createGroupPanel.setVisible(false);
        createGroupPanel.setManaged(false);

        setupGroupListView();
        setupMembersTable();
        setupGroupExpensesTable();

        refreshGroupsList();
    }

    private void setupGroupListView() {
        groupsListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Group group, boolean empty) {
                super.updateItem(group, empty);
                if (empty || group == null) {
                    setText(null);
                } else {
                    setText(group.getName() + " (" + group.getMemberCount() + " members)");
                }
            }
        });

        groupsListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                currentSelectedGroup = newVal;
                loadGroupDetails(newVal);
            }
        });
    }

    private void setupMembersTable() {
        colMemberName.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getUserFullName() + " (@" + cell.getValue().getUserName() + ")"));
        colMemberEmail.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getUserEmail()));
        colMemberRole.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getRole()));
        colMemberBalance.setCellValueFactory(cell -> {
            if (currentSelectedGroup == null) return new SimpleStringProperty("-");
            List<Balance> balances = expenseService.getGroupBalances(currentSelectedGroup.getId());
            for (Balance b : balances) {
                if (b.getUserId() == cell.getValue().getUserId()) {
                    long net = b.getNetBalancePaise();
                    if (net > 0) {
                        return new SimpleStringProperty("+" + CurrencyFormatter.format(net));
                    } else if (net < 0) {
                        return new SimpleStringProperty("-" + CurrencyFormatter.format(Math.abs(net)));
                    } else {
                        return new SimpleStringProperty("Settled (₹0)");
                    }
                }
            }
            return new SimpleStringProperty("₹0.00");
        });
    }

    private void setupGroupExpensesTable() {
        colExpDate.setCellValueFactory(cell -> new SimpleStringProperty(
                cell.getValue().getExpenseDate() != null ? cell.getValue().getExpenseDate().format(DateTimeFormatter.ofPattern("MMM dd, yyyy")) : ""
        ));
        colExpTitle.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getTitle()));
        colExpCategory.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getCategory() != null ? cell.getValue().getCategory().getDisplayName() : "Other"));
        colExpPayer.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getPaidByUserName()));
        colExpAmount.setCellValueFactory(cell -> new SimpleStringProperty(CurrencyFormatter.format(cell.getValue().getAmountPaise())));
    }

    public void refreshGroupsList() {
        User currentUser = SessionManager.getCurrentUser();
        if (currentUser == null) return;

        List<Group> groups = groupService.getUserGroups(currentUser.getId());
        groupsListView.setItems(FXCollections.observableArrayList(groups));

        if (!groups.isEmpty()) {
            if (currentSelectedGroup != null) {
                // Keep selection if still exists
                for (Group g : groups) {
                    if (g.getId() == currentSelectedGroup.getId()) {
                        groupsListView.getSelectionModel().select(g);
                        return;
                    }
                }
            }
            groupsListView.getSelectionModel().selectFirst();
        }
    }

    private void loadGroupDetails(Group group) {
        selectedGroupNameLabel.setText(group.getName());
        selectedGroupDescLabel.setText(group.getDescription().isEmpty() ? "No description provided." : group.getDescription());
        selectedGroupCreatorLabel.setText("Created by: " + group.getCreatedByUserName());

        List<GroupMember> members = groupService.getGroupMembers(group.getId());
        membersTable.setItems(FXCollections.observableArrayList(members));

        List<Expense> expenses = expenseService.getGroupExpenses(group.getId());
        groupExpensesTable.setItems(FXCollections.observableArrayList(expenses));

        long total = 0;
        for (Expense e : expenses) total += e.getAmountPaise();
        selectedGroupTotalSpentLabel.setText("Total Group Spending: " + CurrencyFormatter.format(total));

        // Setup Add Member combo box
        List<User> allUsers = userDao.findAll();
        List<Integer> existingMemberIds = members.stream().map(GroupMember::getUserId).collect(Collectors.toList());
        List<User> eligibleUsers = allUsers.stream().filter(u -> !existingMemberIds.contains(u.getId())).collect(Collectors.toList());

        addMemberComboBox.setItems(FXCollections.observableArrayList(eligibleUsers));
        addMemberComboBox.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(User item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getFullName() + " (@" + item.getUsername() + ")");
            }
        });
        addMemberComboBox.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(User item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "Select user to add..." : item.getFullName() + " (@" + item.getUsername() + ")");
            }
        });

        User user = SessionManager.getCurrentUser();
        deleteGroupBtn.setVisible(user != null && user.getId() == group.getCreatedByUserId());
    }

    @FXML
    public void handleOpenCreateGroup() {
        createGroupPanel.setVisible(true);
        createGroupPanel.setManaged(true);
        newGroupNameField.clear();
        newGroupDescField.clear();

        User current = SessionManager.getCurrentUser();
        List<User> candidateMembers = userDao.findAll().stream()
                .filter(u -> current == null || u.getId() != current.getId())
                .collect(Collectors.toList());

        selectMembersListView.setItems(FXCollections.observableArrayList(candidateMembers));
        selectMembersListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        selectMembersListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(User u, boolean empty) {
                super.updateItem(u, empty);
                setText(empty || u == null ? null : u.getFullName() + " (@" + u.getUsername() + " - " + u.getEmail() + ")");
            }
        });
    }

    @FXML
    public void handleCancelCreateGroup() {
        createGroupPanel.setVisible(false);
        createGroupPanel.setManaged(false);
    }

    @FXML
    public void handleSaveCreateGroup() {
        String name = newGroupNameField.getText().trim();
        String desc = newGroupDescField.getText().trim();
        User currentUser = SessionManager.getCurrentUser();

        if (name.isEmpty()) {
            AlertHelper.showWarning("Validation", "Please enter a group name.");
            return;
        }

        List<Integer> memberIds = new ArrayList<>();
        if (currentUser != null) {
            memberIds.add(currentUser.getId());
        }
        for (User u : selectMembersListView.getSelectionModel().getSelectedItems()) {
            if (currentUser == null || u.getId() != currentUser.getId()) {
                memberIds.add(u.getId());
            }
        }

        try {
            boolean created = groupService.createGroup(name, desc, currentUser != null ? currentUser.getId() : 1, memberIds);
            if (created) {
                AlertHelper.showInformation("Group Created", "Group '" + name + "' was successfully created!");
                createGroupPanel.setVisible(false);
                createGroupPanel.setManaged(false);
                refreshGroupsList();
            } else {
                AlertHelper.showError("Error", "Could not create group. Please check database permissions.");
            }
        } catch (Exception e) {
            AlertHelper.showError("Error", e.getMessage());
        }
    }

    @FXML
    public void handleAddMemberToCurrentGroup() {
        if (currentSelectedGroup == null) return;
        User selected = addMemberComboBox.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertHelper.showWarning("Select User", "Please select a user to add to this group.");
            return;
        }

        boolean added = groupService.addMember(currentSelectedGroup.getId(), selected.getId(), "MEMBER");
        if (added) {
            AlertHelper.showInformation("Member Added", selected.getFullName() + " has been added to " + currentSelectedGroup.getName());
            loadGroupDetails(currentSelectedGroup);
        } else {
            AlertHelper.showError("Error", "Could not add member.");
        }
    }

    @FXML
    public void handleRemoveSelectedMember() {
        if (currentSelectedGroup == null) return;
        GroupMember selected = membersTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertHelper.showWarning("Select Member", "Please select a member to remove from the table.");
            return;
        }

        if ("ADMIN".equalsIgnoreCase(selected.getRole())) {
            AlertHelper.showWarning("Cannot Remove Admin", "The group creator/admin cannot be removed.");
            return;
        }

        // Check if member has nonzero balance in this group
        List<Balance> balances = expenseService.getGroupBalances(currentSelectedGroup.getId());
        for (Balance b : balances) {
            if (b.getUserId() == selected.getUserId() && b.getNetBalancePaise() != 0) {
                AlertHelper.showWarning("Active Balance", "Cannot remove " + selected.getUserFullName() + " because they have an unsettled balance of " + CurrencyFormatter.format(b.getNetBalancePaise()) + ". Please settle debts first.");
                return;
            }
        }

        if (AlertHelper.showConfirmation("Remove Member", "Are you sure you want to remove " + selected.getUserFullName() + " from this group?")) {
            boolean removed = groupService.removeMember(currentSelectedGroup.getId(), selected.getUserId());
            if (removed) {
                AlertHelper.showInformation("Member Removed", selected.getUserFullName() + " was removed.");
                loadGroupDetails(currentSelectedGroup);
            } else {
                AlertHelper.showError("Error", "Could not remove member.");
            }
        }
    }

    @FXML
    public void handleDeleteGroup() {
        if (currentSelectedGroup == null) return;
        if (AlertHelper.showConfirmation("Delete Group", "Are you sure you want to delete group '" + currentSelectedGroup.getName() + "'? All associated expenses and records will be permanently removed.")) {
            boolean deleted = groupService.deleteGroup(currentSelectedGroup.getId());
            if (deleted) {
                AlertHelper.showInformation("Group Deleted", "Group removed successfully.");
                currentSelectedGroup = null;
                refreshGroupsList();
            } else {
                AlertHelper.showError("Error", "Could not delete group.");
            }
        }
    }
}

