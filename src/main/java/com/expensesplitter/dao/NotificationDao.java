package com.expensesplitter.dao;

import com.expensesplitter.database.DatabaseManager;
import com.expensesplitter.model.Notification;
import com.expensesplitter.model.NotificationChannel;
import com.expensesplitter.model.NotificationStatus;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class NotificationDao {

    public boolean create(Notification n) {
        String sql = """
            INSERT INTO notifications (user_id, recipient_name, recipient_contact, channel, title, message, type, status, error_message, retry_count, idempotency_key, sent_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);
        """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, n.getUserId());
            stmt.setString(2, n.getRecipientName());
            stmt.setString(3, n.getRecipientContact());
            stmt.setString(4, n.getChannel() != null ? n.getChannel().name() : NotificationChannel.IN_APP.name());
            stmt.setString(5, n.getTitle().trim());
            stmt.setString(6, n.getMessage().trim());
            stmt.setString(7, n.getType() != null ? n.getType() : "SYSTEM");
            stmt.setString(8, n.getStatus() != null ? n.getStatus().name() : NotificationStatus.SENT.name());
            stmt.setString(9, n.getErrorMessage());
            stmt.setInt(10, n.getRetryCount());
            stmt.setString(11, n.getIdempotencyKey());

            if (n.getSentAt() != null) {
                stmt.setTimestamp(12, Timestamp.valueOf(n.getSentAt()));
            } else if (n.getStatus() == NotificationStatus.SENT) {
                stmt.setTimestamp(12, Timestamp.valueOf(LocalDateTime.now()));
            } else {
                stmt.setNull(12, Types.TIMESTAMP);
            }

            int affected = stmt.executeUpdate();
            if (affected > 0) {
                try (ResultSet keys = stmt.getGeneratedKeys()) {
                    if (keys.next()) {
                        n.setId(keys.getInt(1));
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            // Check for unique constraint violation on idempotency_key (avoiding duplicate notifications)
            if (e.getMessage() != null && e.getMessage().contains("UNIQUE constraint failed")) {
                System.out.println("[Idempotency] Duplicate notification prevented: " + n.getIdempotencyKey());
                return false;
            }
            e.printStackTrace();
        }
        return false;
    }

    public boolean existsByIdempotencyKey(String key) {
        if (key == null || key.isBlank()) return false;
        String sql = "SELECT 1 FROM notifications WHERE idempotency_key = ? LIMIT 1;";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, key.trim());
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean updateStatus(int id, NotificationStatus status, String errorMessage) {
        String sql = """
            UPDATE notifications
            SET status = ?, error_message = ?, sent_at = CASE WHEN ? = 'SENT' THEN CURRENT_TIMESTAMP ELSE sent_at END, retry_count = retry_count + 1
            WHERE id = ?;
        """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, status.name());
            stmt.setString(2, errorMessage);
            stmt.setString(3, status.name());
            stmt.setInt(4, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public List<Notification> findByUserId(int userId) {
        return findAllWithFilters(userId, null, null, null);
    }

    public List<Notification> findAllWithFilters(Integer userId, String channel, String status, String searchQuery) {
        List<Notification> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
            SELECT id, user_id, recipient_name, recipient_contact, channel, title, message, type,
                   status, error_message, retry_count, idempotency_key, is_read, sent_at, created_at
            FROM notifications
            WHERE 1=1
        """);

        List<Object> params = new ArrayList<>();

        if (userId != null && userId > 0) {
            sql.append(" AND user_id = ?");
            params.add(userId);
        }

        if (channel != null && !channel.equalsIgnoreCase("ALL") && !channel.isBlank()) {
            sql.append(" AND UPPER(channel) = ?");
            params.add(channel.trim().toUpperCase());
        }

        if (status != null && !status.equalsIgnoreCase("ALL") && !status.isBlank()) {
            sql.append(" AND UPPER(status) = ?");
            params.add(status.trim().toUpperCase());
        }

        if (searchQuery != null && !searchQuery.isBlank()) {
            sql.append(" AND (LOWER(title) LIKE ? OR LOWER(message) LIKE ? OR LOWER(recipient_name) LIKE ? OR LOWER(recipient_contact) LIKE ?)");
            String q = "%" + searchQuery.trim().toLowerCase() + "%";
            params.add(q);
            params.add(q);
            params.add(q);
            params.add(q);
        }

        sql.append(" ORDER BY id DESC;");

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToNotification(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public Notification findById(int id) {
        String sql = "SELECT * FROM notifications WHERE id = ?;";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToNotification(rs);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean markAsRead(int notificationId) {
        String sql = "UPDATE notifications SET is_read = 1 WHERE id = ?;";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, notificationId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean markAllAsRead(int userId) {
        String sql = "UPDATE notifications SET is_read = 1 WHERE user_id = ?;";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public int getUnreadCount(int userId) {
        String sql = "SELECT COUNT(*) FROM notifications WHERE user_id = ? AND is_read = 0;";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * Returns stats array: [totalCount, emailsSent, smsSent, failedDeliveries]
     */
    public int[] getDeliveryStats(int userId) {
        int total = 0;
        int emails = 0;
        int sms = 0;
        int failed = 0;

        String sql = """
            SELECT
                COUNT(*) as total,
                SUM(CASE WHEN channel = 'EMAIL' AND status = 'SENT' THEN 1 ELSE 0 END) as emails,
                SUM(CASE WHEN channel = 'SMS' AND status = 'SENT' THEN 1 ELSE 0 END) as sms,
                SUM(CASE WHEN status = 'FAILED' THEN 1 ELSE 0 END) as failed
            FROM notifications
            WHERE user_id = ?;
        """;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    total = rs.getInt("total");
                    emails = rs.getInt("emails");
                    sms = rs.getInt("sms");
                    failed = rs.getInt("failed");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return new int[]{total, emails, sms, failed};
    }

    private Notification mapResultSetToNotification(ResultSet rs) throws SQLException {
        Notification n = new Notification();
        n.setId(rs.getInt("id"));
        n.setUserId(rs.getInt("user_id"));
        n.setRecipientName(rs.getString("recipient_name"));
        n.setRecipientContact(rs.getString("recipient_contact"));
        n.setChannel(NotificationChannel.fromString(rs.getString("channel")));
        n.setTitle(rs.getString("title"));
        n.setMessage(rs.getString("message"));
        n.setType(rs.getString("type"));
        n.setStatus(NotificationStatus.fromString(rs.getString("status")));
        n.setErrorMessage(rs.getString("error_message"));
        n.setRetryCount(rs.getInt("retry_count"));
        n.setIdempotencyKey(rs.getString("idempotency_key"));
        n.setRead(rs.getInt("is_read") == 1);

        Timestamp sentTs = rs.getTimestamp("sent_at");
        if (sentTs != null) n.setSentAt(sentTs.toLocalDateTime());

        Timestamp createdTs = rs.getTimestamp("created_at");
        if (createdTs != null) n.setCreatedAt(createdTs.toLocalDateTime());

        return n;
    }
}
