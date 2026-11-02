package com.expensesplitter.engine;

import com.expensesplitter.model.ExpenseParticipant;
import com.expensesplitter.model.SplitMethod;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles precise mathematical calculations for splitting expenses.
 * Uses integer minor units (paise) to eliminate floating-point precision errors.
 */
public class SplitCalculator {

    /**
     * Splits an expense equally among participants.
     * Guarantees that sum(shares) == totalAmountPaise by allocating the remainder.
     */
    public static List<ExpenseParticipant> calculateEqualSplit(long totalAmountPaise, List<Integer> userIds, List<String> userNames) {
        if (userIds == null || userIds.isEmpty()) {
            throw new IllegalArgumentException("At least one participant is required for splitting.");
        }
        if (totalAmountPaise <= 0) {
            throw new IllegalArgumentException("Total amount must be greater than zero.");
        }

        int n = userIds.size();
        long baseShare = totalAmountPaise / n;
        long remainder = totalAmountPaise % n;

        List<ExpenseParticipant> participants = new ArrayList<>();
        double equalPercentage = 100.0 / n;

        for (int i = 0; i < n; i++) {
            long share = baseShare + (i < remainder ? 1 : 0);
            participants.add(new ExpenseParticipant(
                    userIds.get(i),
                    userNames.get(i),
                    share,
                    equalPercentage
            ));
        }

        return participants;
    }

    /**
     * Validates and constructs exact amount split participants.
     */
    public static List<ExpenseParticipant> calculateExactSplit(long totalAmountPaise, List<Integer> userIds,
                                                               List<String> userNames, List<Long> exactAmountsPaise) {
        if (userIds == null || exactAmountsPaise == null || userIds.size() != exactAmountsPaise.size()) {
            throw new IllegalArgumentException("Participant list and exact amounts must be of identical size.");
        }
        if (userIds.isEmpty()) {
            throw new IllegalArgumentException("At least one participant is required.");
        }

        long sum = 0;
        for (long amt : exactAmountsPaise) {
            if (amt < 0) {
                throw new IllegalArgumentException("Individual shares cannot be negative.");
            }
            sum += amt;
        }

        if (sum != totalAmountPaise) {
            long diff = totalAmountPaise - sum;
            throw new IllegalArgumentException(String.format(
                    "Exact shares sum (₹%.2f) does not match total expense amount (₹%.2f). Difference: ₹%.2f",
                    sum / 100.0, totalAmountPaise / 100.0, diff / 100.0
            ));
        }

        List<ExpenseParticipant> participants = new ArrayList<>();
        for (int i = 0; i < userIds.size(); i++) {
            long share = exactAmountsPaise.get(i);
            double percentage = (totalAmountPaise > 0) ? ((double) share / totalAmountPaise) * 100.0 : 0.0;
            participants.add(new ExpenseParticipant(
                    userIds.get(i),
                    userNames.get(i),
                    share,
                    percentage
            ));
        }

        return participants;
    }

    /**
     * Splits an expense by custom percentages.
     * Validates that sum(percentages) == 100.0% and assigns rounding delta fairly.
     */
    public static List<ExpenseParticipant> calculatePercentageSplit(long totalAmountPaise, List<Integer> userIds,
                                                                    List<String> userNames, List<Double> percentages) {
        if (userIds == null || percentages == null || userIds.size() != percentages.size()) {
            throw new IllegalArgumentException("Participant list and percentages must be of identical size.");
        }
        if (userIds.isEmpty()) {
            throw new IllegalArgumentException("At least one participant is required.");
        }

        double totalPercent = 0.0;
        for (double p : percentages) {
            if (p < 0.0) {
                throw new IllegalArgumentException("Percentage cannot be negative.");
            }
            totalPercent += p;
        }

        // Allow tolerance for floating point representation (99.99 to 100.01)
        if (Math.abs(totalPercent - 100.0) > 0.05) {
            throw new IllegalArgumentException(String.format("Percentages must sum to 100%%. Current sum: %.2f%%", totalPercent));
        }

        List<ExpenseParticipant> participants = new ArrayList<>();
        long assignedSum = 0;

        for (int i = 0; i < userIds.size(); i++) {
            double p = percentages.get(i);
            long share = Math.round((totalAmountPaise * p) / 100.0);
            assignedSum += share;
            participants.add(new ExpenseParticipant(
                    userIds.get(i),
                    userNames.get(i),
                    share,
                    p
            ));
        }

        // Fairly adjust any 1-2 paise rounding discrepancy onto the largest share
        long discrepancy = totalAmountPaise - assignedSum;
        if (discrepancy != 0 && !participants.isEmpty()) {
            int targetIdx = 0;
            long maxShare = -1;
            for (int i = 0; i < participants.size(); i++) {
                if (participants.get(i).getShareAmountPaise() > maxShare) {
                    maxShare = participants.get(i).getShareAmountPaise();
                    targetIdx = i;
                }
            }
            ExpenseParticipant target = participants.get(targetIdx);
            target.setShareAmountPaise(target.getShareAmountPaise() + discrepancy);
        }

        return participants;
    }
}

