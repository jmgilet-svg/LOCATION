CREATE TABLE IF NOT EXISTS resource_type (
  id VARCHAR(36) PRIMARY KEY,
  name VARCHAR(80) NOT NULL UNIQUE,
  icon_name VARCHAR(120) NOT NULL
);

ALTER TABLE resource ADD COLUMN IF NOT EXISTS resource_type_id VARCHAR(36);
ALTER TABLE resource
  ADD CONSTRAINT IF NOT EXISTS fk_resource_type
  FOREIGN KEY (resource_type_id) REFERENCES resource_type(id);

INSERT INTO resource_type(id, name, icon_name) VALUES
  ('RT_GRUE', 'Grue', 'crane.svg'),
  ('RT_CAMION', 'Camion', 'truck.svg'),
  ('RT_REMORQUE', 'Remorque', 'trailer.svg')
ON CONFLICT (id) DO NOTHING;

UPDATE resource SET resource_type_id = 'RT_CAMION' WHERE id = 'R1' AND resource_type_id IS NULL;
UPDATE resource SET resource_type_id = 'RT_GRUE' WHERE id = 'R2' AND resource_type_id IS NULL;
UPDATE resource SET resource_type_id = 'RT_REMORQUE' WHERE id = 'R3' AND resource_type_id IS NULL;
