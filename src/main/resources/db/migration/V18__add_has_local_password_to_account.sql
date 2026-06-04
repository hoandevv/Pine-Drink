-- Track whether an account owns a real local password.
--
-- Google-created accounts still keep a random encoded placeholder password
-- because ia_account.password remains NOT NULL. This flag tells the business
-- layer whether the user can use the normal change-password flow.
--
-- Defaults to TRUE so existing LOCAL accounts keep current behavior.
ALTER TABLE ia_account
    ADD COLUMN has_local_password BOOLEAN NOT NULL DEFAULT TRUE AFTER provider_id;
