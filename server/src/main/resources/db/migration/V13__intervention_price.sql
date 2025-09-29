ALTER TABLE intervention ADD COLUMN IF NOT EXISTS price_eur NUMERIC(14, 2);
UPDATE intervention SET price_eur = COALESCE(price_eur, 0);
