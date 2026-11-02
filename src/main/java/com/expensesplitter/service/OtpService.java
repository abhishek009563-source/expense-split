package com.expensesplitter.service;

import com.expensesplitter.dao.OtpDao;
import com.expensesplitter.dao.UserDao;
import com.expensesplitter.model.PendingOtp;
import com.expensesplitter.model.User;
import com.expensesplitter.util.ValidationUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Handles all OTP lifecycle operations:
 *  - Generate & send OTP for registration
 *  - Generate & send OTP for existing-user login
 *  - Verify submitted OTP (with expiry, reuse-prevention, rate-limiting)
 *  - Complete account creation after successful registration OTP
 *  - Complete login session after successful login OTP
 *
 * Security:
 *  - OTPs are 6-digit cryptographically-random numbers (SecureRandom).
 *  - Only the SHA-256 hash is stored; the raw OTP is never persisted.
 *  - OTPs expire after 5 minutes.
 *  - Resend is blocked for 60 seconds.
 *  - Max 5 wrong attempts before the OTP is invalidated.
 */
public class OtpService {

    private static final int OTP_EXPIRY_MINUTES = 5;
    private static final int MAX_ATTEMPTS = 5;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final OtpDao otpDao = new OtpDao();
    private final UserDao userDao = new UserDao();
    private final EmailGateway emailGateway = new EmailGateway();

    // ─── Public: initiate registration OTP ───────────────────────────────────

    /**
     * Validates input, checks for duplicate email, generates and emails an OTP.
     * Does NOT create the user account yet.
     *
     * @throws IllegalArgumentException on validation failures or duplicate email.
     */
    public void initiateRegistration(String fullName, String email) {
        // 1. Validate
        if (fullName == null || fullName.trim().length() < 2) {
            throw new IllegalArgumentException("Full name must be at least 2 characters.");
        }
        if (!ValidationUtils.isValidEmail(email)) {
            throw new IllegalArgumentException("Invalid email address format.");
        }

        String normEmail = email.trim().toLowerCase();

        // 2. Check for existing active account
        if (userDao.findByEmail(normEmail).isPresent()) {
            throw new IllegalArgumentException(
                    "An account with this email already exists. Please log in instead.");
        }

        // 3. Check resend cooldown from any existing pending OTP
        Optional<PendingOtp> existing = otpDao.findActiveByEmailAndPurpose(normEmail, PendingOtp.OtpPurpose.REGISTER);
        if (existing.isPresent() && !existing.get().isResendAllowed()) {
            long secs = existing.get().secondsUntilResendAllowed();
            throw new IllegalArgumentException(
                    "Please wait " + secs + " second(s) before requesting a new OTP.");
        }

        // 4. Generate OTP
        String rawOtp = generateOtp();

        // 5. Build pending record
        PendingOtp pending = new PendingOtp();
        pending.setEmail(normEmail);
        pending.setFullName(fullName.trim());
        pending.setOtpHash(hashOtp(rawOtp));
        pending.setPurpose(PendingOtp.OtpPurpose.REGISTER);
        pending.setExpiresAt(LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES));
        pending.setLastSentAt(LocalDateTime.now());

        // 6. Persist (upsert replaces any old record)
        if (!otpDao.upsert(pending)) {
            throw new RuntimeException("Failed to save OTP record. Please try again.");
        }

        // 7. Send email
        sendOtpEmail(normEmail, fullName.trim(), rawOtp, "Create Account");
    }

    // ─── Public: initiate login OTP ──────────────────────────────────────────

    /**
     * Sends an OTP to the registered user's email for passwordless login.
     *
     * @throws IllegalArgumentException if the email is not registered or cooldown not elapsed.
     */
    public void initiateLogin(String email) {
        if (!ValidationUtils.isValidEmail(email)) {
            throw new IllegalArgumentException("Invalid email address format.");
        }

        String normEmail = email.trim().toLowerCase();

        // Must be a registered user
        Optional<User> userOpt = userDao.findByEmail(normEmail);
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException(
                    "No account found for this email. Please create an account first.");
        }

        // Check resend cooldown
        Optional<PendingOtp> existing = otpDao.findActiveByEmailAndPurpose(normEmail, PendingOtp.OtpPurpose.LOGIN);
        if (existing.isPresent() && !existing.get().isResendAllowed()) {
            long secs = existing.get().secondsUntilResendAllowed();
            throw new IllegalArgumentException(
                    "Please wait " + secs + " second(s) before requesting a new OTP.");
        }

        String rawOtp = generateOtp();

        PendingOtp pending = new PendingOtp();
        pending.setEmail(normEmail);
        pending.setFullName(userOpt.get().getFullName());
        pending.setOtpHash(hashOtp(rawOtp));
        pending.setPurpose(PendingOtp.OtpPurpose.LOGIN);
        pending.setExpiresAt(LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES));
        pending.setLastSentAt(LocalDateTime.now());

        if (!otpDao.upsert(pending)) {
            throw new RuntimeException("Failed to save OTP record. Please try again.");
        }

        sendOtpEmail(normEmail, userOpt.get().getFullName(), rawOtp, "Login");
    }

    // ─── Public: resend OTP ───────────────────────────────────────────────────

    /**
     * Resends the OTP (generates a fresh one) if the cooldown has elapsed.
     */
    public void resendOtp(String email, PendingOtp.OtpPurpose purpose) {
        if (purpose == PendingOtp.OtpPurpose.REGISTER) {
            // We need to fetch the stored full name
            Optional<PendingOtp> existing = otpDao.findActiveByEmailAndPurpose(
                    email.trim().toLowerCase(), purpose);
            String fullName = existing.map(PendingOtp::getFullName).orElse("User");
            initiateRegistration(fullName, email);
        } else {
            initiateLogin(email);
        }
    }

    // ─── Public: verify OTP ───────────────────────────────────────────────────

    /**
     * Result object returned by verifyOtp().
     */
    public record VerifyResult(boolean success, String errorMessage, User createdOrLoggedInUser) {
        public static VerifyResult ok(User u) { return new VerifyResult(true, null, u); }
        public static VerifyResult fail(String msg) { return new VerifyResult(false, msg, null); }
    }

    /**
     * Verifies the submitted OTP code.
     * On success for REGISTER: creates the user account.
     * On success for LOGIN: returns the existing user.
     */
    public VerifyResult verifyOtp(String email, String rawOtp, PendingOtp.OtpPurpose purpose) {
        if (rawOtp == null || rawOtp.trim().length() != 6) {
            return VerifyResult.fail("Please enter the 6-digit OTP.");
        }

        String normEmail = email.trim().toLowerCase();
        Optional<PendingOtp> opt = otpDao.findActiveByEmailAndPurpose(normEmail, purpose);

        if (opt.isEmpty()) {
            return VerifyResult.fail("No active OTP found. Please request a new one.");
        }

        PendingOtp pending = opt.get();

        if (pending.isUsed()) {
            return VerifyResult.fail("This OTP has already been used. Please request a new one.");
        }
        if (pending.isExpired()) {
            return VerifyResult.fail("OTP has expired. Please request a new one.");
        }
        if (pending.getAttemptCount() >= MAX_ATTEMPTS) {
            otpDao.markUsed(pending.getId()); // invalidate after too many attempts
            return VerifyResult.fail("Too many incorrect attempts. Please request a new OTP.");
        }

        // Compare hashes
        String submittedHash = hashOtp(rawOtp.trim());
        if (!submittedHash.equalsIgnoreCase(pending.getOtpHash())) {
            otpDao.incrementAttempts(pending.getId());
            int remaining = MAX_ATTEMPTS - pending.getAttemptCount() - 1;
            return VerifyResult.fail("Incorrect OTP. " + remaining + " attempt(s) remaining.");
        }

        // ✅ Valid OTP – mark as used
        otpDao.markUsed(pending.getId());

        if (purpose == PendingOtp.OtpPurpose.REGISTER) {
            // Create the user account
            User user = createUserFromPending(pending);
            if (user == null) {
                return VerifyResult.fail("Failed to create account. Please try again.");
            }
            return VerifyResult.ok(user);
        } else {
            // Fetch and return existing user
            Optional<User> userOpt = userDao.findByEmail(normEmail);
            return userOpt.map(VerifyResult::ok)
                          .orElse(VerifyResult.fail("Account not found. Please register."));
        }
    }

    // ─── Private helpers ──────────────────────────────────────────────────────

    private String generateOtp() {
        // Cryptographically-secure 6-digit OTP (100000 – 999999)
        int otp = 100000 + SECURE_RANDOM.nextInt(900000);
        return String.valueOf(otp);
    }

    private String hashOtp(String rawOtp) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawOtp.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) sb.append('0');
                sb.append(hex);
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    private void sendOtpEmail(String toEmail, String name, String rawOtp, String purpose) {
        String subject = "Your Expense Splitter OTP – " + rawOtp;
        String bodyText = """
                Hi %s,

                Your one-time password (OTP) for %s is:

                        %s

                This code is valid for %d minutes. Do not share it with anyone.

                If you did not request this, please ignore this email.

                – Expense Splitter
                """.formatted(name, purpose, rawOtp, OTP_EXPIRY_MINUTES);

        String bodyHtml = """
                <div style="font-family:Arial,sans-serif;max-width:480px;margin:auto;padding:32px;
                            background:#0f172a;border-radius:16px;color:#f8fafc;">
                  <h2 style="color:#3b82f6;margin-bottom:4px;">⚡ Expense Splitter</h2>
                  <p style="color:#94a3b8;margin-top:0;">Secure %s</p>
                  <hr style="border:none;border-top:1px solid #334155;margin:20px 0;">
                  <p>Hi <strong>%s</strong>,</p>
                  <p>Your one-time password is:</p>
                  <div style="font-size:36px;font-weight:800;letter-spacing:8px;
                              background:#1e293b;border:2px solid #3b82f6;border-radius:12px;
                              padding:16px 24px;text-align:center;color:#60a5fa;margin:20px 0;">
                    %s
                  </div>
                  <p style="color:#94a3b8;font-size:13px;">
                    ⏱ Valid for <strong style="color:#f59e0b;">%d minutes</strong>.
                    Never share this code with anyone.
                  </p>
                  <hr style="border:none;border-top:1px solid #334155;margin:20px 0;">
                  <p style="color:#64748b;font-size:12px;">
                    If you didn't request this, please ignore this email.
                  </p>
                </div>
                """.formatted(purpose, name, rawOtp, OTP_EXPIRY_MINUTES);

        DeliveryResult result = emailGateway.sendEmail(toEmail, name, subject, bodyText, bodyHtml);
        if (!result.isSuccess()) {
            // Log but don't propagate to UI – OTP is already saved, user can resend
            System.err.println("[OtpService] Email delivery failed: " + result.getErrorMessage());
        }
    }

    private User createUserFromPending(PendingOtp pending) {
        // Use email-prefix as username if none stored (registration flow only needs name+email)
        String username = pending.getEmail().split("@")[0].replaceAll("[^a-zA-Z0-9_]", "_");
        // Ensure username uniqueness by appending random digits if needed
        String baseUsername = username;
        int suffix = 1;
        while (userDao.findByUsername(username).isPresent()) {
            username = baseUsername + suffix++;
        }

        User user = new User();
        user.setUsername(username);
        user.setFullName(pending.getFullName());
        user.setEmail(pending.getEmail());
        // No password – OTP-only account. Set a random unusable hash.
        user.setPasswordHash("OTP_AUTH_" + hashOtp(java.util.UUID.randomUUID().toString()));
        user.setEmailNotificationsEnabled(true);
        user.setSmsNotificationsEnabled(false);

        boolean created = userDao.create(user);
        return created ? user : null;
    }
}
