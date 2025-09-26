CREATE TABLE IF NOT EXISTS driver (
  id VARCHAR(36) PRIMARY KEY,
  name VARCHAR(128) NOT NULL,
  email VARCHAR(160)
);

ALTER TABLE intervention
  ADD COLUMN IF NOT EXISTS driver_id VARCHAR(36);

ALTER TABLE intervention
  ADD CONSTRAINT IF NOT EXISTS fk_intervention_driver FOREIGN KEY (driver_id) REFERENCES driver(id);

CREATE INDEX IF NOT EXISTS idx_intervention_driver_start_end ON intervention(driver_id, start_ts, end_ts);
