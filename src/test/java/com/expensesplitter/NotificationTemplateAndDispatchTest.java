package com.expensesplitter;

import com.expensesplitter.database.DatabaseInitializer;
import com.expensesplitter.dao.NotificationDao;
import com.expensesplitter.model.*;
import com.expensesplitter.service.DeliveryResult;
import com.expensesplitter.service.EmailGateway;
import com.expensesplitter.service.NotificationTemplates;
import com.expensesplitter.service.SmsGateway;
import com.expensesplitter.util.CurrencyFormatter;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Notification Templates, Idempotency, and Delivery Resilience Tests")
public class NotificationTemplateAndDispatchTest {

    @BeforeAll
    public static void setUpDatabase() {
        DatabaseInitializer.initializeDatabase();
    }

    @Test
    @DisplayName("Should generate proper Equal Split email and SMS templates for debtor and payer")
    public void testEqualSplitTemplates() {
        Expense expense = new Expense();
        expense.setId(101);
        expense.setTitle("Team Dinner");
        expense.setAmountPaise(90000); // ₹900.00
        expense.setCategory(ExpenseCategory.FOOD);
        expense.setSplitMethod(SplitMethod.EQUAL);
        expense.setExpenseDate(LocalDate.of(2026, 11, 2));

        String groupName = "Goa Trip";
        String payerName = "Rahul";
        long participantShare = 30000; // ₹300.00
        String totalFormatted = CurrencyFormatter.format(expense.getAmountPaise()); // "₹ 900.00"
        String shareFormatted = CurrencyFormatter.format(participantShare);         // "₹ 300.00"

        // Test non-payer (debtor: Aman)
        String debtorEmailText = NotificationTemplates.buildExpenseEmailText(
                expense, "Aman", groupName, payerName, participantShare, false);
        assertNotNull(debtorEmailText);
        assertTrue(debtorEmailText.contains("Rahul added a new group expense in 'Goa Trip'"));
        assertTrue(debtorEmailText.contains("Team Dinner"));
        assertTrue(debtorEmailText.contains(totalFormatted));
        assertTrue(debtorEmailText.contains(shareFormatted));
        assertTrue(debtorEmailText.contains("Amount You Owe:      " + shareFormatted + " to Rahul"));

        String debtorEmailHtml = NotificationTemplates.buildExpenseEmailHtml(
                expense, "Aman", groupName, payerName, participantShare, false);
        assertTrue(debtorEmailHtml.contains("Hello <strong>Aman</strong>"));
        assertTrue(debtorEmailHtml.contains("Team Dinner"));
        assertTrue(debtorEmailHtml.contains(totalFormatted));
        assertTrue(debtorEmailHtml.contains(shareFormatted));

        String debtorSms = NotificationTemplates.buildExpenseSms(
                expense, groupName, payerName, participantShare, false);
        assertTrue(debtorSms.contains("Rahul paid " + totalFormatted + " for 'Team Dinner' in Goa Trip"));
        assertTrue(debtorSms.contains("Your share: " + shareFormatted + " (Owed to Rahul)"));

        // Test payer (Rahul)
        String payerEmailText = NotificationTemplates.buildExpenseEmailText(
                expense, "Rahul", groupName, payerName, participantShare, true);
        assertTrue(payerEmailText.contains("You recorded a new group expense in 'Goa Trip'"));
        assertFalse(payerEmailText.contains("Amount You Owe"));

        String payerSms = NotificationTemplates.buildExpenseSms(
                expense, groupName, payerName, participantShare, true);
        assertTrue(payerSms.contains("ExpenseSplitter: You logged 'Team Dinner'"));
        assertTrue(payerSms.contains(totalFormatted));
        assertTrue(payerSms.contains("Share: " + shareFormatted));
    }

    @Test
    @DisplayName("Should generate accurate templates for Exact and Percentage splits")
    public void testUnequalSplitTemplates() {
        Expense expense = new Expense();
        expense.setId(102);
        expense.setTitle("Electricity Bill");
        expense.setAmountPaise(250000); // ₹2500.00
        expense.setCategory(ExpenseCategory.ELECTRICITY);
        expense.setSplitMethod(SplitMethod.PERCENTAGE);
        expense.setExpenseDate(LocalDate.of(2026, 11, 1));

        long customShare = 150000; // ₹1500.00 (60%)
        String totalFormatted = CurrencyFormatter.format(expense.getAmountPaise()); // "₹ 2,500.00"
        String shareFormatted = CurrencyFormatter.format(customShare);              // "₹ 1,500.00"

        String emailText = NotificationTemplates.buildExpenseEmailText(
                expense, "Priya", "Apartment 402", "Alex", customShare, false);

        assertTrue(emailText.contains("Alex added a new group expense in 'Apartment 402'"));
        assertTrue(emailText.contains("Electricity Bill"));
        assertTrue(emailText.contains("Total Bill Amount:   " + totalFormatted));
        assertTrue(emailText.contains("Your Share:          " + shareFormatted));
        assertTrue(emailText.contains("Amount You Owe:      " + shareFormatted + " to Alex"));
    }

    @Test
    @DisplayName("Should generate Settlement Confirmation and Receipt templates for debtor and creditor")
    public void testSettlementTemplates() {
        Settlement settlement = new Settlement();
        settlement.setId(201);
        settlement.setAmountPaise(30000); // ₹300.00
        settlement.setSettlementDate(LocalDate.of(2026, 11, 2));
        settlement.setNotes("UPI Ref 99881122");
        String amountFormatted = CurrencyFormatter.format(settlement.getAmountPaise()); // "₹ 300.00"

        // 1. Debtor Confirmation
        String debtorEmail = NotificationTemplates.buildSettlementPayerEmailText(
                settlement, "Aman", "Rahul", "Goa Trip");
        assertTrue(debtorEmail.contains("Your payment of " + amountFormatted + " to Rahul for group 'Goa Trip'"));
        assertTrue(debtorEmail.contains("UPI Ref 99881122"));
        assertTrue(debtorEmail.contains("group debt balance has been automatically cleared"));

        String debtorSms = NotificationTemplates.buildSettlementPayerSms(
                settlement, "Aman", "Rahul", "Goa Trip");
        assertTrue(debtorSms.contains("Confirmed! Your payment of " + amountFormatted + " to Rahul for Goa Trip was recorded"));

        // 2. Creditor Receipt
        String creditorEmail = NotificationTemplates.buildSettlementReceiverEmailText(
                settlement, "Aman", "Rahul", "Goa Trip");
        assertTrue(creditorEmail.contains("Aman has recorded a settlement payment of " + amountFormatted + " to you in group 'Goa Trip'"));
        assertTrue(creditorEmail.contains("ledger has been automatically credited"));

        String creditorSms = NotificationTemplates.buildSettlementReceiverSms(
                settlement, "Aman", "Rahul", "Goa Trip");
        assertTrue(creditorSms.contains("Aman marked " + amountFormatted + " as paid to you for Goa Trip"));
    }

    @Test
    @DisplayName("Should enforce idempotency and prevent duplicate notifications via unique key")
    public void testIdempotencyProtection() {
        NotificationDao dao = new NotificationDao();
        String uniqueKey = "TEST_IDEMP_" + UUID.randomUUID().toString();

        assertFalse(dao.existsByIdempotencyKey(uniqueKey));

        Notification n1 = new Notification(
                1, "Test User", "test@example.com", NotificationChannel.EMAIL,
                "Test Subject", "Test Body", "TEST_SPLIT", uniqueKey
        );
        n1.setStatus(NotificationStatus.SENT);
        n1.setSentAt(LocalDateTime.now());

        boolean createdFirst = dao.create(n1);
        assertTrue(createdFirst, "First notification creation should succeed");
        assertTrue(dao.existsByIdempotencyKey(uniqueKey), "Idempotency key must now exist");

        // Duplicate attempt with identical idempotency key
        Notification n2 = new Notification(
                1, "Test User", "test@example.com", NotificationChannel.EMAIL,
                "Duplicate Subject", "Duplicate Body", "TEST_SPLIT", uniqueKey
        );
        boolean createdSecond = dao.create(n2);
        assertFalse(createdSecond, "Duplicate notification must be blocked by idempotency key constraint");
    }

    @Test
    @DisplayName("Should gracefully handle simulation mode and validate gateways")
    public void testSimulationModeAndGateways() {
        EmailGateway emailGateway = new EmailGateway();
        SmsGateway smsGateway = new SmsGateway();

        // Valid email in simulation/test mode succeeds safely
        DeliveryResult emailRes = emailGateway.sendEmail(
                "tester@example.com", "Test User", "Simulation Subject", "Simulation body", null);
        assertTrue(emailRes.isSuccess(), "Simulation email must succeed safely");
        assertNotNull(emailRes.getMessageId());
        assertTrue(emailRes.isSimulated());

        // Invalid email fails with clear validation error
        DeliveryResult invalidEmailRes = emailGateway.sendEmail(
                "not-an-email", "Test User", "Simulation Subject", "Simulation body", null);
        assertFalse(invalidEmailRes.isSuccess());
        assertTrue(invalidEmailRes.getErrorMessage().contains("Invalid recipient email"));

        // Valid international phone with country code in simulation mode succeeds safely
        DeliveryResult smsRes = smsGateway.sendSms("+919876543210", "Simulation SMS message");
        assertTrue(smsRes.isSuccess(), "Simulation SMS must succeed safely");
        assertNotNull(smsRes.getMessageId());
        assertTrue(smsRes.isSimulated());

        // Invalid phone without country code fails with helpful explanation
        DeliveryResult invalidSmsRes = smsGateway.sendSms("9876543210", "Simulation SMS message");
        assertFalse(invalidSmsRes.isSuccess());
        assertTrue(invalidSmsRes.getErrorMessage().contains("Must include country code"));
    }

    @Test
    @DisplayName("Should record delivery failure status and error details, and support retry")
    public void testFailureRecordingAndRetry() {
        NotificationDao dao = new NotificationDao();
        String testKey = "TEST_FAIL_" + UUID.randomUUID().toString();

        Notification n = new Notification(
                1, "Failing User", "invalid-destination", NotificationChannel.SMS,
                "SMS Alert", "Body", "ALERT", testKey
        );
        n.setStatus(NotificationStatus.PENDING);
        assertTrue(dao.create(n));

        // Mark as failed with gateway error
        boolean updatedFailed = dao.updateStatus(n.getId(), NotificationStatus.FAILED, "Connection timeout to gateway");
        assertTrue(updatedFailed);

        // Retry and mark as sent
        boolean updatedRetry = dao.updateStatus(n.getId(), NotificationStatus.SENT, null);
        assertTrue(updatedRetry);
    }
}
