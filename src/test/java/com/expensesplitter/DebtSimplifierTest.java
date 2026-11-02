package com.expensesplitter;

import com.expensesplitter.engine.DebtSimplifier;
import com.expensesplitter.model.Balance;
import com.expensesplitter.model.SimplifiedDebt;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class DebtSimplifierTest {

    @Test
    public void testUserSpecificationDinnerExample() {
        // Rahul pays ₹900 (90000 paise) for dinner.
        // Divided equally among Rahul, Aman, Rohit (₹300 each = 30000 paise).
        // Rahul net balance = 90000 - 30000 = +60000 (+₹600)
        // Aman net balance = 0 - 30000 = -30000 (-₹300)
        // Rohit net balance = 0 - 30000 = -30000 (-₹300)

        Balance bRahul = new Balance(1, "rahul", "Rahul Sharma", 90000, 30000);
        Balance bAman = new Balance(2, "aman", "Aman Verma", 0, 30000);
        Balance bRohit = new Balance(3, "rohit", "Rohit Patel", 0, 30000);

        List<SimplifiedDebt> debts = DebtSimplifier.simplifyDebts(List.of(bRahul, bAman, bRohit));

        assertEquals(2, debts.size());

        // Each should owe Rahul ₹300
        long totalOwedToRahul = debts.stream()
                .filter(d -> d.getToUserId() == 1)
                .mapToLong(SimplifiedDebt::getAmountPaise)
                .sum();
        assertEquals(60000, totalOwedToRahul);

        for (SimplifiedDebt d : debts) {
            assertEquals(1, d.getToUserId());
            assertEquals(30000, d.getAmountPaise());
            assertTrue(d.getFromUserId() == 2 || d.getFromUserId() == 3);
        }
    }

    @Test
    public void testCircularDebtSimplification() {
        // User A owes B ₹100, B owes C ₹100, C owes A ₹100 -> All net balances are 0!
        Balance bA = new Balance(1, "A", "User A", 10000, 10000);
        Balance bB = new Balance(2, "B", "User B", 10000, 10000);
        Balance bC = new Balance(3, "C", "User C", 10000, 10000);

        List<SimplifiedDebt> debts = DebtSimplifier.simplifyDebts(List.of(bA, bB, bC));
        assertEquals(0, debts.size()); // Zero transactions required!
    }

    @Test
    public void testTransitiveDebtSimplification() {
        // A paid 0, share 100 => net -100
        // B paid 100, share 100 => net 0
        // C paid 200, share 100 => net +100
        // Result: A directly pays C ₹100! (B doesn't need to be involved in the transaction)
        Balance bA = new Balance(1, "A", "User A", 0, 10000);
        Balance bB = new Balance(2, "B", "User B", 10000, 10000);
        Balance bC = new Balance(3, "C", "User C", 20000, 10000);

        List<SimplifiedDebt> debts = DebtSimplifier.simplifyDebts(List.of(bA, bB, bC));
        assertEquals(1, debts.size());
        SimplifiedDebt d = debts.get(0);
        assertEquals(1, d.getFromUserId()); // A
        assertEquals(3, d.getToUserId());   // C
        assertEquals(10000, d.getAmountPaise()); // ₹100
    }
}

