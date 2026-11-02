package com.expensesplitter.model;

import java.time.LocalDateTime;

/**
 * Represents a pending OTP record stored in the database.
 * Used for both registration verification and existing-user login.
 */
public class PendingOtp {

    public enum OtpPurpose {
        REGISTER,   // New user awaiting email verification before account creation
        LOGIN       // Existing user requesting OTP-based login
    }

    private int id;
    private String email;
    private String fullName;        // only set for REGISTER purpose
    private String otpHash;         // SHA-256 hash of the 6-digit OTP
    private OtpPurpose purpose;
    private LocalDateTime expiresAt; // 5-minute window
    private LocalDateTime lastSentAt;// for 60-second resend cooldown
    private boolean used;            // prevent OTP reuse
    private int attemptCount;        // rate-limiting: max 5 wrong attempts
    private LocalDateTime createdAt;

    public PendingOtp() {}

    // ─── Getters & Setters ───────────────────────────────────────────────────

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getOtpHash() { return otpHash; }
    public void setOtpHash(String otpHash) { this.otpHash = otpHash; }

    public OtpPurpose getPurpose() { return purpose; }
    public void setPurpose(OtpPurpose purpose) { this.purpose = purpose; }

    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }

    public LocalDateTime getLastSentAt() { return lastSentAt; }
    public void setLastSentAt(LocalDateTime lastSentAt) { this.lastSentAt = lastSentAt; }

    public boolean isUsed() { return used; }
    public void setUsed(boolean used) { this.used = used; }

    public int getAttemptCount() { return attemptCount; }
    public void setAttemptCount(int attemptCount) { this.attemptCount = attemptCount; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    // ─── Convenience ─────────────────────────────────────────────────────────

    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }

    public boolean isResendAllowed() {
        if (lastSentAt == null) return true;
        return LocalDateTime.now().isAfter(lastSentAt.plusSeconds(60));
    }

    public long secondsUntilResendAllowed() {
        if (lastSentAt == null) return 0;
        LocalDateTime resendAt = lastSentAt.plusSeconds(60);
        java.time.Duration d = java.time.Duration.between(LocalDateTime.now(), resendAt);
        return d.isNegative() ? 0 : d.getSeconds() + 1;
    }
}
