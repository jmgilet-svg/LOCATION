CREATE TABLE IF NOT EXISTS doc_sequence (
  id VARCHAR(64) PRIMARY KEY,
  agency_id VARCHAR(64) NOT NULL,
  doc_year INT NOT NULL,
  doc_type VARCHAR(16) NOT NULL,
  last_no INT NOT NULL DEFAULT 0,
  CONSTRAINT fk_doc_sequence_agency FOREIGN KEY (agency_id) REFERENCES agency(id),
  CONSTRAINT uk_doc_sequence_agency_year_type UNIQUE (agency_id, doc_year, doc_type)
);

CREATE TABLE IF NOT EXISTS email_template (
  id VARCHAR(64) PRIMARY KEY,
  agency_id VARCHAR(64) NOT NULL,
  doc_type VARCHAR(16) NOT NULL,
  subject VARCHAR(180),
  body TEXT,
  CONSTRAINT fk_email_template_agency FOREIGN KEY (agency_id) REFERENCES agency(id),
  CONSTRAINT uk_email_template_agency_type UNIQUE (agency_id, doc_type)
);
