ALTER TABLE ch_message
    ADD COLUMN sender_type VARCHAR(30) NOT NULL DEFAULT 'CUSTOMER' AFTER sender_account_id;

UPDATE ch_message m
JOIN ch_room r ON r.id = m.room_id
SET m.sender_type = CASE
    WHEN m.sender_account_id = r.customer_account_id THEN 'CUSTOMER'
    WHEN r.assigned_staff_account_id IS NOT NULL AND m.sender_account_id = r.assigned_staff_account_id THEN 'STAFF'
    ELSE 'STAFF'
END;

CREATE INDEX idx_ch_room_customer_branch_status ON ch_room (customer_account_id, branch_id, status);
