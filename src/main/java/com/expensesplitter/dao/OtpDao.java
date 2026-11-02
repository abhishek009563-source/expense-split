package com.expensesplitter.dao;

import com.expensesplitter.database.DatabaseManager;
import com.expensesplitter.model.PendingOtp;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Data-access object for the {@code pending_otps} table.
 * All OTPs are stored as SHA-256 hashes – the raw OTP is never persisted.
 */
public class OtpDao {

    // ─── Upsert (insert or replace) ──────────────────────────────────────────

    /**
     * Inserts a new OTP record for the email+purpose, or replaces an existing one.
     * Existing used/expired records for the same email+purpose are deleted first.
     */
    public boolean upsert(PendingOtp otp) {
        // Delete any stale records for this email+purpose
        deleteByEmailAndPurpose(otp.getEmail(), otp.getPurpose());

        String sql = """
            INSERT INTO pending_otps
                (email, full_name, otp_hash, purpose, expires_at, last_sent_at, used, attempt_count)
            VALUES (?, ?, ?, ?, ?, ?, 0, 0);
            """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, otp.getEmail().trim().toLowerCase());
            stmt.setString(2, otp.getFullName());
            stmt.setString(3, otp.getOtpHash());
            stmt.setString(4, otp.getPurpose().name());
            stmt.setString(5, otp.getExpiresAt().toString());
            stmt.setString(6, otp.getLastSentAt().toString());
            int rows = stmt.executeUpdate();
            if (rows > 0) {
                try (ResultSet keys = stmt.getGeneratedKeys()) {
                    if (keys.next()) otp.setId(keys.getInt(1));
                }
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // ─── Find active (unused, non-expired) record ────────────────────────────

    public Optional<PendingOtp> findActiveByEmailAndPurpose(String email, PendingOtp.OtpPurpose purpose) {
        String sql = """
            SELECT id, email, full_name, otp_hash, purpose, expires_at, last_sent_at,
                   used, attempt_count, created_at
            FROM pending_otps
            WHERE LOWER(email) = LOWER(?)
              AND purpose = ?
              AND used = 0
            ORDER BY created_at DESC
            LIMIT 1;
            """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email.trim());
            stmt.setString(2, purpose.name());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(map(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    // ─── Mark OTP as used ────────────────────────────────────────────────────

    public boolean markUsed(int otpId) {
        String sql = "UPDATE pending_otps SET used = 1 WHERE id = ?;";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, otpId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // ─── Increment attempt count ─────────────────────────────────────────────

    public boolean incrementAttempts(int otpId) {
        String sql = "UPDATE pending_otps SET attempt_count = attempt_count + 1 WHERE id = ?;";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, otpId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // ─── Update last_sent_at (for resend cooldown) ───────────────────────────

    public boolean updateLastSentAt(int otpId, LocalDateTime lastSentAt) {
        String sql = "UPDATE pending_otps SET last_sent_at = ? WHERE id = ?;";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, lastSentAt.toString());
            stmt.setInt(2, otpId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // ─── Delete helpers ──────────────────────────────────────────────────────

    public void deleteByEmailAndPurpose(String email, PendingOtp.OtpPurpose purpose) {
        String sql = "DELETE FROM pending_otps WHERE LOWER(email) = LOWER(?) AND purpose = ?;";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email.trim());
            stmt.setString(2, purpose.name());
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /** Housekeeping: purge OTP records older than 24 hours. */
    public void purgeExpired() {
        String sql = "DELETE FROM pending_otps WHERE created_at < datetime('now', '-24 hours');";
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ─── Mapper ──────────────────────────────────────────────────────────────

    private PendingOtp map(ResultSet rs) throws SQLException {
        PendingOtp o = new PendingOtp();
        o.setId(rs.getInt("id"));
        o.setEmail(rs.getString("email"));
        o.setFullName(rs.getString("full_name"));
        o.setOtpHash(rs.getString("otp_hash"));
        o.setPurpose(PendingOtp.OtpPurpose.valueOf(rs.getString("purpose")));
        String expiresAt = rs.getString("expires_at");
        if (expiresAt != null) o.setExpiresAt(LocalDateTime.parse(expiresAt));
        String lastSentAt = rs.getString("last_sent_at");
        if (lastSentAt != null) o.setLastSentAt(LocalDateTime.parse(lastSentAt));
        o.setUsed(rs.getInt("used") == 1);
        o.setAttemptCount(rs.getInt("attempt_count"));
        String createdAt = rs.getString("created_at");
        if (createdAt != null) {
            try { o.setCreatedAt(LocalDateTime.parse(createdAt.replace(" ", "T"))); } catch (Exception ignored) {}
        }
        return o;
    }
}
