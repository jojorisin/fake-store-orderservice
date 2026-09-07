ALTER TABLE orders RENAME COLUMN stripe_session_id TO payment_reference;
ALTER TABLE orders ADD COLUMN payment_type VARCHAR(100);