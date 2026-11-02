package com.expensesplitter.dao;

import com.expensesplitter.database.DatabaseManager;
import com.expensesplitter.model.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UserDao {

    public Optional<User> findById(int id) {
        String sql = "SELECT id, username, full_name, email, password_hash, phone, email_notifications_enabled, sms_notifications_enabled, created_at, updated_at FROM users WHERE id = ?;";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToUser(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    public Optional<User> findByUsername(String username) {
        String sql = "SELECT id, username, full_name, email, password_hash, phone, email_notifications_enabled, sms_notifications_enabled, created_at, updated_at FROM users WHERE LOWER(username) = LOWER(?);";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToUser(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    public Optional<User> findByEmail(String email) {
        String sql = "SELECT id, username, full_name, email, password_hash, phone, email_notifications_enabled, sms_notifications_enabled, created_at, updated_at FROM users WHERE LOWER(email) = LOWER(?);";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToUser(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    public List<User> findAll() {
        List<User> list = new ArrayList<>();
        String sql = "SELECT id, username, full_name, email, password_hash, phone, email_notifications_enabled, sms_notifications_enabled, created_at, updated_at FROM users ORDER BY full_name ASC;";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                list.add(mapResultSetToUser(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public boolean create(User user) {
        String sql = "INSERT INTO users (username, full_name, email, password_hash, phone, email_notifications_enabled, sms_notifications_enabled) VALUES (?, ?, ?, ?, ?, ?, ?);";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, user.getUsername().trim());
            stmt.setString(2, user.getFullName().trim());
            stmt.setString(3, user.getEmail().trim().toLowerCase());
            stmt.setString(4, user.getPasswordHash());
            stmt.setString(5, user.getPhone() != null ? user.getPhone().trim() : null);
            stmt.setInt(6, user.isEmailNotificationsEnabled() ? 1 : 0);
            stmt.setInt(7, user.isSmsNotificationsEnabled() ? 1 : 0);

            int affected = stmt.executeUpdate();
            if (affected > 0) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        user.setId(generatedKeys.getInt(1));
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean update(User user) {
        String sql = "UPDATE users SET full_name = ?, email = ?, phone = ?, email_notifications_enabled = ?, sms_notifications_enabled = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?;";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, user.getFullName().trim());
            stmt.setString(2, user.getEmail().trim().toLowerCase());
            stmt.setString(3, user.getPhone() != null ? user.getPhone().trim() : null);
            stmt.setInt(4, user.isEmailNotificationsEnabled() ? 1 : 0);
            stmt.setInt(5, user.isSmsNotificationsEnabled() ? 1 : 0);
            stmt.setInt(6, user.getId());

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean updatePassword(int userId, String newPasswordHash) {
        String sql = "UPDATE users SET password_hash = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?;";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, newPasswordHash);
            stmt.setInt(2, userId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        User u = new User();
        u.setId(rs.getInt("id"));
        u.setUsername(rs.getString("username"));
        u.setFullName(rs.getString("full_name"));
        u.setEmail(rs.getString("email"));
        u.setPasswordHash(rs.getString("password_hash"));
        u.setPhone(rs.getString("phone"));

        try {
            u.setEmailNotificationsEnabled(rs.getInt("email_notifications_enabled") == 1);
            u.setSmsNotificationsEnabled(rs.getInt("sms_notifications_enabled") == 1);
        } catch (SQLException ignored) {
            u.setEmailNotificationsEnabled(true);
            u.setSmsNotificationsEnabled(true);
        }

        return u;
    }
}
