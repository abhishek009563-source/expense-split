package com.expensesplitter.util;

import com.expensesplitter.model.User;

public class SessionManager {

    private static User currentUser;
    private static String activeTheme = "dark"; // "dark" or "light"

    public static User getCurrentUser() {
        return currentUser;
    }

    public static void setCurrentUser(User user) {
        currentUser = user;
    }

    public static boolean isLoggedIn() {
        return currentUser != null;
    }

    public static void logout() {
        currentUser = null;
    }

    public static String getActiveTheme() {
        return activeTheme;
    }

    public static void setActiveTheme(String theme) {
        if ("light".equalsIgnoreCase(theme)) {
            activeTheme = "light";
        } else {
            activeTheme = "dark";
        }
    }
}

