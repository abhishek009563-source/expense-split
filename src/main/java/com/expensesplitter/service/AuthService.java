package com.expensesplitter.service;

import com.expensesplitter.dao.UserDao;
import com.expensesplitter.model.User;
import com.expensesplitter.util.PasswordHasher;
import com.expensesplitter.util.SessionManager;
import com.expensesplitter.util.ValidationUtils;

import java.util.Optional;

public class AuthService {

    private final UserDao userDao = new UserDao();

    public boolean login(String usernameOrEmail, String password) {
        if (usernameOrEmail == null || usernameOrEmail.isBlank() || password == null || password.isBlank()) {
            throw new IllegalArgumentException("Username/Email and password cannot be empty.");
        }

        Optional<User> userOpt = userDao.findByUsername(usernameOrEmail.trim());
        if (userOpt.isEmpty()) {
            userOpt = userDao.findByEmail(usernameOrEmail.trim());
        }

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (PasswordHasher.verify(password, user.getPasswordHash())) {
                SessionManager.setCurrentUser(user);
                return true;
            }
        }
        return false;
    }

    public boolean register(String username, String fullName, String email, String password, String phone) {
        if (username == null || username.trim().length() < 3) {
            throw new IllegalArgumentException("Username must be at least 3 characters long.");
        }
        ValidationUtils.validateContactDetails(fullName, email, phone);

        if (password == null || password.length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters long.");
        }

        if (userDao.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("Username '" + username + "' is already taken.");
        }
        if (userDao.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("An account with email '" + email + "' already exists.");
        }

        User newUser = new User();
        newUser.setUsername(username.trim());
        newUser.setFullName(fullName.trim());
        newUser.setEmail(email.trim().toLowerCase());
        newUser.setPasswordHash(PasswordHasher.hash(password));
        newUser.setPhone(phone != null && !phone.isBlank() ? ValidationUtils.normalizePhoneNumber(phone) : null);
        newUser.setEmailNotificationsEnabled(true);
        newUser.setSmsNotificationsEnabled(true);

        boolean created = userDao.create(newUser);
        if (created) {
            SessionManager.setCurrentUser(newUser);
        }
        return created;
    }

    public boolean updateProfile(int userId, String fullName, String email, String phone) {
        return updateProfile(userId, fullName, email, phone, true, true);
    }

    public boolean updateProfile(int userId, String fullName, String email, String phone, boolean emailEnabled, boolean smsEnabled) {
        Optional<User> userOpt = userDao.findById(userId);
        if (userOpt.isEmpty()) return false;
        User user = userOpt.get();

        ValidationUtils.validateContactDetails(fullName, email, phone);

        user.setFullName(fullName.trim());
        user.setEmail(email.trim().toLowerCase());
        user.setPhone(phone != null && !phone.isBlank() ? ValidationUtils.normalizePhoneNumber(phone) : null);
        user.setEmailNotificationsEnabled(emailEnabled);
        user.setSmsNotificationsEnabled(smsEnabled);
        return updateProfile(user);
    }

    public boolean updateProfile(User user) {
        if (user == null || user.getId() <= 0) return false;
        ValidationUtils.validateContactDetails(user.getFullName(), user.getEmail(), user.getPhone());

        Optional<User> existing = userDao.findByEmail(user.getEmail());
        if (existing.isPresent() && existing.get().getId() != user.getId()) {
            throw new IllegalArgumentException("Email is already used by another account.");
        }

        boolean success = userDao.update(user);
        if (success && SessionManager.getCurrentUser() != null && SessionManager.getCurrentUser().getId() == user.getId()) {
            SessionManager.setCurrentUser(user);
        }
        return success;
    }

    public boolean changePassword(int userId, String oldPassword, String newPassword) {
        Optional<User> userOpt = userDao.findById(userId);
        if (userOpt.isEmpty()) return false;
        User user = userOpt.get();

        if (!PasswordHasher.verify(oldPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("Current password is incorrect.");
        }
        if (newPassword == null || newPassword.length() < 6) {
            throw new IllegalArgumentException("New password must be at least 6 characters.");
        }

        return userDao.updatePassword(userId, PasswordHasher.hash(newPassword));
    }
}
