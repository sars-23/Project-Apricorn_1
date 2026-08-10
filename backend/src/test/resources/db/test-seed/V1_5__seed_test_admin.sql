-- Seed admin user (id=1) required by V6 problem seeds.
-- Runs AFTER V1 (users table created).
-- This file exists ONLY in src/test/resources/db/test-seed/ and is ONLY loaded in the H2 test DB.

INSERT INTO users (id, username, email, password, role, enabled, created_at, updated_at)
SELECT 1, 'admin', 'admin@apricorn.com', '$2a$12$placeholder_test_hash_only_not_real', 'ADMIN', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM users WHERE id = 1);

ALTER TABLE users ALTER COLUMN id RESTART WITH 1000;
