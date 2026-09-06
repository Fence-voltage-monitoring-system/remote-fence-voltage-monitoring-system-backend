ALTER TABLE alerts ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE alerts ADD COLUMN healthy_readings_received INTEGER NOT NULL DEFAULT 0;
ALTER TABLE alerts ADD COLUMN last_observation_at TIMESTAMPTZ;
CREATE INDEX idx_alerts_assignment_deadline ON alerts (assignment_status, acceptance_deadline);
ALTER TABLE alerts ADD COLUMN attempted_assignees TEXT;
