ALTER TABLE event_outbox ADD COLUMN failed_at TIMESTAMPTZ;
CREATE INDEX idx_event_outbox_pending ON event_outbox (id) WHERE failed_at IS NULL;
