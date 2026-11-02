package com.expensesplitter.dao;

import com.expensesplitter.database.DatabaseManager;
import com.expensesplitter.model.Balance;
import com.expensesplitter.model.Expense;
import com.expensesplitter.model.ExpenseCategory;
import com.expensesplitter.model.ExpenseParticipant;
import com.expensesplitter.model.SplitMethod;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ExpenseDao {

    public boolean create(Expense expense, List<ExpenseParticipant> participants) {
        Connection conn = null;
        try {
            conn = DatabaseManager.getConnection();
            conn.setAutoCommit(false);

            String sqlExpense = """
                INSERT INTO expenses (group_id, paid_by_user_id, title, description, category, split_method, amount_paise, expense_date)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?);
            """;
            try (PreparedStatement stmt = conn.prepareStatement(sqlExpense, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setInt(1, expense.getGroupId());
                stmt.setInt(2, expense.getPaidByUserId());
                stmt.setString(3, expense.getTitle().trim());
                stmt.setString(4, expense.getDescription() != null ? expense.getDescription().trim() : "");
                stmt.setString(5, expense.getCategory() != null ? expense.getCategory().name() : "OTHER");
                stmt.setString(6, expense.getSplitMethod() != null ? expense.getSplitMethod().name() : "EQUAL");
                stmt.setLong(7, expense.getAmountPaise());
                stmt.setDate(8, Date.valueOf(expense.getExpenseDate() != null ? expense.getExpenseDate() : LocalDate.now()));
                stmt.executeUpdate();

                try (ResultSet keys = stmt.getGeneratedKeys()) {
                    if (keys.next()) {
                        expense.setId(keys.getInt(1));
                    }
                }
            }

            String sqlPart = "INSERT INTO expense_participants (expense_id, user_id, share_amount_paise, percentage) VALUES (?, ?, ?, ?);";
            try (PreparedStatement stmt = conn.prepareStatement(sqlPart)) {
                for (ExpenseParticipant p : participants) {
                    stmt.setInt(1, expense.getId());
                    stmt.setInt(2, p.getUserId());
                    stmt.setLong(3, p.getShareAmountPaise());
                    stmt.setDouble(4, p.getPercentage());
                    stmt.addBatch();
                }
                stmt.executeBatch();
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

    public boolean update(Expense expense, List<ExpenseParticipant> participants) {
        Connection conn = null;
        try {
            conn = DatabaseManager.getConnection();
            conn.setAutoCommit(false);

            String sqlUpdate = """
                UPDATE expenses SET group_id = ?, paid_by_user_id = ?, title = ?, description = ?,
                                    category = ?, split_method = ?, amount_paise = ?, expense_date = ?
                WHERE id = ?;
            """;
            try (PreparedStatement stmt = conn.prepareStatement(sqlUpdate)) {
                stmt.setInt(1, expense.getGroupId());
                stmt.setInt(2, expense.getPaidByUserId());
                stmt.setString(3, expense.getTitle().trim());
                stmt.setString(4, expense.getDescription() != null ? expense.getDescription().trim() : "");
                stmt.setString(5, expense.getCategory() != null ? expense.getCategory().name() : "OTHER");
                stmt.setString(6, expense.getSplitMethod() != null ? expense.getSplitMethod().name() : "EQUAL");
                stmt.setLong(7, expense.getAmountPaise());
                stmt.setDate(8, Date.valueOf(expense.getExpenseDate() != null ? expense.getExpenseDate() : LocalDate.now()));
                stmt.setInt(9, expense.getId());
                stmt.executeUpdate();
            }

            // Remove old participants and reinsert updated ones
            try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM expense_participants WHERE expense_id = ?;")) {
                stmt.setInt(1, expense.getId());
                stmt.executeUpdate();
            }

            String sqlPart = "INSERT INTO expense_participants (expense_id, user_id, share_amount_paise, percentage) VALUES (?, ?, ?, ?);";
            try (PreparedStatement stmt = conn.prepareStatement(sqlPart)) {
                for (ExpenseParticipant p : participants) {
                    stmt.setInt(1, expense.getId());
                    stmt.setInt(2, p.getUserId());
                    stmt.setLong(3, p.getShareAmountPaise());
                    stmt.setDouble(4, p.getPercentage());
                    stmt.addBatch();
                }
                stmt.executeBatch();
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

    public boolean delete(int expenseId) {
        String sql = "DELETE FROM expenses WHERE id = ?;";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, expenseId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public Optional<Expense> findById(int id) {
        String sql = """
            SELECT e.id, e.group_id, e.paid_by_user_id, e.title, e.description, e.category, e.split_method,
                   e.amount_paise, e.expense_date, e.created_at, g.name as group_name, u.full_name as payer_name
            FROM expenses e
            JOIN groups g ON e.group_id = g.id
            JOIN users u ON e.paid_by_user_id = u.id
            WHERE e.id = ?;
        """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Expense exp = mapResultSetToExpense(rs);
                    exp.setParticipants(getParticipants(exp.getId()));
                    return Optional.of(exp);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    public List<Expense> findByGroupId(int groupId) {
        List<Expense> list = new ArrayList<>();
        String sql = """
            SELECT e.id, e.group_id, e.paid_by_user_id, e.title, e.description, e.category, e.split_method,
                   e.amount_paise, e.expense_date, e.created_at, g.name as group_name, u.full_name as payer_name
            FROM expenses e
            JOIN groups g ON e.group_id = g.id
            JOIN users u ON e.paid_by_user_id = u.id
            WHERE e.group_id = ?
            ORDER BY e.expense_date DESC, e.id DESC;
        """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, groupId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToExpense(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public List<Expense> findByUserId(int userId) {
        List<Expense> list = new ArrayList<>();
        String sql = """
            SELECT DISTINCT e.id, e.group_id, e.paid_by_user_id, e.title, e.description, e.category, e.split_method,
                            e.amount_paise, e.expense_date, e.created_at, g.name as group_name, u.full_name as payer_name
            FROM expenses e
            JOIN groups g ON e.group_id = g.id
            JOIN users u ON e.paid_by_user_id = u.id
            LEFT JOIN expense_participants ep ON e.id = ep.expense_id
            WHERE e.paid_by_user_id = ? OR ep.user_id = ?
            ORDER BY e.expense_date DESC, e.id DESC;
        """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToExpense(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public List<Expense> findRecentByUserId(int userId, int limit) {
        List<Expense> list = new ArrayList<>();
        String sql = """
            SELECT DISTINCT e.id, e.group_id, e.paid_by_user_id, e.title, e.description, e.category, e.split_method,
                            e.amount_paise, e.expense_date, e.created_at, g.name as group_name, u.full_name as payer_name
            FROM expenses e
            JOIN groups g ON e.group_id = g.id
            JOIN users u ON e.paid_by_user_id = u.id
            LEFT JOIN expense_participants ep ON e.id = ep.expense_id
            WHERE e.paid_by_user_id = ? OR ep.user_id = ?
            ORDER BY e.expense_date DESC, e.id DESC
            LIMIT ?;
        """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, userId);
            stmt.setInt(3, limit > 0 ? limit : 10);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToExpense(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public List<ExpenseParticipant> getParticipants(int expenseId) {
        List<ExpenseParticipant> list = new ArrayList<>();
        String sql = """
            SELECT ep.id, ep.expense_id, ep.user_id, ep.share_amount_paise, ep.percentage,
                   u.username, u.full_name
            FROM expense_participants ep
            JOIN users u ON ep.user_id = u.id
            WHERE ep.expense_id = ?;
        """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, expenseId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    ExpenseParticipant p = new ExpenseParticipant();
                    p.setId(rs.getInt("id"));
                    p.setExpenseId(rs.getInt("expense_id"));
                    p.setUserId(rs.getInt("user_id"));
                    p.setShareAmountPaise(rs.getLong("share_amount_paise"));
                    p.setPercentage(rs.getDouble("percentage"));
                    p.setUserName(rs.getString("username"));
                    p.setUserFullName(rs.getString("full_name"));
                    list.add(p);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    /**
     * Calculates net balances for all members in a given group, factoring in expenses and settlements.
     */
    public List<Balance> getBalancesForGroup(int groupId) {
        List<Balance> balances = new ArrayList<>();
        String sql = """
            SELECT u.id as user_id, u.username, u.full_name,
                -- Total paid for expenses in this group
                COALESCE((SELECT SUM(amount_paise) FROM expenses WHERE group_id = gm.group_id AND paid_by_user_id = u.id), 0) as total_paid,
                -- Total share owed for expenses in this group
                COALESCE((SELECT SUM(ep.share_amount_paise)
                          FROM expense_participants ep
                          JOIN expenses e ON ep.expense_id = e.id
                          WHERE e.group_id = gm.group_id AND ep.user_id = u.id), 0) as total_share,
                -- Total settlements paid by user in this group
                COALESCE((SELECT SUM(amount_paise) FROM settlements WHERE group_id = gm.group_id AND payer_user_id = u.id), 0) as settled_paid,
                -- Total settlements received by user in this group
                COALESCE((SELECT SUM(amount_paise) FROM settlements WHERE group_id = gm.group_id AND receiver_user_id = u.id), 0) as settled_received
            FROM group_members gm
            JOIN users u ON gm.user_id = u.id
            WHERE gm.group_id = ?
            ORDER BY u.full_name ASC;
        """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, groupId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    int uid = rs.getInt("user_id");
                    String uname = rs.getString("username");
                    String fname = rs.getString("full_name");
                    long paid = rs.getLong("total_paid");
                    long share = rs.getLong("total_share");
                    long settledPaid = rs.getLong("settled_paid");
                    long settledRec = rs.getLong("settled_received");

                    // Net balance = (paid - share) + (settledPaid - settledRec)
                    long net = (paid - share) + (settledPaid - settledRec);

                    Balance b = new Balance(uid, uname, fname, paid, share);
                    b.setNetBalancePaise(net);
                    balances.add(b);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return balances;
    }

    /**
     * Calculates overall financial situation for a user across all groups:
     * [totalUserPaid, totalUserShare, totalYouAreOwed, totalYouOwe]
     */
    public long[] getUserFinancialOverview(int userId) {
        // [0] = totalPaid, [1] = totalShare, [2] = youAreOwed (positive debts), [3] = youOwe (negative debts)
        long totalPaid = 0;
        long totalShare = 0;
        long youAreOwed = 0;
        long youOwe = 0;

        String sqlPaid = "SELECT COALESCE(SUM(amount_paise), 0) FROM expenses WHERE paid_by_user_id = ?;";
        String sqlShare = "SELECT COALESCE(SUM(share_amount_paise), 0) FROM expense_participants WHERE user_id = ?;";

        try (Connection conn = DatabaseManager.getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement(sqlPaid)) {
                stmt.setInt(1, userId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) totalPaid = rs.getLong(1);
                }
            }
            try (PreparedStatement stmt = conn.prepareStatement(sqlShare)) {
                stmt.setInt(1, userId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) totalShare = rs.getLong(1);
                }
            }

            // Now compute group-level debts across all user's groups
            List<Integer> groupIds = new ArrayList<>();
            try (PreparedStatement stmt = conn.prepareStatement("SELECT group_id FROM group_members WHERE user_id = ?;")) {
                stmt.setInt(1, userId);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) groupIds.add(rs.getInt(1));
                }
            }

            for (int gid : groupIds) {
                List<Balance> groupBalances = getBalancesForGroup(gid);
                for (Balance b : groupBalances) {
                    if (b.getUserId() == userId) {
                        long net = b.getNetBalancePaise();
                        if (net > 0) {
                            youAreOwed += net;
                        } else if (net < 0) {
                            youOwe += (-net);
                        }
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return new long[]{totalPaid, totalShare, youAreOwed, youOwe};
    }

    private Expense mapResultSetToExpense(ResultSet rs) throws SQLException {
        Expense exp = new Expense();
        exp.setId(rs.getInt("id"));
        exp.setGroupId(rs.getInt("group_id"));
        exp.setGroupName(rs.getString("group_name"));
        exp.setPaidByUserId(rs.getInt("paid_by_user_id"));
        exp.setPaidByUserName(rs.getString("payer_name"));
        exp.setTitle(rs.getString("title"));
        exp.setDescription(rs.getString("description"));
        exp.setCategory(ExpenseCategory.fromString(rs.getString("category")));
        exp.setSplitMethod(SplitMethod.fromString(rs.getString("split_method")));
        exp.setAmountPaise(rs.getLong("amount_paise"));
        Date d = rs.getDate("expense_date");
        if (d != null) exp.setExpenseDate(d.toLocalDate());
        return exp;
    }
}

