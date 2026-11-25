# MyFinance

Premium dark-themed multi-user finance management starter built with Java 17, Spring Boot 3.5, Thymeleaf, Spring Security and MySQL 8.

## Included in this starter
- User registration, BCrypt password hashing, login/logout and authenticated dashboard
- User/role schema and per-user ownership model
- Premium dark responsive UI with animated gradients, hover lift, page entrance and reveal effects
- MySQL schema for income/expense transactions, people, loans, repayments, EMI schedules, reminders and audit logs
- Environment-variable based database configuration

## Requirements
- JDK 17+
- Maven 3.9+
- MySQL 8+

## Run
1. Create the database and base schema:
   ```sql
   SOURCE database/schema.sql;
   ```
2. Set environment variables (Windows PowerShell):
   ```powershell
   $env:DB_URL="jdbc:mysql://localhost:3306/myfinance_db?serverTimezone=Asia/Kolkata"
   $env:DB_USERNAME="root"
   $env:DB_PASSWORD="your_mysql_password"
   ```
   Or configure them in your IDE run configuration.
3. Run:
   ```bash
   mvn clean spring-boot:run
   ```
4. Open http://localhost:8080/register and create your first account.
5. Login at http://localhost:8080/login.

## Important
- `spring.jpa.hibernate.ddl-auto=validate`: schema must exist before startup.
- The public registration endpoint always assigns `USER`; never accepts a role from the browser.
- The `ADMIN` role is seeded but no public admin registration is provided.
- Financial CRUD screens and EMI amortization/payment workflows are the next implementation phase; this archive provides their schema foundations, not completed financial modules.
- Before production: add database migrations (Flyway), email verification/password reset, rate limiting, secure cookie settings, HTTPS, backups, audit coverage, and integration tests. Review access controls before storing real financial data.
