INSERT INTO agency(id, name) VALUES
  ('A1', 'Agence 1'),
  ('A2', 'Agence 2')
ON CONFLICT (id) DO NOTHING;

INSERT INTO client(id, name, billing_email) VALUES
  ('C1', 'Client Alpha', 'facture@alpha.tld'),
  ('C2', 'Client Beta', 'billing@beta.tld'),
  ('C3', 'Client Gamma', 'compta@gamma.tld')
ON CONFLICT (id) DO NOTHING;

INSERT INTO resource(id, name, license_plate, color_rgb, agency_id) VALUES
  ('R1', 'Camion X', 'AB-123-CD', 16733572, 'A1'),
  ('R2', 'Grue Y', 'EF-456-GH', 4521988, 'A1'),
  ('R3', 'Remorque Z', 'IJ-789-KL', 44783, 'A2')
ON CONFLICT (id) DO NOTHING;

INSERT INTO intervention(id, title, start_ts, end_ts, agency_id, resource_id, client_id) VALUES
  ('I1', 'Livraison chantier', '2025-09-26T09:00:00Z', '2025-09-26T11:00:00Z', 'A1', 'R1', 'C1'),
  ('I2', 'Levage poutres', '2025-09-26T12:00:00Z', '2025-09-26T14:00:00Z', 'A1', 'R2', 'C2'),
  ('I3', 'Transport matériel', '2025-09-27T09:00:00Z', '2025-09-27T10:00:00Z', 'A2', 'R3', 'C3')
ON CONFLICT (id) DO NOTHING;
