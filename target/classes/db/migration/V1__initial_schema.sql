
-- =====================================
-- 1. ROLES
-- =====================================

CREATE TABLE roles (
                       id BIGINT PRIMARY KEY AUTO_INCREMENT,
                       name VARCHAR(50) NOT NULL UNIQUE
);

-- =====================================
-- 2. USERS
-- =====================================

CREATE TABLE users (
                       id BIGINT PRIMARY KEY AUTO_INCREMENT,
                       full_name VARCHAR(120) NOT NULL,
                       email VARCHAR(150) NOT NULL UNIQUE,
                       password VARCHAR(255) NOT NULL,
                       phone VARCHAR(20),
                       enabled BOOLEAN NOT NULL DEFAULT TRUE,
                       created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                       updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                           ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE user_roles (
                            user_id BIGINT NOT NULL,
                            role_id BIGINT NOT NULL,
                            PRIMARY KEY (user_id, role_id),
                            FOREIGN KEY (user_id) REFERENCES users(id),
                            FOREIGN KEY (role_id) REFERENCES roles(id)
);

INSERT INTO roles(name) VALUES ('USER'), ('ADMIN');

-- =====================================
-- 3. CATEGORIES
-- =====================================

CREATE TABLE categories (
                            id BIGINT PRIMARY KEY AUTO_INCREMENT,
                            user_id BIGINT NOT NULL,
                            name VARCHAR(100) NOT NULL,
                            type VARCHAR(20) NOT NULL,
                            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                            FOREIGN KEY (user_id) REFERENCES users(id),
                            UNIQUE KEY uk_category_user_name (user_id, name, type)
);

-- =====================================
-- 4. TRANSACTIONS
-- =====================================

CREATE TABLE transactions (
                              id BIGINT PRIMARY KEY AUTO_INCREMENT,
                              user_id BIGINT NOT NULL,
                              category_id BIGINT,
                              type VARCHAR(20) NOT NULL,
                              amount DECIMAL(15,2) NOT NULL,
                              description VARCHAR(500),
                              transaction_date DATE NOT NULL,
                              payment_method VARCHAR(40),
                              created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                              FOREIGN KEY (user_id) REFERENCES users(id),
                              FOREIGN KEY (category_id) REFERENCES categories(id),
                              INDEX idx_transaction_user_date (user_id, transaction_date)
);

-- =====================================
-- 5. PERSONS
-- =====================================

CREATE TABLE persons (
                         id BIGINT PRIMARY KEY AUTO_INCREMENT,
                         user_id BIGINT NOT NULL,
                         name VARCHAR(120) NOT NULL,
                         phone VARCHAR(20),
                         email VARCHAR(150),
                         notes VARCHAR(500),
                         created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                         FOREIGN KEY (user_id) REFERENCES users(id),
                         INDEX idx_person_user (user_id)
);

-- =====================================
-- 6. LOANS
-- =====================================

CREATE TABLE loans (
                       id BIGINT PRIMARY KEY AUTO_INCREMENT,
                       user_id BIGINT NOT NULL,
                       person_id BIGINT NULL,
                       loan_type VARCHAR(20) NOT NULL,
                       direction VARCHAR(20) NOT NULL,
                       lender_or_bank VARCHAR(150),
                       principal_amount DECIMAL(15,2) NOT NULL,
                       annual_interest_rate DECIMAL(7,4) DEFAULT 0,
                       tenure_months INT,
                       start_date DATE NOT NULL,
                       end_date DATE,
                       status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
                       notes VARCHAR(500),
                       created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                       FOREIGN KEY (user_id) REFERENCES users(id),
                       FOREIGN KEY (person_id) REFERENCES persons(id),
                       INDEX idx_loan_user (user_id)
);

-- =====================================
-- 7. LOAN PAYMENTS
-- =====================================

CREATE TABLE loan_payments (
                               id BIGINT PRIMARY KEY AUTO_INCREMENT,
                               loan_id BIGINT NOT NULL,
                               amount DECIMAL(15,2) NOT NULL,
                               principal_component DECIMAL(15,2) DEFAULT 0,
                               interest_component DECIMAL(15,2) DEFAULT 0,
                               payment_date DATE NOT NULL,
                               payment_method VARCHAR(40),
                               reference_number VARCHAR(100),
                               notes VARCHAR(500),
                               created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                               FOREIGN KEY (loan_id) REFERENCES loans(id),
                               INDEX idx_payment_loan_date (loan_id, payment_date)
);

-- =====================================
-- 8. EMI SCHEDULE
-- =====================================

CREATE TABLE emi_schedules (
                               id BIGINT PRIMARY KEY AUTO_INCREMENT,
                               loan_id BIGINT NOT NULL,
                               installment_number INT NOT NULL,
                               due_date DATE NOT NULL,
                               principal_amount DECIMAL(15,2) NOT NULL,
                               interest_amount DECIMAL(15,2) NOT NULL,
                               total_amount DECIMAL(15,2) NOT NULL,
                               paid_amount DECIMAL(15,2) DEFAULT 0,
                               status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
                               paid_date DATE,
                               FOREIGN KEY (loan_id) REFERENCES loans(id),
                               UNIQUE KEY uk_loan_installment (loan_id, installment_number)
);

-- =====================================
-- 9. REMINDERS
-- =====================================

CREATE TABLE reminders (
                           id BIGINT PRIMARY KEY AUTO_INCREMENT,
                           user_id BIGINT NOT NULL,
                           loan_id BIGINT,
                           title VARCHAR(150) NOT NULL,
                           reminder_date DATE NOT NULL,
                           status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
                           FOREIGN KEY (user_id) REFERENCES users(id),
                           FOREIGN KEY (loan_id) REFERENCES loans(id)
);

-- =====================================
-- 10. AUDIT LOGS
-- =====================================

CREATE TABLE audit_logs (
                            id BIGINT PRIMARY KEY AUTO_INCREMENT,
                            user_id BIGINT NOT NULL,
                            action VARCHAR(100) NOT NULL,
                            entity_name VARCHAR(100),
                            entity_id BIGINT,
                            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                            FOREIGN KEY (user_id) REFERENCES users(id)
);
INSERT IGNORE INTO roles(name)
VALUES ('USER'), ('ADMIN');