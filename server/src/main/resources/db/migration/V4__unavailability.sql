CREATE TABLE IF NOT EXISTS unavailability (
  id VARCHAR(36) PRIMARY KEY,
  resource_id VARCHAR(36) NOT NULL REFERENCES resource(id),
  start_ts TIMESTAMP WITH TIME ZONE NOT NULL,
  end_ts   TIMESTAMP WITH TIME ZONE NOT NULL,
  reason   VARCHAR(140) NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_unav_resource_start_end ON unavailability(resource_id, start_ts, end_ts);
