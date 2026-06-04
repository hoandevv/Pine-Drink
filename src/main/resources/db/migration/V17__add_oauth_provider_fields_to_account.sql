-- Add OAuth provider metadata to ia_account.
--
-- Purpose:
-- - Support Google login while keeping existing username/password login intact.
-- - Mark old accounts as LOCAL by default.
-- - Store Google user identifier in provider_id when auth_provider = 'GOOGLE'.
--
-- Column details:
-- - auth_provider: login source. Expected values: LOCAL, GOOGLE.
-- - provider_id: external provider user id. For Google, this is ID token claim "sub".
--
-- Constraint details:
-- - uk_ia_account_provider prevents duplicate accounts for same provider user.
-- - MySQL allows multiple NULL values in a unique key, so LOCAL accounts with
--   provider_id = NULL do not conflict.
-- - idx_ia_account_auth_provider supports filtering/reporting by login source.
--
-- Password remains NOT NULL in this migration. Google accounts will use a random
-- encoded placeholder password, so existing password login and validation code
-- do not break.
ALTER TABLE ia_account
    ADD COLUMN auth_provider VARCHAR(30) NOT NULL DEFAULT 'LOCAL' AFTER password,
    ADD COLUMN provider_id VARCHAR(150) NULL AFTER auth_provider,
    ADD UNIQUE KEY uk_ia_account_provider (auth_provider, provider_id),
    ADD INDEX idx_ia_account_auth_provider (auth_provider);
