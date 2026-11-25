ALTER TABLE users ADD COLUMN username VARCHAR(60) NULL UNIQUE AFTER full_name;
CREATE INDEX idx_users_phone ON users(phone);
