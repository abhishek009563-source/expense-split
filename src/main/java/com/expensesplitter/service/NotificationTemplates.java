package com.expensesplitter.service;

import com.expensesplitter.model.Expense;
import com.expensesplitter.model.Settlement;
import com.expensesplitter.util.CurrencyFormatter;

import java.time.format.DateTimeFormatter;

public class NotificationTemplates {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy");

    /**
     * Plain-text email for newly created/split expense.
     */
    public static String buildExpenseEmailText(Expense expense, String recipientName, String groupName,
                                              String payerName, long participantSharePaise, boolean isPayer) {
        String dateStr = expense.getExpenseDate() != null ? expense.getExpenseDate().format(DATE_FORMATTER) : "Today";
        String totalFormatted = CurrencyFormatter.format(expense.getAmountPaise());
        String shareFormatted = CurrencyFormatter.format(participantSharePaise);

        StringBuilder sb = new StringBuilder();
        sb.append("Hello ").append(recipientName).append(",\n\n");
        if (isPayer) {
            sb.append("You recorded a new group expense in '").append(groupName).append("':\n\n");
        } else {
            sb.append(payerName).append(" added a new group expense in '").append(groupName).append("':\n\n");
        }

        sb.append("--------------------------------------------------\n");
        sb.append(" Expense Title:       ").append(expense.getTitle()).append("\n");
        sb.append(" Total Bill Amount:   ").append(totalFormatted).append("\n");
        sb.append(" Category:            ").append(expense.getCategory() != null ? expense.getCategory().getDisplayName() : "General").append("\n");
        sb.append(" Paid By:             ").append(payerName).append("\n");
        sb.append(" Group:               ").append(groupName).append("\n");
        sb.append(" Transaction Date:    ").append(dateStr).append("\n");
        sb.append(" Split Method:        ").append(expense.getSplitMethod() != null ? expense.getSplitMethod().getDisplayName() : "Equal").append("\n");
        sb.append(" Your Share:          ").append(shareFormatted).append("\n");

        if (!isPayer) {
            sb.append(" Amount You Owe:      ").append(shareFormatted).append(" to ").append(payerName).append("\n");
        }
        sb.append("--------------------------------------------------\n\n");

        if (expense.getDescription() != null && !expense.getDescription().isBlank()) {
            sb.append("Notes: ").append(expense.getDescription()).append("\n\n");
        }

        sb.append("Log into Expense Splitter to view your updated net balance ledger and settlement options.\n");
        sb.append("Best regards,\nExpense Splitter Automated Financial Dispatcher");

        return sb.toString();
    }

    /**
     * HTML email for newly created/split expense.
     */
    public static String buildExpenseEmailHtml(Expense expense, String recipientName, String groupName,
                                              String payerName, long participantSharePaise, boolean isPayer) {
        String dateStr = expense.getExpenseDate() != null ? expense.getExpenseDate().format(DATE_FORMATTER) : "Today";
        String totalFormatted = CurrencyFormatter.format(expense.getAmountPaise());
        String shareFormatted = CurrencyFormatter.format(participantSharePaise);

        return """
            <!DOCTYPE html>
            <html>
            <head><meta charset="utf-8"></head>
            <body style="font-family:'Segoe UI',Helvetica,Arial,sans-serif; background-color:#f1f5f9; padding:20px; color:#1e293b;">
              <div style="max-width:560px; margin:0 auto; background:#ffffff; border-radius:12px; overflow:hidden; border:1px solid #e2e8f0; box-shadow:0 4px 12px rgba(0,0,0,0.05);">
                <div style="background:linear-gradient(135deg, #1e3a8a, #3b82f6); padding:24px; color:#ffffff;">
                  <h2 style="margin:0; font-size:20px; font-weight:800;">⚡ Expense Splitter</h2>
                  <p style="margin:4px 0 0; font-size:13px; opacity:0.9;">Group Expense Notification</p>
                </div>
                <div style="padding:24px;">
                  <p style="font-size:14px; margin-top:0;">Hello <strong>%s</strong>,</p>
                  <p style="font-size:13px; color:#475569;">
                    %s added a new bill to <strong>%s</strong>.
                  </p>
                  
                  <div style="background:#f8fafc; border:1px solid #e2e8f0; border-radius:8px; padding:16px; margin:18px 0;">
                    <div style="font-size:11px; text-transform:uppercase; font-weight:bold; color:#64748b;">Expense Title</div>
                    <div style="font-size:17px; font-weight:bold; color:#0f172a; margin-bottom:12px;">%s</div>

                    <table style="width:100%%; font-size:13px; border-collapse:collapse;">
                      <tr>
                        <td style="padding:4px 0; color:#64748b;">Total Bill:</td>
                        <td style="padding:4px 0; font-weight:bold; text-align:right;">%s</td>
                      </tr>
                      <tr>
                        <td style="padding:4px 0; color:#64748b;">Paid By:</td>
                        <td style="padding:4px 0; font-weight:bold; text-align:right;">%s</td>
                      </tr>
                      <tr>
                        <td style="padding:4px 0; color:#64748b;">Date:</td>
                        <td style="padding:4px 0; text-align:right;">%s</td>
                      </tr>
                      <tr style="border-top:1px dashed #cbd5e1;">
                        <td style="padding:8px 0 4px; color:#1e3a8a; font-weight:bold;">Your Share:</td>
                        <td style="padding:8px 0 4px; font-size:16px; font-weight:800; color:#2563eb; text-align:right;">%s</td>
                      </tr>
                    </table>
                  </div>

                  <p style="font-size:12px; color:#64748b; margin-bottom:0;">
                    Open Expense Splitter to see your group balance ledger and minimize debts.
                  </p>
                </div>
              </div>
            </body>
            </html>
            """.formatted(
                recipientName,
                isPayer ? "You" : payerName,
                groupName,
                expense.getTitle(),
                totalFormatted,
                payerName,
                dateStr,
                shareFormatted
        );
    }

    /**
     * Concise SMS message for expense split.
     */
    public static String buildExpenseSms(Expense expense, String groupName, String payerName,
                                        long participantSharePaise, boolean isPayer) {
        String totalFormatted = CurrencyFormatter.format(expense.getAmountPaise());
        String shareFormatted = CurrencyFormatter.format(participantSharePaise);

        if (isPayer) {
            return String.format("ExpenseSplitter: You logged '%s' (%s) in %s. Share: %s.",
                    expense.getTitle(), totalFormatted, groupName, shareFormatted);
        } else {
            return String.format("ExpenseSplitter: %s paid %s for '%s' in %s. Your share: %s (Owed to %s).",
                    payerName, totalFormatted, expense.getTitle(), groupName, shareFormatted, payerName);
        }
    }

    /**
     * Settlement Confirmation for Debtor (Payer of settlement).
     */
    public static String buildSettlementPayerEmailText(Settlement s, String debtorName, String creditorName, String groupName) {
        String amountFormatted = CurrencyFormatter.format(s.getAmountPaise());
        String dateStr = s.getSettlementDate() != null ? s.getSettlementDate().format(DATE_FORMATTER) : "Today";

        return String.format("""
            Hello %s,

            Your payment of %s to %s for group '%s' has been successfully recorded and verified.

            Settlement Details:
            --------------------------------------------------
             Amount Paid:         %s
             Paid To:             %s
             Group:               %s
             Payment Date:        %s
             Notes / Reference:   %s
            --------------------------------------------------

            Your group debt balance has been automatically cleared.

            Best regards,
            Expense Splitter Team
            """, debtorName, amountFormatted, creditorName, groupName, amountFormatted, creditorName, groupName, dateStr,
                s.getNotes() != null && !s.getNotes().isBlank() ? s.getNotes() : "Direct Payment");
    }

    public static String buildSettlementPayerSms(Settlement s, String debtorName, String creditorName, String groupName) {
        String amountFormatted = CurrencyFormatter.format(s.getAmountPaise());
        return String.format("ExpenseSplitter: Confirmed! Your payment of %s to %s for %s was recorded. Balances updated.",
                amountFormatted, creditorName, groupName);
    }

    /**
     * Settlement Notification for Creditor (Receiver of settlement).
     */
    public static String buildSettlementReceiverEmailText(Settlement s, String debtorName, String creditorName, String groupName) {
        String amountFormatted = CurrencyFormatter.format(s.getAmountPaise());
        String dateStr = s.getSettlementDate() != null ? s.getSettlementDate().format(DATE_FORMATTER) : "Today";

        return String.format("""
            Hello %s,

            %s has recorded a settlement payment of %s to you in group '%s'.

            Settlement Details:
            --------------------------------------------------
             Amount Received:     %s
             Paid By:             %s
             Group:               %s
             Payment Date:        %s
             Notes / Reference:   %s
            --------------------------------------------------

            Your group ledger has been automatically credited.

            Best regards,
            Expense Splitter Team
            """, creditorName, debtorName, amountFormatted, groupName, amountFormatted, debtorName, groupName, dateStr,
                s.getNotes() != null && !s.getNotes().isBlank() ? s.getNotes() : "Direct Payment");
    }

    public static String buildSettlementReceiverSms(Settlement s, String debtorName, String creditorName, String groupName) {
        String amountFormatted = CurrencyFormatter.format(s.getAmountPaise());
        return String.format("ExpenseSplitter: %s marked %s as paid to you for %s. Your ledger is credited.",
                debtorName, amountFormatted, groupName);
    }
}

