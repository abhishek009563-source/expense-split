package com.expensesplitter;

import com.expensesplitter.engine.SplitCalculator;
import com.expensesplitter.model.ExpenseParticipant;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class SplitCalculatorTest {

    @Test
    public void testEqualSplitWithRemainder() {
        // ₹10.00 = 1000 paise split among 3 people
        // 1000 / 3 = 333 remainder 1.
        // First person gets 334 paise (₹3.34), other two get 333 paise (₹3.33)
        List<Integer> userIds = List.of(1, 2, 3);
        List<String> userNames = List.of("A", "B", "C");

        List<ExpenseParticipant> result = SplitCalculator.calculateEqualSplit(1000, userIds, userNames);

        assertEquals(3, result.size());
        assertEquals(334, result.get(0).getShareAmountPaise());
        assertEquals(333, result.get(1).getShareAmountPaise());
        assertEquals(333, result.get(2).getShareAmountPaise());

        // Check total sum equals original amount
        long sum = result.stream().mapToLong(ExpenseParticipant::getShareAmountPaise).sum();
        assertEquals(1000, sum);
    }

    @Test
    public void testEqualSplitUserExample() {
        // Rahul pays ₹900 for dinner among Rahul, Aman, Rohit (₹300 each = 30000 paise each)
        List<Integer> userIds = List.of(1, 2, 3);
        List<String> userNames = List.of("Rahul", "Aman", "Rohit");

        List<ExpenseParticipant> result = SplitCalculator.calculateEqualSplit(90000, userIds, userNames);

        assertEquals(3, result.size());
        for (ExpenseParticipant p : result) {
            assertEquals(30000, p.getShareAmountPaise());
        }
        assertEquals(90000, result.stream().mapToLong(ExpenseParticipant::getShareAmountPaise).sum());
    }

    @Test
    public void testExactSplitValid() {
        List<Integer> userIds = List.of(1, 2);
        List<String> userNames = List.of("User1", "User2");
        List<Long> shares = List.of(4500L, 5500L); // ₹45 and ₹55 = ₹100 (10000 paise)

        List<ExpenseParticipant> result = SplitCalculator.calculateExactSplit(10000, userIds, userNames, shares);
        assertEquals(2, result.size());
        assertEquals(4500, result.get(0).getShareAmountPaise());
        assertEquals(5500, result.get(1).getShareAmountPaise());
        assertEquals(10000, result.stream().mapToLong(ExpenseParticipant::getShareAmountPaise).sum());
    }

    @Test
    public void testExactSplitInvalidSumThrowsException() {
        List<Integer> userIds = List.of(1, 2);
        List<String> userNames = List.of("User1", "User2");
        List<Long> shares = List.of(4000L, 5000L); // Sum is 9000 instead of 10000

        assertThrows(IllegalArgumentException.class, () -> {
            SplitCalculator.calculateExactSplit(10000, userIds, userNames, shares);
        });
    }

    @Test
    public void testPercentageSplitValid() {
        List<Integer> userIds = List.of(1, 2, 3);
        List<String> userNames = List.of("User1", "User2", "User3");
        List<Double> percentages = List.of(50.0, 30.0, 20.0);

        List<ExpenseParticipant> result = SplitCalculator.calculatePercentageSplit(10000, userIds, userNames, percentages);
        assertEquals(3, result.size());
        assertEquals(5000, result.get(0).getShareAmountPaise());
        assertEquals(3000, result.get(1).getShareAmountPaise());
        assertEquals(2000, result.get(2).getShareAmountPaise());
        assertEquals(10000, result.stream().mapToLong(ExpenseParticipant::getShareAmountPaise).sum());
    }

    @Test
    public void testPercentageSplitInvalidPercentageThrowsException() {
        List<Integer> userIds = List.of(1, 2);
        List<String> userNames = List.of("User1", "User2");
        List<Double> percentages = List.of(60.0, 30.0); // Sum is 90%, not 100%

        assertThrows(IllegalArgumentException.class, () -> {
            SplitCalculator.calculatePercentageSplit(10000, userIds, userNames, percentages);
        });
    }
}

