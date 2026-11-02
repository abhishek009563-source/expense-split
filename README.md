# ⚡ Expense Splitter — Smart Group Expense Management System

[![Java Version](https://img.shields.io/badge/Java-21%20LTS-orange.svg)](https://www.oracle.com/java/)
[![JavaFX](https://img.shields.io/badge/JavaFX-21.0.2-blue.svg)](https://openjfx.io/)
[![Database](https://img.shields.io/badge/Database-SQLite%203-003B57.svg)](https://sqlite.org/)
[![Build Tool](https://img.shields.io/badge/Build-Maven-C71A36.svg)](https://maven.apache.org/)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)
[![Tests](https://img.shields.io/badge/Unit%20Tests-25%20Passed-brightgreen.svg)](src/test/java/com/expensesplitter)

An enterprise-grade, modern finance management desktop application built with **Java 21**, **JavaFX**, and an embedded **SQLite 3** engine. Designed for friends, roommates, travel companions, and project teams to seamlessly track shared expenses, split bills with mathematical precision, minimize settlement transactions, and dispatch automatic real-time **Email & SMS Notifications** to all participants.

---

## 📸 Key Features

- **📊 Modern Finance Dashboard:**
  - 4 Real-time KPI summary cards (*Total Group Spending, Out-of-Pocket Paid, You Are Owed, You Owe*).
  - Interactive **Monthly Spending Trend Bar Chart** and **Category Distribution Pie Chart**.
  - Live **Recent Transactions Table** and **Active Minimized Debts Feed**.
- **👥 Multi-Group Collaboration:**
  - Create and manage isolated expense groups (*e.g., Goa Trip 2026, Flat 402 Roommates, Office Tech Team*).
  - Group-specific net balance ledgers and member role governance (*ADMIN / MEMBER*).
  - Add existing registered members or remove settled participants safely.
- **⚡ Three Flexible Bill Splitting Algorithms:**
  - **Equal Split:** Automatically divides the total bill among selected participants with *fair remainder allocation* to guarantee exact penny matching without precision loss.
  - **Exact Split:** Strict validation ensuring participant amounts sum up to 100% of the total.
  - **Percentage Split:** Validates participant percentage shares against 100.00% with automatic rounding adjustment.
- **🧮 Integer-Paise Minor Units Engine:**
  - Eliminates IEEE 754 floating-point drift by computing all monetary balances in 64-bit integer minor units (paise/cents).
- **📉 Greedy Debt Simplification (Min-Cash-Flow):**
  - Implements a priority-queue Min-Cash-Flow greedy algorithm reducing complex $N$-way circular debts into the minimum possible direct transactions.
- **🤝 Verified Settlement Management:**
  - Record payments (Cash, UPI, Net Banking) that immediately credit the receiver, debit the payer, and auto-dispatch settlement confirmation notifications.
- **📧 Automatic Email & SMS Notification System:**
  - **Expense Alerts:** Dispatched automatically upon expense creation/splitting with expense title, total bill, payer name, individual share, amount owed, group, and date.
  - **Settlement Alerts:** Double-sided notifications confirming payments to the debtor and alerting the creditor that their balance was credited.
  - **E.164 International Phone & RFC 5322 Email Validation:** Enforces valid country code (`+` followed by 7–15 digits) and standard email syntax.
  - **Multi-Gateway Support:** Integrates with **Twilio REST API**, **MSG91 REST API**, and direct **TLS SMTP** email servers.
  - **Resilient Asynchronous Worker:** Background thread pool ensures network latency or delivery failures *never* block the UI or halt database transactions.
  - **Zero Duplicates (Idempotency):** Unique database idempotency keys protect against duplicate dispatches.
  - **Notification History & Audit Log:** Dedicated dashboard showing recipient, contact, channel (`EMAIL`, `SMS`, `IN_APP`), status (`SENT`, `FAILED`, `PENDING`), error details, and a one-click **Retry Delivery** mechanism.
  - **Safe Simulation/Test Mode:** Works out of the box without requiring external API credentials.
- **📈 Advanced Analytics & CSV Export:**
  - Filter expenses by date ranges and specific groups.
  - One-click CSV export for offline auditing and Excel spreadsheet integration.
- **🌓 Dual-Theme Appearance:**
  - High-contrast **Dark Navy Executive Theme** and **Clean Modern Light Theme**.
  - Persisted user preferences across sessions.

---

## 🛠️ Technology Stack

| Layer | Technology | Details |
|---|---|---|
| **Language** | Java 21 LTS | Records, Pattern Matching, Modern Collections, Native HttpClient |
| **GUI Framework** | JavaFX 21.0.2 | Controls, FXML, Charts, CSS Stylesheets |
| **Database** | SQLite 3 | Embedded zero-configuration ACID database with WAL mode |
| **Database Driver** | SQLite JDBC 3.45.1.0 | Connection pooling and prepared statements |
| **Architecture** | Strict MVC Pattern | Separation of Models, Views (FXML), Services, and Controllers |
| **Gateways** | Twilio / MSG91 / SMTP | Native Java HTTP & TLS Sockets for SMS and Email delivery |
| **Packaging** | Maven Shade Plugin | Standalone executable Fat JAR (23.2 MB) |
| **Testing** | JUnit 5 Jupiter | 25 Unit Tests covering splits, debts, contact validation, templates, and delivery resilience |

---

## 🚀 Quick Start Guide

### Prerequisites
- **Java 17 or later** (Java 21 LTS recommended): Ensure `java -version` works in your terminal.
- **Apache Maven 3.8+** (Optional if using the pre-packaged executable JAR).

### 1. One-Click Launch (Windows)
Double-click `run.bat` inside the `expense_splitter` folder or run from PowerShell:
```cmd
run.bat
```

### 2. Standalone Executable JAR Launch
Run the pre-built Fat JAR directly:
```cmd
java -jar target\expense-splitter-1.0.0.jar
```

### 3. Build & Run from Source with Maven
```cmd
# Clean, compile, and run unit tests (25/25 passing)
mvn clean test

# Package standalone Fat JAR
mvn package -DskipTests

# Run using JavaFX Maven Plugin
mvn javafx:run
```

---

## ⚙️ Email & SMS Gateway Configuration

The notification engine automatically checks configuration in the following order of precedence:
1. **System Environment Variables**
2. **`.env` file** located in the application directory
3. **SQLite `app_settings` table** (editable via the application's Settings UI)

### 1. SMS Gateway Setup (Twilio or MSG91)

#### Option A: Twilio
Set the following environment variables:
```bash
export SMS_GATEWAY_PROVIDER=TWILIO
export TWILIO_ACCOUNT_SID=ACXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
export TWILIO_AUTH_TOKEN=your_auth_token_here
export TWILIO_PHONE_NUMBER=+1234567890
export NOTIFICATION_MODE=LIVE
```

#### Option B: MSG91
```bash
export SMS_GATEWAY_PROVIDER=MSG91
export MSG91_AUTH_KEY=your_msg91_auth_key
export MSG91_SENDER_ID=EXPSPL
export MSG91_TEMPLATE_ID=your_dlt_template_id
export NOTIFICATION_MODE=LIVE
```

### 2. Email Gateway Setup (SMTP)

Configure standard TLS/STARTTLS SMTP credentials:
```bash
export SMTP_HOST=smtp.gmail.com
export SMTP_PORT=587
export SMTP_USERNAME=your_email@gmail.com
export SMTP_PASSWORD=your_app_password
export SMTP_FROM_EMAIL=your_email@gmail.com
export NOTIFICATION_MODE=LIVE
```

### 3. Out-of-the-Box Simulation Mode (Default)

If no gateway credentials are provided or `NOTIFICATION_MODE=SIMULATION`, the application safely operates in **Simulation Mode**:
- Every notification is recorded in the SQLite database and UI Notification Dashboard.
- Formatted plain-text and HTML emails and SMS payloads are logged with simulation IDs (`SIM-EML-...`, `SIM-SMS-...`).
- No external network requests are made, preventing accidental billing or unwanted messages during local testing.

---

## 🔑 Demo Accounts (Pre-Seeded)

The application initializes SQLite (`expense_splitter.db`) on first launch with 4 sample users with valid E.164 phone numbers and pre-configured groups:

| Username | Password | Full Name | Phone (E.164) | Role / Profile |
|---|---|---|---|---|
| `rahul` | `rahul123` | Rahul Sharma | `+919876543210` | Trip Admin / Primary Creditor |
| `aman` | `aman123` | Aman Verma | `+919876543211` | Roommate / Debtor |
| `rohit` | `rohit123` | Rohit Kumar | `+919876543212` | College Friend |
| `priya` | `priya123` | Priya Patel | `+919876543213` | Colleague |

*You can edit your phone number, email, and notification consent at any time under **Settings**.*

---

## 📐 Mathematical Formulation & Algorithms

### 1. Net Balance Calculation
For any user $U$ within group $G$:
$$\text{Net Balance}(U) = \left( \sum \text{Paid By } U - \sum \text{Share Allocated to } U \right) + \left( \sum \text{Settled Given By } U - \sum \text{Settled Received By } U \right)$$

- If $\text{Net Balance}(U) > 0$: $U$ is a **creditor** (is owed money).
- If $\text{Net Balance}(U) < 0$: $U$ is a **debtor** (owes money).
- $\sum_{U \in G} \text{Net Balance}(U) \equiv 0$ (Conservation of value).

### 2. Greedy Min-Cash-Flow Algorithm
Instead of each member individually reimbursing every payer across all expenses:
1. Compute the net balance for each member.
2. Separate into two priority sets: **Debtors** ($net < 0$) and **Creditors** ($net > 0$).
3. Greedily match the largest debtor with the largest creditor:
   $$\text{Settlement Amount} = \min(|\text{Debtor Balance}|, |\text{Creditor Balance}|)$$
4. Update remaining balances and repeat until all net balances reach zero.
5. **Result:** Reduces up to $O(N^2)$ cross-payments down to at most $N-1$ optimal direct payments.

---

## 📁 Project Structure

```
expense_splitter/
├── pom.xml                               # Maven dependency and shade configuration
├── run.bat                               # Windows one-click launcher
├── README.md                             # Documentation & setup guide
├── src/
│   ├── main/
│   │   ├── java/com/expensesplitter/
│   │   │   ├── Launcher.java             # Fat JAR bootstrap launcher
│   │   │   ├── Main.java                 # JavaFX Application entrypoint
│   │   │   ├── controller/               # MVC Controllers
│   │   │   │   ├── LoginController.java
│   │   │   │   ├── RegisterController.java
│   │   │   │   ├── MainLayoutController.java
│   │   │   │   ├── DashboardController.java
│   │   │   │   ├── GroupsController.java
│   │   │   │   ├── AddExpenseController.java
│   │   │   │   ├── ExpensesController.java
│   │   │   │   ├── BalancesController.java
│   │   │   │   ├── SettlementsController.java
│   │   │   │   ├── ReportsController.java
│   │   │   │   ├── NotificationsController.java
│   │   │   │   └── SettingsController.java
│   │   │   ├── dao/                      # Data Access Objects (SQL Queries)
│   │   │   │   ├── UserDao.java
│   │   │   │   ├── GroupDao.java
│   │   │   │   ├── ExpenseDao.java
│   │   │   │   ├── SettlementDao.java
│   │   │   │   └── NotificationDao.java
│   │   │   ├── database/                 # SQLite Connection Pool & DDL Schema
│   │   │   │   ├── DatabaseManager.java
│   │   │   │   └── DatabaseInitializer.java
│   │   │   ├── engine/                   # Algorithmic & Math Engines
│   │   │   │   ├── SplitCalculator.java
│   │   │   │   └── DebtSimplifier.java
│   │   │   ├── model/                    # Data Transfer Objects & Enums
│   │   │   │   ├── User.java, Group.java, Expense.java, Settlement.java
│   │   │   │   ├── Notification.java, NotificationChannel.java, NotificationStatus.java
│   │   │   ├── service/                  # Business Logic & Notification Dispatchers
│   │   │   │   ├── AuthService.java, ExpenseService.java, SettlementService.java
│   │   │   │   ├── NotificationService.java, NotificationDispatcher.java
│   │   │   │   ├── NotificationTemplates.java, NotificationConfig.java
│   │   │   │   ├── EmailGateway.java, SmsGateway.java, DeliveryResult.java
│   │   │   └── util/                     # Formatters & Validators
│   │   │       ├── CurrencyFormatter.java
│   │   │       ├── ValidationUtils.java
│   │   │       ├── PasswordHasher.java
│   │   │       ├── SessionManager.java
│   │   │       └── CsvExporter.java
│   │   └── resources/
│   │       ├── css/                      # Application Stylesheets
│   │       │   ├── dark-theme.css
│   │       │   └── light-theme.css
│   │       └── fxml/                     # JavaFX GUI Layouts
│   │           ├── notifications.fxml, settings.fxml, dashboard.fxml, ...
│   └── test/java/com/expensesplitter/   # 25 Unit Tests (100% Passing)
│       ├── NotificationValidationTest.java
│       ├── NotificationTemplateAndDispatchTest.java
│       ├── SplitCalculatorTest.java
│       └── DebtSimplifierTest.java
```

---

## 🛡️ Security & Integrity
- Passwords are encrypted with salted **SHA-256** before database storage.
- All database interactions use parameter-bound **PreparedStatements** preventing SQL injection.
- Notification dispatch runs on asynchronous daemon threads, completely isolating GUI operations from network latency or third-party outages.
- Database idempotency keys (`EXPENSE_{id}_{channel}_{user}`, `SETTLE_{id}_{channel}_{user}`) prevent duplicate notifications.
- Participant opt-out preferences (`email_notifications_enabled`, `sms_notifications_enabled`) are honored for all outbound communications.
