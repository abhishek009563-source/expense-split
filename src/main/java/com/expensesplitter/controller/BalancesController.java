package com.expensesplitter.controller;

import com.expensesplitter.model.Balance;
import com.expensesplitter.model.Group;
import com.expensesplitter.model.SimplifiedDebt;
import com.expensesplitter.model.User;
import com.expensesplitter.service.ExpenseService;
import com.expensesplitter.service.GroupService;
import com.expensesplitter.util.CurrencyFormatter;
import com.expensesplitter.util.SessionManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.List;

public class BalancesController {

    @FXML private ComboBox<Group> groupComboBox;
    @FXML private Label netBalanceSummaryLabel;

    @FXML private TableView<Balance> balancesTable;
    @FXML private TableColumn<Balance, String> colMember;
    @FXML private TableColumn<Balance, String> colTotalPaid;
    @FXML private TableColumn<Balance, String> colTotalShare;
    @FXML private TableColumn<Balance, String> colNetBalance;
    @FXML private TableColumn<Balance, String> colStatus;

    @FXML private VBox simplifiedDebtsContainer;
    @FXML private Label simplifiedDebtsCountLabel;

    private final ExpenseService expenseService = new ExpenseService();
    private final GroupService groupService = new GroupService();

    @FXML
    public void initialize() {
        setupTable();
        setupGroupSelector();
        loadGroups();
    }

    private void setupTable() {
        colMember.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getUserFullName() + " (@" + cell.getValue().getUserName() + ")"));
        colTotalPaid.setCellValueFactory(cell -> new SimpleStringProperty(CurrencyFormatter.format(cell.getValue().getTotalPaidPaise())));
        colTotalShare.setCellValueFactory(cell -> new SimpleStringProperty(CurrencyFormatter.format(cell.getValue().getTotalSharePaise())));
        colNetBalance.setCellValueFactory(cell -> {
            long net = cell.getValue().getNetBalancePaise();
            if (net > 0) return new SimpleStringProperty("+" + CurrencyFormatter.format(net));
            else if (net < 0) return new SimpleStringProperty("-" + CurrencyFormatter.format(Math.abs(net)));
            else return new SimpleStringProperty("₹0.00");
        });
        colStatus.setCellValueFactory(cell -> {
            long net = cell.getValue().getNetBalancePaise();
            if (net > 0) return new SimpleStringProperty("Gets back " + CurrencyFormatter.format(net));
            else if (net < 0) return new SimpleStringProperty("Owes " + CurrencyFormatter.format(Math.abs(net)));
            else return new SimpleStringProperty("Fully Settled");
        });
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
                setText(empty || item == null ? "Select Group..." : item.getName());
            }
        });

        groupComboBox.getSelectionModel().selectedItemProperty().addListener((obs, oldG, newG) -> {
            if (newG != null) {
                loadBalancesAndDebts(newG.getId());
            }
        });
    }

    private void loadGroups() {
        User user = SessionManager.getCurrentUser();
        if (user == null) return;
        List<Group> groups = groupService.getUserGroups(user.getId());
        groupComboBox.setItems(FXCollections.observableArrayList(groups));
        if (!groups.isEmpty()) {
            groupComboBox.getSelectionModel().selectFirst();
        }
    }

    private void loadBalancesAndDebts(int groupId) {
        List<Balance> balances = expenseService.getGroupBalances(groupId);
        balancesTable.setItems(FXCollections.observableArrayList(balances));

        User currentUser = SessionManager.getCurrentUser();
        if (currentUser != null) {
            long myNet = 0;
            for (Balance b : balances) {
                if (b.getUserId() == currentUser.getId()) {
                    myNet = b.getNetBalancePaise();
                    break;
                }
            }
            if (myNet > 0) {
                netBalanceSummaryLabel.setText("You are owed a total of " + CurrencyFormatter.format(myNet) + " in this group.");
                netBalanceSummaryLabel.setStyle("-fx-text-fill: #10b981; -fx-font-weight: bold;");
            } else if (myNet < 0) {
                netBalanceSummaryLabel.setText("You owe a total of " + CurrencyFormatter.format(Math.abs(myNet)) + " in this group.");
                netBalanceSummaryLabel.setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold;");
            } else {
                netBalanceSummaryLabel.setText("You are completely settled up in this group (₹0.00).");
                netBalanceSummaryLabel.setStyle("-fx-text-fill: #3b82f6; -fx-font-weight: bold;");
            }
        }

        // Render Simplified Debts Cards
        List<SimplifiedDebt> debts = expenseService.getSimplifiedDebtsForGroup(groupId);
        simplifiedDebtsContainer.getChildren().clear();
        simplifiedDebtsCountLabel.setText(debts.size() + " optimal payments required to clear all balances");

        if (debts.isEmpty()) {
            Label settledLabel = new Label("🎉 Everyone is settled up! There are no outstanding debts in this group.");
            settledLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #10b981; -fx-padding: 16;");
            simplifiedDebtsContainer.getChildren().add(settledLabel);
            return;
        }

        for (SimplifiedDebt debt : debts) {
            HBox card = createDebtCard(debt);
            simplifiedDebtsContainer.getChildren().add(card);
        }
    }

    private HBox createDebtCard(SimplifiedDebt debt) {
        HBox box = new HBox(14);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setStyle("-fx-background-color: -color-bg-base; -fx-border-color: -color-border; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-padding: 12px 16px;");

        // Debtor
        VBox debtorBox = new VBox(2);
        Label debtorLbl = new Label(debt.getFromUserName());
        debtorLbl.setStyle("-fx-font-weight: bold; -fx-text-fill: #ef4444; -fx-font-size: 14px;");
        Label debtorSub = new Label("Debtor (Owes)");
        debtorSub.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8;");
        debtorBox.getChildren().addAll(debtorLbl, debtorSub);

        Label arrow = new Label("── pays ──►");
        arrow.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #3b82f6;");

        // Creditor
        VBox creditorBox = new VBox(2);
        Label creditorLbl = new Label(debt.getToUserName());
        creditorLbl.setStyle("-fx-font-weight: bold; -fx-text-fill: #10b981; -fx-font-size: 14px;");
        Label creditorSub = new Label("Creditor (Receives)");
        creditorSub.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8;");
        creditorBox.getChildren().addAll(creditorLbl, creditorSub);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Amount
        Label amtLbl = new Label(CurrencyFormatter.format(debt.getAmountPaise()));
        amtLbl.setStyle("-fx-font-size: 16px; -fx-font-weight: 800; -fx-text-fill: -color-text-primary;");

        Button settleBtn = new Button("🤝 Settle Now");
        settleBtn.getStyleClass().add("btn-success");
        settleBtn.setStyle("-fx-font-size: 11px; -fx-padding: 6 12;");
        settleBtn.setOnAction(e -> {
            if (MainLayoutController.getInstance() != null) {
                MainLayoutController.getInstance().showSettlements();
            }
        });

        box.getChildren().addAll(debtorBox, arrow, creditorBox, spacer, amtLbl, settleBtn);
        return box;
    }
}

