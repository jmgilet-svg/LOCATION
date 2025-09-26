ALTER TABLE resource ADD COLUMN IF NOT EXISTS tags VARCHAR(255);
ALTER TABLE resource ADD COLUMN IF NOT EXISTS capacity_tons INTEGER;

CREATE TABLE IF NOT EXISTS recurring_unavailability (
  id VARCHAR(36) PRIMARY KEY,
  resource_id VARCHAR(36) NOT NULL REFERENCES resource(id),
  dow VARCHAR(10) NOT NULL,
  start_t TIME NOT NULL,
  end_t TIME NOT NULL,
  reason VARCHAR(140) NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_recurring_unav_resource_dow
  ON recurring_unavailability(resource_id, dow);

