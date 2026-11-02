package com.expensesplitter.dao;

import com.expensesplitter.database.DatabaseManager;
import com.expensesplitter.model.Settlement;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class SettlementDao {

    public boolean create(Settlement settlement) {
        String sql = """
            INSERT INTO settlements (group_id, payer_user_id, receiver_user_id, amount_paise, settlement_date, notes)
            VALUES (?, ?, ?, ?, ?, ?);
        """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, settlement.getGroupId());
            stmt.setInt(2, settlement.getPayerUserId());
            stmt.setInt(3, settlement.getReceiverUserId());
            stmt.setLong(4, settlement.getAmountPaise());
            stmt.setDate(5, Date.valueOf(settlement.getSettlementDate() != null ? settlement.getSettlementDate() : LocalDate.now()));
            stmt.setString(6, settlement.getNotes() != null ? settlement.getNotes().trim() : "");

            int affected = stmt.executeUpdate();
            if (affected > 0) {
                try (ResultSet keys = stmt.getGeneratedKeys()) {
                    if (keys.next()) {
                        settlement.setId(keys.getInt(1));
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean delete(int settlementId) {
        String sql = "DELETE FROM settlements WHERE id = ?;";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, settlementId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public List<Settlement> findByGroupId(int groupId) {
        List<Settlement> list = new ArrayList<>();
        String sql = """
            SELECT s.id, s.group_id, s.payer_user_id, s.receiver_user_id, s.amount_paise,
                   s.settlement_date, s.notes, s.created_at,
                   g.name as group_name,
                   u1.full_name as payer_name,
                   u2.full_name as receiver_name
            FROM settlements s
            JOIN groups g ON s.group_id = g.id
            JOIN users u1 ON s.payer_user_id = u1.id
            JOIN users u2 ON s.receiver_user_id = u2.id
            WHERE s.group_id = ?
            ORDER BY s.settlement_date DESC, s.id DESC;
        """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, groupId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToSettlement(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public List<Settlement> findByUserId(int userId) {
        List<Settlement> list = new ArrayList<>();
        String sql = """
            SELECT s.id, s.group_id, s.payer_user_id, s.receiver_user_id, s.amount_paise,
                   s.settlement_date, s.notes, s.created_at,
                   g.name as group_name,
                   u1.full_name as payer_name,
                   u2.full_name as receiver_name
            FROM settlements s
            JOIN groups g ON s.group_id = g.id
            JOIN users u1 ON s.payer_user_id = u1.id
            JOIN users u2 ON s.receiver_user_id = u2.id
            WHERE s.payer_user_id = ? OR s.receiver_user_id = ?
            ORDER BY s.settlement_date DESC, s.id DESC;
        """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToSettlement(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public List<Settlement> findRecentByUserId(int userId, int limit) {
        List<Settlement> list = new ArrayList<>();
        String sql = """
            SELECT s.id, s.group_id, s.payer_user_id, s.receiver_user_id, s.amount_paise,
                   s.settlement_date, s.notes, s.created_at,
                   g.name as group_name,
                   u1.full_name as payer_name,
                   u2.full_name as receiver_name
            FROM settlements s
            JOIN groups g ON s.group_id = g.id
            JOIN users u1 ON s.payer_user_id = u1.id
            JOIN users u2 ON s.receiver_user_id = u2.id
            WHERE s.payer_user_id = ? OR s.receiver_user_id = ?
            ORDER BY s.settlement_date DESC, s.id DESC
            LIMIT ?;
        """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, userId);
            stmt.setInt(3, limit > 0 ? limit : 5);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToSettlement(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    private Settlement mapResultSetToSettlement(ResultSet rs) throws SQLException {
        Settlement s = new Settlement();
        s.setId(rs.getInt("id"));
        s.setGroupId(rs.getInt("group_id"));
        s.setGroupName(rs.getString("group_name"));
        s.setPayerUserId(rs.getInt("payer_user_id"));
        s.setPayerName(rs.getString("payer_name"));
        s.setReceiverUserId(rs.getInt("receiver_user_id"));
        s.setReceiverName(rs.getString("receiver_name"));
        s.setAmountPaise(rs.getLong("amount_paise"));
        Date d = rs.getDate("settlement_date");
        if (d != null) s.setSettlementDate(d.toLocalDate());
        s.setNotes(rs.getString("notes"));
        return s;
    }
}

