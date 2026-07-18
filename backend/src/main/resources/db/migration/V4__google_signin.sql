-- Google sign-in accounts have no local password, and are linked by Google's own user id.
ALTER TABLE users ALTER COLUMN password_hash DROP NOT NULL;
ALTER TABLE users ADD COLUMN auth_provider VARCHAR(20) NOT NULL DEFAULT 'LOCAL' CHECK (auth_provider IN ('LOCAL', 'GOOGLE'));
ALTER TABLE users ADD COLUMN google_id VARCHAR(255) UNIQUE;
