package com.expensesplitter.database;

import com.expensesplitter.util.PasswordHasher;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseInitializer {

    public static void initializeDatabase() {
        initialize();
    }

    public static void initialize() {
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement()) {

            // 1. Users table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS users (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    username TEXT UNIQUE NOT NULL,
                    full_name TEXT NOT NULL,
                    email TEXT UNIQUE NOT NULL,
                    password_hash TEXT NOT NULL,
                    phone TEXT,
                    email_notifications_enabled INTEGER DEFAULT 1,
                    sms_notifications_enabled INTEGER DEFAULT 1,
                    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
                );
            """);

            // 2. Groups table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS groups (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL,
                    description TEXT,
                    created_by_user_id INTEGER NOT NULL,
                    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (created_by_user_id) REFERENCES users(id) ON DELETE CASCADE
                );
            """);

            // 3. Group Members table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS group_members (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    group_id INTEGER NOT NULL,
                    user_id INTEGER NOT NULL,
                    role TEXT DEFAULT 'MEMBER',
                    joined_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                    UNIQUE(group_id, user_id),
                    FOREIGN KEY (group_id) REFERENCES groups(id) ON DELETE CASCADE,
                    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
                );
            """);

            // 4. Expenses table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS expenses (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    group_id INTEGER NOT NULL,
                    paid_by_user_id INTEGER NOT NULL,
                    title TEXT NOT NULL,
                    description TEXT,
                    category TEXT NOT NULL,
                    split_method TEXT NOT NULL,
                    amount_paise INTEGER NOT NULL,
                    expense_date DATE NOT NULL,
                    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (group_id) REFERENCES groups(id) ON DELETE CASCADE,
                    FOREIGN KEY (paid_by_user_id) REFERENCES users(id) ON DELETE RESTRICT
                );
            """);

            // 5. Expense Participants table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS expense_participants (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    expense_id INTEGER NOT NULL,
                    user_id INTEGER NOT NULL,
                    share_amount_paise INTEGER NOT NULL,
                    percentage REAL,
                    is_settled INTEGER DEFAULT 0,
                    FOREIGN KEY (expense_id) REFERENCES expenses(id) ON DELETE CASCADE,
                    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE RESTRICT
                );
            """);

            // 6. Settlements table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS settlements (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    group_id INTEGER NOT NULL,
                    payer_user_id INTEGER NOT NULL,
                    receiver_user_id INTEGER NOT NULL,
                    amount_paise INTEGER NOT NULL,
                    settlement_date DATE NOT NULL,
                    notes TEXT,
                    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (group_id) REFERENCES groups(id) ON DELETE CASCADE,
                    FOREIGN KEY (payer_user_id) REFERENCES users(id) ON DELETE RESTRICT,
                    FOREIGN KEY (receiver_user_id) REFERENCES users(id) ON DELETE RESTRICT
                );
            """);

            // 7. Notifications table (with full Multi-Channel Delivery Support)
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS notifications (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id INTEGER NOT NULL,
                    recipient_name TEXT,
                    recipient_contact TEXT,
                    channel TEXT NOT NULL DEFAULT 'IN_APP',
                    title TEXT NOT NULL,
                    message TEXT NOT NULL,
                    type TEXT NOT NULL,
                    status TEXT NOT NULL DEFAULT 'SENT',
                    error_message TEXT,
                    retry_count INTEGER DEFAULT 0,
                    idempotency_key TEXT,
                    is_read INTEGER DEFAULT 0,
                    sent_at DATETIME,
                    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
                );
            """);

            // 8. App Settings table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS app_settings (
                    key TEXT PRIMARY KEY,
                    value TEXT NOT NULL
                );
            """);

            // Migration step for existing DB files
            migrateSchema(conn);

            // Seed initial data if empty
            seedInitialData(conn);

        } catch (SQLException e) {
            System.err.println("Database initialization failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void migrateSchema(Connection conn) {
        // Users table migrations
        addColumnIfNotExists(conn, "users", "email_notifications_enabled", "INTEGER DEFAULT 1");
        addColumnIfNotExists(conn, "users", "sms_notifications_enabled", "INTEGER DEFAULT 1");

        // Notifications table migrations
        addColumnIfNotExists(conn, "notifications", "recipient_name", "TEXT");
        addColumnIfNotExists(conn, "notifications", "recipient_contact", "TEXT");
        addColumnIfNotExists(conn, "notifications", "channel", "TEXT DEFAULT 'IN_APP'");
        addColumnIfNotExists(conn, "notifications", "status", "TEXT DEFAULT 'SENT'");
        addColumnIfNotExists(conn, "notifications", "error_message", "TEXT");
        addColumnIfNotExists(conn, "notifications", "retry_count", "INTEGER DEFAULT 0");
        addColumnIfNotExists(conn, "notifications", "idempotency_key", "TEXT");
        addColumnIfNotExists(conn, "notifications", "sent_at", "DATETIME");

        try (Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE UNIQUE INDEX IF NOT EXISTS idx_notif_idempotency ON notifications(idempotency_key) WHERE idempotency_key IS NOT NULL;");
        } catch (SQLException ignored) {
        }
    }

    private static void addColumnIfNotExists(Connection conn, String table, String column, String definition) {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition + ";");
        } catch (SQLException e) {
            // "duplicate column name" is expected when column already exists in SQLite
        }
    }

    private static void seedInitialData(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM users;");
            if (rs.next() && rs.getInt(1) > 0) {
                return; // Already seeded
            }
        }

        String defaultPassHash = PasswordHasher.hash("rahul123");
        String amanPassHash = PasswordHasher.hash("aman123");
        String rohitPassHash = PasswordHasher.hash("rohit123");
        String priyaPassHash = PasswordHasher.hash("priya123");

        try (Statement stmt = conn.createStatement()) {
            // Seed Users with validated E.164 phone numbers with country codes
            stmt.executeUpdate(String.format("""
                INSERT INTO users (id, username, full_name, email, password_hash, phone, email_notifications_enabled, sms_notifications_enabled) VALUES
                (1, 'rahul', 'Rahul Sharma', 'rahul@example.com', '%s', '+919876543210', 1, 1),
                (2, 'aman', 'Aman Verma', 'aman@example.com', '%s', '+919876543211', 1, 1),
                (3, 'rohit', 'Rohit Kumar', 'rohit@example.com', '%s', '+919876543212', 1, 1),
                (4, 'priya', 'Priya Patel', 'priya@example.com', '%s', '+919876543213', 1, 1);
            """, defaultPassHash, amanPassHash, rohitPassHash, priyaPassHash));

            // Seed Groups
            stmt.executeUpdate("""
                INSERT INTO groups (id, name, description, created_by_user_id) VALUES
                (1, 'Goa Trip 2026', 'Summer vacation beach villa & dining', 1),
                (2, 'Flat 402 Roommates', 'Apartment rent, groceries & broadband', 1),
                (3, 'Office Tech Team', 'Team lunches and hackathon celebrations', 1);
            """);

            // Seed Group Members
            stmt.executeUpdate("""
                INSERT INTO group_members (group_id, user_id, role) VALUES
                (1, 1, 'ADMIN'),
                (1, 2, 'MEMBER'),
                (1, 3, 'MEMBER'),
                (2, 1, 'ADMIN'),
                (2, 2, 'MEMBER'),
                (2, 4, 'MEMBER'),
                (3, 1, 'ADMIN'),
                (3, 3, 'MEMBER'),
                (3, 4, 'MEMBER');
            """);

            // Seed Expenses
            stmt.executeUpdate("""
                INSERT INTO expenses (id, group_id, paid_by_user_id, title, description, category, split_method, amount_paise, expense_date) VALUES
                (1, 1, 1, 'Beachside Dinner', 'Seafood and drinks at Baga Beach', 'FOOD', 'EQUAL', 90000, '2026-10-25'),
                (2, 1, 1, 'Scuba Diving & Boat Rental', 'Grand Island scuba package with video', 'ENTERTAINMENT', 'EQUAL', 450000, '2026-10-26'),
                (3, 2, 2, 'Apartment Wi-Fi & Electricity', 'Fiber broadband monthly recharge', 'ELECTRICITY', 'EQUAL', 240000, '2026-10-27'),
                (4, 2, 1, 'Weekly Organic Groceries', 'Supermarket supplies', 'GROCERIES', 'EQUAL', 180000, '2026-10-28'),
                (5, 3, 4, 'Client Demo Celebration Buffet', 'Team outing barbecue', 'FOOD', 'EQUAL', 360000, '2026-10-29');
            """);

            // Seed Expense Participants
            stmt.executeUpdate("""
                INSERT INTO expense_participants (expense_id, user_id, share_amount_paise, percentage) VALUES
                (1, 1, 30000, 33.33),
                (1, 2, 30000, 33.33),
                (1, 3, 30000, 33.33),
                (2, 1, 150000, 33.33),
                (2, 2, 150000, 33.33),
                (2, 3, 150000, 33.33),
                (3, 1, 80000, 33.33),
                (3, 2, 80000, 33.33),
                (3, 4, 80000, 33.33),
                (4, 1, 60000, 33.33),
                (4, 2, 60000, 33.33),
                (4, 4, 60000, 33.33),
                (5, 1, 120000, 33.33),
                (5, 3, 120000, 33.33),
                (5, 4, 120000, 33.33);
            """);

            // Seed Settlements
            stmt.executeUpdate("""
                INSERT INTO settlements (id, group_id, payer_user_id, receiver_user_id, amount_paise, settlement_date, notes) VALUES
                (1, 1, 2, 1, 50000, '2026-10-27', 'Google Pay UPI Transaction ref 8392109');
            """);

            // Seed Initial Notifications across channels
            stmt.executeUpdate("""
                INSERT INTO notifications (id, user_id, recipient_name, recipient_contact, channel, title, message, type, status, is_read) VALUES
                (1, 2, 'Aman Verma', 'aman@example.com', 'EMAIL', 'New Expense: Beachside Dinner', 'Rahul Sharma added Beachside Dinner (₹900.00) in Goa Trip 2026. Your share is ₹300.00.', 'EXPENSE_SPLIT', 'SENT', 0),
                (2, 2, 'Aman Verma', '+919876543211', 'SMS', 'Expense Split: Beachside Dinner', 'Split: Rahul paid ₹900.00 for Beachside Dinner in Goa Trip 2026. Your share: ₹300.00.', 'EXPENSE_SPLIT', 'SENT', 0),
                (3, 1, 'Rahul Sharma', 'rahul@example.com', 'EMAIL', 'Settlement Received: ₹500.00', 'Aman Verma recorded payment of ₹500.00 in Goa Trip 2026.', 'SETTLEMENT_RECEIVED', 'SENT', 1),
                (4, 2, 'Aman Verma', '+919876543211', 'SMS', 'Settlement Confirmation: ₹500.00', 'Your settlement of ₹500.00 to Rahul Sharma in Goa Trip 2026 was confirmed.', 'SETTLEMENT_CONFIRMATION', 'SENT', 1);
            """);

            // App Settings
            stmt.executeUpdate("""
                INSERT OR IGNORE INTO app_settings (key, value) VALUES
                ('currency', '₹'),
                ('theme', 'dark'),
                ('notification_mode', 'SIMULATION');
            """);
        }
    }
}
