CREATE TABLE credential_vault (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    app_title VARCHAR(120) NOT NULL,
    app_url VARCHAR(255),
    login_username VARCHAR(150) NOT NULL,
    encrypted_password VARCHAR(512) NOT NULL,
    category VARCHAR(50) DEFAULT 'GENERAL',
    notes VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_vault_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_vault_user (user_id)
);
