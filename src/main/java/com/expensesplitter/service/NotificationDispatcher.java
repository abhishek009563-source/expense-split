package com.expensesplitter.service;

import com.expensesplitter.dao.NotificationDao;
import com.expensesplitter.dao.UserDao;
import com.expensesplitter.model.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NotificationDispatcher {

    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(4, r -> {
        Thread t = new Thread(r, "Notification-Worker");
        t.setDaemon(true);
        return t;
    });

    private final NotificationDao notificationDao;
    private final UserDao userDao;
    private final EmailGateway emailGateway;
    private final SmsGateway smsGateway;

    public NotificationDispatcher() {
        this.notificationDao = new NotificationDao();
        this.userDao = new UserDao();
        this.emailGateway = new EmailGateway();
        this.smsGateway = new SmsGateway();
    }

    // For unit testing with mocks/fakes
    public NotificationDispatcher(NotificationDao notificationDao, UserDao userDao,
                                  EmailGateway emailGateway, SmsGateway smsGateway) {
        this.notificationDao = notificationDao;
        this.userDao = userDao;
        this.emailGateway = emailGateway;
        this.smsGateway = smsGateway;
    }

    /**
     * Dispatches automatic Email and SMS notifications to all affected participants
     * when a group expense is created or split.
     * Runs asynchronously so expense creation is never delayed or blocked.
     */
    public void dispatchExpenseNotificationsAsync(Expense expense, List<ExpenseParticipant> participants,
                                                 String groupName, String payerName) {
        EXECUTOR.submit(() -> {
            try {
                dispatchExpenseNotificationsSync(expense, participants, groupName, payerName);
            } catch (Exception e) {
                System.err.println("[NotificationDispatcher Error] Failed during expense async dispatch: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    public void dispatchExpenseNotificationsSync(Expense expense, List<ExpenseParticipant> participants,
                                                String groupName, String payerName) {
        if (participants == null || participants.isEmpty()) return;

        for (ExpenseParticipant p : participants) {
            Optional<User> userOpt = userDao.findById(p.getUserId());
            if (userOpt.isEmpty()) continue;
            User user = userOpt.get();

            boolean isPayer = (user.getId() == expense.getPaidByUserId());

            // 1. In-App Notification (always generated)
            String inAppKey = "EXPENSE_" + expense.getId() + "_IN_APP_" + user.getId();
            if (!notificationDao.existsByIdempotencyKey(inAppKey)) {
                String title = isPayer ? "Expense Logged: " + expense.getTitle() : "New Bill: " + expense.getTitle();
                String msg = NotificationTemplates.buildExpenseSms(expense, groupName, payerName, p.getShareAmountPaise(), isPayer);
                Notification nInApp = new Notification(
                        user.getId(), user.getFullName(), user.getEmail(),
                        NotificationChannel.IN_APP, title, msg, "EXPENSE_SPLIT", inAppKey
                );
                nInApp.setStatus(NotificationStatus.SENT);
                nInApp.setSentAt(LocalDateTime.now());
                notificationDao.create(nInApp);
            }

            // 2. Email Notification (if consent enabled and valid email present)
            if (user.isEmailNotificationsEnabled() && user.getEmail() != null && !user.getEmail().isBlank()) {
                String emailKey = "EXPENSE_" + expense.getId() + "_EMAIL_" + user.getId();
                if (!notificationDao.existsByIdempotencyKey(emailKey)) {
                    String subject = "[ExpenseSplitter] New Bill: " + expense.getTitle() + " in " + groupName;
                    String bodyText = NotificationTemplates.buildExpenseEmailText(expense, user.getFullName(), groupName, payerName, p.getShareAmountPaise(), isPayer);
                    String bodyHtml = NotificationTemplates.buildExpenseEmailHtml(expense, user.getFullName(), groupName, payerName, p.getShareAmountPaise(), isPayer);

                    Notification nEmail = new Notification(
                            user.getId(), user.getFullName(), user.getEmail(),
                            NotificationChannel.EMAIL, subject, bodyText, "EXPENSE_SPLIT", emailKey
                    );
                    notificationDao.create(nEmail);

                    // Execute delivery
                    DeliveryResult res = emailGateway.sendEmail(user.getEmail(), user.getFullName(), subject, bodyText, bodyHtml);
                    if (res.isSuccess()) {
                        notificationDao.updateStatus(nEmail.getId(), NotificationStatus.SENT, null);
                    } else {
                        notificationDao.updateStatus(nEmail.getId(), NotificationStatus.FAILED, res.getErrorMessage());
                    }
                }
            }

            // 3. SMS Notification (if consent enabled and valid phone present)
            if (user.isSmsNotificationsEnabled() && user.getPhone() != null && !user.getPhone().isBlank()) {
                String smsKey = "EXPENSE_" + expense.getId() + "_SMS_" + user.getId();
                if (!notificationDao.existsByIdempotencyKey(smsKey)) {
                    String smsBody = NotificationTemplates.buildExpenseSms(expense, groupName, payerName, p.getShareAmountPaise(), isPayer);
                    Notification nSms = new Notification(
                            user.getId(), user.getFullName(), user.getPhone(),
                            NotificationChannel.SMS, "Expense Alert: " + expense.getTitle(), smsBody, "EXPENSE_SPLIT", smsKey
                    );
                    notificationDao.create(nSms);

                    // Execute delivery
                    DeliveryResult res = smsGateway.sendSms(user.getPhone(), smsBody);
                    if (res.isSuccess()) {
                        notificationDao.updateStatus(nSms.getId(), NotificationStatus.SENT, null);
                    } else {
                        notificationDao.updateStatus(nSms.getId(), NotificationStatus.FAILED, res.getErrorMessage());
                    }
                }
            }
        }
    }

    /**
     * Dispatches automatic settlement notifications to both Debtor (Payer) and Creditor (Receiver).
     */
    public void dispatchSettlementNotificationsAsync(Settlement settlement, String groupName,
                                                    String payerName, String receiverName) {
        EXECUTOR.submit(() -> {
            try {
                dispatchSettlementNotificationsSync(settlement, groupName, payerName, receiverName);
            } catch (Exception e) {
                System.err.println("[NotificationDispatcher Error] Failed during settlement async dispatch: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    public void dispatchSettlementNotificationsSync(Settlement settlement, String groupName,
                                                   String payerName, String receiverName) {
        Optional<User> debtorOpt = userDao.findById(settlement.getPayerUserId());
        Optional<User> creditorOpt = userDao.findById(settlement.getReceiverUserId());

        // 1. Notify Debtor (Confirmation)
        if (debtorOpt.isPresent()) {
            User debtor = debtorOpt.get();
            String emailKey = "SETTLE_CONFIRM_" + settlement.getId() + "_EMAIL_" + debtor.getId();
            String smsKey = "SETTLE_CONFIRM_" + settlement.getId() + "_SMS_" + debtor.getId();

            // Email
            if (debtor.isEmailNotificationsEnabled() && debtor.getEmail() != null && !debtor.getEmail().isBlank()) {
                if (!notificationDao.existsByIdempotencyKey(emailKey)) {
                    String subj = "[ExpenseSplitter] Settlement Confirmed for " + groupName;
                    String body = NotificationTemplates.buildSettlementPayerEmailText(settlement, debtor.getFullName(), receiverName, groupName);
                    Notification n = new Notification(debtor.getId(), debtor.getFullName(), debtor.getEmail(),
                            NotificationChannel.EMAIL, subj, body, "SETTLEMENT_CONFIRMATION", emailKey);
                    notificationDao.create(n);
                    DeliveryResult res = emailGateway.sendEmail(debtor.getEmail(), debtor.getFullName(), subj, body, body);
                    notificationDao.updateStatus(n.getId(), res.isSuccess() ? NotificationStatus.SENT : NotificationStatus.FAILED, res.getErrorMessage());
                }
            }

            // SMS
            if (debtor.isSmsNotificationsEnabled() && debtor.getPhone() != null && !debtor.getPhone().isBlank()) {
                if (!notificationDao.existsByIdempotencyKey(smsKey)) {
                    String msg = NotificationTemplates.buildSettlementPayerSms(settlement, debtor.getFullName(), receiverName, groupName);
                    Notification n = new Notification(debtor.getId(), debtor.getFullName(), debtor.getPhone(),
                            NotificationChannel.SMS, "Settlement Confirmed", msg, "SETTLEMENT_CONFIRMATION", smsKey);
                    notificationDao.create(n);
                    DeliveryResult res = smsGateway.sendSms(debtor.getPhone(), msg);
                    notificationDao.updateStatus(n.getId(), res.isSuccess() ? NotificationStatus.SENT : NotificationStatus.FAILED, res.getErrorMessage());
                }
            }
        }

        // 2. Notify Creditor (Payment Received)
        if (creditorOpt.isPresent()) {
            User creditor = creditorOpt.get();
            String emailKey = "SETTLE_REC_" + settlement.getId() + "_EMAIL_" + creditor.getId();
            String smsKey = "SETTLE_REC_" + settlement.getId() + "_SMS_" + creditor.getId();

            // Email
            if (creditor.isEmailNotificationsEnabled() && creditor.getEmail() != null && !creditor.getEmail().isBlank()) {
                if (!notificationDao.existsByIdempotencyKey(emailKey)) {
                    String subj = "[ExpenseSplitter] Payment Received from " + payerName;
                    String body = NotificationTemplates.buildSettlementReceiverEmailText(settlement, payerName, creditor.getFullName(), groupName);
                    Notification n = new Notification(creditor.getId(), creditor.getFullName(), creditor.getEmail(),
                            NotificationChannel.EMAIL, subj, body, "SETTLEMENT_RECEIVED", emailKey);
                    notificationDao.create(n);
                    DeliveryResult res = emailGateway.sendEmail(creditor.getEmail(), creditor.getFullName(), subj, body, body);
                    notificationDao.updateStatus(n.getId(), res.isSuccess() ? NotificationStatus.SENT : NotificationStatus.FAILED, res.getErrorMessage());
                }
            }

            // SMS
            if (creditor.isSmsNotificationsEnabled() && creditor.getPhone() != null && !creditor.getPhone().isBlank()) {
                if (!notificationDao.existsByIdempotencyKey(smsKey)) {
                    String msg = NotificationTemplates.buildSettlementReceiverSms(settlement, payerName, creditor.getFullName(), groupName);
                    Notification n = new Notification(creditor.getId(), creditor.getFullName(), creditor.getPhone(),
                            NotificationChannel.SMS, "Payment Received", msg, "SETTLEMENT_RECEIVED", smsKey);
                    notificationDao.create(n);
                    DeliveryResult res = smsGateway.sendSms(creditor.getPhone(), msg);
                    notificationDao.updateStatus(n.getId(), res.isSuccess() ? NotificationStatus.SENT : NotificationStatus.FAILED, res.getErrorMessage());
                }
            }
        }
    }

    /**
     * Retries a failed or pending notification delivery.
     */
    public boolean retryNotification(int notificationId) {
        Notification n = notificationDao.findById(notificationId);
        if (n == null) return false;

        DeliveryResult res;
        if (n.getChannel() == NotificationChannel.EMAIL) {
            res = emailGateway.sendEmail(n.getRecipientContact(), n.getRecipientName(), n.getTitle(), n.getMessage(), n.getMessage());
        } else if (n.getChannel() == NotificationChannel.SMS) {
            res = smsGateway.sendSms(n.getRecipientContact(), n.getMessage());
        } else {
            res = DeliveryResult.success("IN_APP", false);
        }

        if (res.isSuccess()) {
            notificationDao.updateStatus(notificationId, NotificationStatus.SENT, null);
            return true;
        } else {
            notificationDao.updateStatus(notificationId, NotificationStatus.FAILED, res.getErrorMessage());
            return false;
        }
    }
}

