ALTER TABLE agency ADD COLUMN IF NOT EXISTS email_subject_template VARCHAR(255);
ALTER TABLE agency ADD COLUMN IF NOT EXISTS email_body_template TEXT;
