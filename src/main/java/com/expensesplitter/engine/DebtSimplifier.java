package com.expensesplitter.engine;

import com.expensesplitter.model.Balance;
import com.expensesplitter.model.SimplifiedDebt;

import java.util.*;

/**
 * Implements the Minimum Cash Flow (Debt Simplification) algorithm.
 * Converts complex N-to-N cross debts into the minimum number of direct settlement transactions (at most N-1).
 */
public class DebtSimplifier {

    private static class Node {
        int userId;
        String userName;
        String userFullName;
        long netBalance;

        Node(int userId, String userName, String userFullName, long netBalance) {
            this.userId = userId;
            this.userName = userName;
            this.userFullName = userFullName;
            this.netBalance = netBalance;
        }
    }

    /**
     * Simplifies debts for a list of member balances.
     */
    public static List<SimplifiedDebt> simplifyDebts(List<Balance> balances) {
        List<SimplifiedDebt> settlements = new ArrayList<>();
        if (balances == null || balances.isEmpty()) {
            return settlements;
        }

        // Separate debtors (netBalance < 0) and creditors (netBalance > 0)
        PriorityQueue<Node> debtors = new PriorityQueue<>(Comparator.comparingLong(n -> n.netBalance)); // most negative first
        PriorityQueue<Node> creditors = new PriorityQueue<>((a, b) -> Long.compare(b.netBalance, a.netBalance)); // most positive first

        for (Balance b : balances) {
            if (b.getNetBalancePaise() < 0) {
                debtors.add(new Node(b.getUserId(), b.getUserName(), b.getUserFullName(), b.getNetBalancePaise()));
            } else if (b.getNetBalancePaise() > 0) {
                creditors.add(new Node(b.getUserId(), b.getUserName(), b.getUserFullName(), b.getNetBalancePaise()));
            }
        }

        // Greedy matching of largest debtor with largest creditor
        while (!debtors.isEmpty() && !creditors.isEmpty()) {
            Node debtor = debtors.poll();
            Node creditor = creditors.poll();

            long debtAmount = -debtor.netBalance;
            long creditAmount = creditor.netBalance;

            long settledAmount = Math.min(debtAmount, creditAmount);

            if (settledAmount > 0) {
                settlements.add(new SimplifiedDebt(
                        debtor.userId,
                        debtor.userName,
                        debtor.userFullName,
                        creditor.userId,
                        creditor.userName,
                        creditor.userFullName,
                        settledAmount
                ));
            }

            long remainingDebt = debtAmount - settledAmount;
            long remainingCredit = creditAmount - settledAmount;

            if (remainingDebt > 0) {
                debtor.netBalance = -remainingDebt;
                debtors.add(debtor);
            }

            if (remainingCredit > 0) {
                creditor.netBalance = remainingCredit;
                creditors.add(creditor);
            }
        }

        return settlements;
    }
}

