CREATE TABLE IF NOT EXISTS document_template (
  id VARCHAR(64) PRIMARY KEY,
  agency_id VARCHAR(64) NOT NULL,
  doc_type VARCHAR(16) NOT NULL,
  html TEXT NOT NULL,
  CONSTRAINT fk_document_template_agency FOREIGN KEY (agency_id) REFERENCES agency(id),
  CONSTRAINT uk_document_template_agency_type UNIQUE (agency_id, doc_type)
);
