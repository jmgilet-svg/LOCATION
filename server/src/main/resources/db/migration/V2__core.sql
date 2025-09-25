CREATE TABLE IF NOT EXISTS agency (
  id VARCHAR(36) PRIMARY KEY,
  name VARCHAR(128) NOT NULL
);

CREATE TABLE IF NOT EXISTS client (
  id VARCHAR(36) PRIMARY KEY,
  name VARCHAR(128) NOT NULL,
  billing_email VARCHAR(160) NOT NULL
);

CREATE TABLE IF NOT EXISTS resource (
  id VARCHAR(36) PRIMARY KEY,
  name VARCHAR(128) NOT NULL,
  license_plate VARCHAR(32),
  color_rgb INT,
  agency_id VARCHAR(36) NOT NULL REFERENCES agency(id)
);

CREATE TABLE IF NOT EXISTS intervention (
  id VARCHAR(36) PRIMARY KEY,
  title VARCHAR(140) NOT NULL,
  start_ts TIMESTAMP WITH TIME ZONE NOT NULL,
  end_ts   TIMESTAMP WITH TIME ZONE NOT NULL,
  agency_id   VARCHAR(36) NOT NULL REFERENCES agency(id),
  resource_id VARCHAR(36) NOT NULL REFERENCES resource(id),
  client_id   VARCHAR(36) NOT NULL REFERENCES client(id)
);

CREATE INDEX IF NOT EXISTS idx_intervention_resource_start_end ON intervention(resource_id, start_ts, end_ts);
