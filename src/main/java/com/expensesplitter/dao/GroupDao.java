package com.expensesplitter.dao;

import com.expensesplitter.database.DatabaseManager;
import com.expensesplitter.model.Group;
import com.expensesplitter.model.GroupMember;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class GroupDao {

    public Optional<Group> findById(int id) {
        String sql = """
            SELECT g.id, g.name, g.description, g.created_by_user_id, g.created_at, u.full_name as creator_name,
                   (SELECT COUNT(*) FROM group_members gm WHERE gm.group_id = g.id) as member_count,
                   (SELECT COALESCE(SUM(amount_paise), 0) FROM expenses e WHERE e.group_id = g.id) as total_expense
            FROM groups g
            JOIN users u ON g.created_by_user_id = u.id
            WHERE g.id = ?;
        """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToGroup(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    public List<Group> findGroupsByUserId(int userId) {
        List<Group> list = new ArrayList<>();
        String sql = """
            SELECT g.id, g.name, g.description, g.created_by_user_id, g.created_at, u.full_name as creator_name,
                   (SELECT COUNT(*) FROM group_members gm WHERE gm.group_id = g.id) as member_count,
                   (SELECT COALESCE(SUM(amount_paise), 0) FROM expenses e WHERE e.group_id = g.id) as total_expense
            FROM groups g
            JOIN group_members gm ON g.id = gm.group_id
            JOIN users u ON g.created_by_user_id = u.id
            WHERE gm.user_id = ?
            ORDER BY g.created_at DESC;
        """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToGroup(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public boolean create(Group group, List<Integer> memberUserIds) {
        Connection conn = null;
        try {
            conn = DatabaseManager.getConnection();
            conn.setAutoCommit(false);

            String sqlGroup = "INSERT INTO groups (name, description, created_by_user_id) VALUES (?, ?, ?);";
            try (PreparedStatement stmt = conn.prepareStatement(sqlGroup, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setString(1, group.getName().trim());
                stmt.setString(2, group.getDescription() != null ? group.getDescription().trim() : "");
                stmt.setInt(3, group.getCreatedByUserId());
                stmt.executeUpdate();

                try (ResultSet keys = stmt.getGeneratedKeys()) {
                    if (keys.next()) {
                        group.setId(keys.getInt(1));
                    }
                }
            }

            // Always add creator as ADMIN
            String sqlMember = "INSERT OR IGNORE INTO group_members (group_id, user_id, role) VALUES (?, ?, ?);";
            try (PreparedStatement stmt = conn.prepareStatement(sqlMember)) {
                stmt.setInt(1, group.getId());
                stmt.setInt(2, group.getCreatedByUserId());
                stmt.setString(3, "ADMIN");
                stmt.executeUpdate();

                // Add other members
                if (memberUserIds != null) {
                    for (int uid : memberUserIds) {
                        if (uid != group.getCreatedByUserId()) {
                            stmt.setInt(1, group.getId());
                            stmt.setInt(2, uid);
                            stmt.setString(3, "MEMBER");
                            stmt.executeUpdate();
                        }
                    }
                }
            }

            conn.commit();
            return true;
        } catch (SQLException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            }
            e.printStackTrace();
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); } catch (SQLException e) { e.printStackTrace(); }
            }
        }
        return false;
    }

    public boolean update(Group group) {
        String sql = "UPDATE groups SET name = ?, description = ? WHERE id = ?;";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, group.getName().trim());
            stmt.setString(2, group.getDescription() != null ? group.getDescription().trim() : "");
            stmt.setInt(3, group.getId());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean delete(int groupId) {
        String sql = "DELETE FROM groups WHERE id = ?;";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, groupId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public List<GroupMember> getMembers(int groupId) {
        List<GroupMember> list = new ArrayList<>();
        String sql = """
            SELECT gm.id, gm.group_id, gm.user_id, gm.role, gm.joined_at,
                   u.username, u.full_name, u.email
            FROM group_members gm
            JOIN users u ON gm.user_id = u.id
            WHERE gm.group_id = ?
            ORDER BY gm.role DESC, u.full_name ASC;
        """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, groupId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    GroupMember m = new GroupMember();
                    m.setId(rs.getInt("id"));
                    m.setGroupId(rs.getInt("group_id"));
                    m.setUserId(rs.getInt("user_id"));
                    m.setRole(rs.getString("role"));
                    m.setUserName(rs.getString("username"));
                    m.setUserFullName(rs.getString("full_name"));
                    m.setUserEmail(rs.getString("email"));
                    list.add(m);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public boolean addMember(int groupId, int userId, String role) {
        String sql = "INSERT OR IGNORE INTO group_members (group_id, user_id, role) VALUES (?, ?, ?);";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, groupId);
            stmt.setInt(2, userId);
            stmt.setString(3, role != null ? role : "MEMBER");
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean removeMember(int groupId, int userId) {
        String sql = "DELETE FROM group_members WHERE group_id = ? AND user_id = ?;";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, groupId);
            stmt.setInt(2, userId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    private Group mapResultSetToGroup(ResultSet rs) throws SQLException {
        Group g = new Group();
        g.setId(rs.getInt("id"));
        g.setName(rs.getString("name"));
        g.setDescription(rs.getString("description"));
        g.setCreatedByUserId(rs.getInt("created_by_user_id"));
        g.setCreatedByUserName(rs.getString("creator_name"));
        g.setMemberCount(rs.getInt("member_count"));
        g.setTotalExpensePaise(rs.getLong("total_expense"));
        return g;
    }
}

