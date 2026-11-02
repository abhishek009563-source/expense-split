package com.expensesplitter.service;

import com.expensesplitter.dao.NotificationDao;
import com.expensesplitter.model.Notification;
import com.expensesplitter.model.NotificationChannel;
import com.expensesplitter.model.NotificationStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class NotificationService {

    private final NotificationDao notificationDao;
    private final NotificationDispatcher dispatcher;

    public NotificationService() {
        this.notificationDao = new NotificationDao();
        this.dispatcher = new NotificationDispatcher();
    }

    public NotificationService(NotificationDao notificationDao, NotificationDispatcher dispatcher) {
        this.notificationDao = notificationDao;
        this.dispatcher = dispatcher;
    }

    public List<Notification> getUserNotifications(int userId) {
        return notificationDao.findByUserId(userId);
    }

    public List<Notification> getFilteredNotifications(Integer userId, String channel, String status, String search) {
        return notificationDao.findAllWithFilters(userId, channel, status, search);
    }

    public boolean markAsRead(int notificationId) {
        return notificationDao.markAsRead(notificationId);
    }

    public boolean markAllAsRead(int userId) {
        return notificationDao.markAllAsRead(userId);
    }

    public int getUnreadCount(int userId) {
        return notificationDao.getUnreadCount(userId);
    }

    public int[] getDeliveryStats(int userId) {
        return notificationDao.getDeliveryStats(userId);
    }

    public boolean retryNotification(int notificationId) {
        return dispatcher.retryNotification(notificationId);
    }

    public NotificationDispatcher getDispatcher() {
        return dispatcher;
    }

    public boolean sendTestNotification(int userId, NotificationChannel channel, String targetContact, String message) {
        String testKey = "TEST_" + channel.name() + "_" + UUID.randomUUID().toString().substring(0, 8);
        Notification n = new Notification(
                userId, "Test User", targetContact,
                channel, "Test Alert: " + channel.getDisplayName(), message, "TEST", testKey
        );
        n.setStatus(NotificationStatus.PENDING);
        n.setCreatedAt(LocalDateTime.now());
        notificationDao.create(n);

        if (channel == NotificationChannel.EMAIL) {
            EmailGateway eg = new EmailGateway();
            DeliveryResult res = eg.sendEmail(targetContact, "Test User", "Test Alert", message, message);
            notificationDao.updateStatus(n.getId(), res.isSuccess() ? NotificationStatus.SENT : NotificationStatus.FAILED, res.getErrorMessage());
            return res.isSuccess();
        } else if (channel == NotificationChannel.SMS) {
            SmsGateway sg = new SmsGateway();
            DeliveryResult res = sg.sendSms(targetContact, message);
            notificationDao.updateStatus(n.getId(), res.isSuccess() ? NotificationStatus.SENT : NotificationStatus.FAILED, res.getErrorMessage());
            return res.isSuccess();
        } else {
            notificationDao.updateStatus(n.getId(), NotificationStatus.SENT, null);
            return true;
        }
    }
}
