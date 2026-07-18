-- Tracks whether the "expiring within 3 days" notification (§12) has already fired for a batch,
-- so the daily scheduled check doesn't re-notify every day the batch remains in that window.
ALTER TABLE inventory_batches ADD COLUMN expiry_notified_at TIMESTAMPTZ;
