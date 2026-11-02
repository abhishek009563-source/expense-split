package com.expensesplitter.service;

import com.expensesplitter.dao.GroupDao;
import com.expensesplitter.dao.SettlementDao;
import com.expensesplitter.dao.UserDao;
import com.expensesplitter.model.Group;
import com.expensesplitter.model.Settlement;
import com.expensesplitter.model.User;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class SettlementService {

    private final SettlementDao settlementDao = new SettlementDao();
    private final GroupDao groupDao = new GroupDao();
    private final UserDao userDao = new UserDao();
    private final NotificationDispatcher notificationDispatcher = new NotificationDispatcher();

    public boolean recordSettlement(int groupId, int payerUserId, int receiverUserId, long amountPaise,
                                    LocalDate date, String notes) {
        if (payerUserId == receiverUserId) {
            throw new IllegalArgumentException("Payer and receiver cannot be the same user.");
        }
        if (amountPaise <= 0) {
            throw new IllegalArgumentException("Settlement amount must be greater than zero.");
        }

        Settlement s = new Settlement();
        s.setGroupId(groupId);
        s.setPayerUserId(payerUserId);
        s.setReceiverUserId(receiverUserId);
        s.setAmountPaise(amountPaise);
        s.setSettlementDate(date != null ? date : LocalDate.now());
        s.setNotes(notes != null ? notes.trim() : "");

        boolean created = settlementDao.create(s);
        if (created) {
            Optional<Group> g = groupDao.findById(groupId);
            String groupName = g.map(Group::getName).orElse("Group");
            Optional<User> payer = userDao.findById(payerUserId);
            Optional<User> receiver = userDao.findById(receiverUserId);
            String payerName = payer.map(User::getFullName).orElse("Payer");
            String receiverName = receiver.map(User::getFullName).orElse("Receiver");

            // Dispatch automatic settlement notifications (In-App, Email, SMS) asynchronously
            notificationDispatcher.dispatchSettlementNotificationsAsync(s, groupName, payerName, receiverName);
        }
        return created;
    }

    public boolean deleteSettlement(int settlementId) {
        return settlementDao.delete(settlementId);
    }

    public List<Settlement> getGroupSettlements(int groupId) {
        return settlementDao.findByGroupId(groupId);
    }

    public List<Settlement> getUserSettlements(int userId) {
        return settlementDao.findByUserId(userId);
    }

    public List<Settlement> getRecentSettlements(int userId, int limit) {
        return settlementDao.findRecentByUserId(userId, limit);
    }
}
