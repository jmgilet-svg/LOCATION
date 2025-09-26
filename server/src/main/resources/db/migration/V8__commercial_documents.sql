CREATE TABLE commercial_document (
  id VARCHAR(64) PRIMARY KEY,
  type VARCHAR(16) NOT NULL,
  status VARCHAR(16) NOT NULL,
  doc_ref VARCHAR(64),
  doc_title VARCHAR(140),
  doc_date TIMESTAMPTZ,
  agency_id VARCHAR(64) NOT NULL,
  client_id VARCHAR(64) NOT NULL,
  total_ht NUMERIC(14,2) NOT NULL DEFAULT 0,
  total_vat NUMERIC(14,2) NOT NULL DEFAULT 0,
  total_ttc NUMERIC(14,2) NOT NULL DEFAULT 0,
  CONSTRAINT fk_document_agency FOREIGN KEY (agency_id) REFERENCES agency(id),
  CONSTRAINT fk_document_client FOREIGN KEY (client_id) REFERENCES client(id)
);

CREATE INDEX idx_document_type_date ON commercial_document(type, doc_date);
CREATE INDEX idx_document_client ON commercial_document(client_id);
CREATE INDEX idx_document_agency ON commercial_document(agency_id);

CREATE TABLE commercial_document_line (
  id VARCHAR(64) PRIMARY KEY,
  document_id VARCHAR(64) NOT NULL,
  line_no INT NOT NULL,
  designation VARCHAR(240) NOT NULL,
  quantity NUMERIC(14,3) NOT NULL,
  unit_price NUMERIC(14,2) NOT NULL,
  vat_rate NUMERIC(5,2) NOT NULL,
  CONSTRAINT fk_document_line_document FOREIGN KEY (document_id) REFERENCES commercial_document(id) ON DELETE CASCADE
);

CREATE INDEX idx_document_line_document ON commercial_document_line(document_id);
