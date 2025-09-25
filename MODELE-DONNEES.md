# MODELE-DONNEES — LOCATION

## Vue d’ensemble
Le modèle **complet** (agences, clients, ressources, interventions, documents) sera livré en **Diff 2**.  
En **Diff 1**, nous livrons :
- Le **socle DB** (Flyway V1) pour valider le pipeline et les profils.
- Les **DTOs légers** côté client pour le mode Mock.

## Schéma (Diff 1)
```
-- V1__init.sql
create table if not exists app_info (
  id int primary key,
  name varchar(64) not null,
  created_at timestamp not null default current_timestamp
);
insert into app_info (id, name) values (1, 'LOCATION') on conflict do nothing;
```

> Objectif : valider Flyway, profils, et connexions.  
> Le reste des tables (agences, clients, ressources, etc.) arrivent en Diff 2 avec index pertinents (dates, ressources, agence).

## Objets Mock ↔ DTO REST (cible Diff 2)
- **Agency** ↔ `/api/v1/agencies`
- **Client** ↔ `/api/v1/clients`
- **Contact** ↔ `/api/v1/clients/{id}/contacts`
- **Driver** ↔ `/api/v1/drivers`
- **ResourceType** ↔ `/api/v1/resource-types`
- **Resource** ↔ `/api/v1/resources`
- **Intervention** ↔ `/api/v1/interventions`
- **Document** (`QUOTE`, `ORDER`, `DELIVERY`, `INVOICE`) ↔ `/api/v1/documents`

## Invariants (à affermir en Diff 2)
- Identifiants UUID (ou ULID).
- Intégrité de référence (`agency_id`, `client_id`, `resource_id`).
- Unicités utiles : immatriculation ressource, nom agence, etc.
- Index : `(agency_id, start_date)`, `(resource_id, start_date)`, `(doc_type, date)`.

## Migrations
- **V1** : app_info (placeholder).
- **V2+** : ajout progressif des entités + index + contraintes.

## Seeds (dev)
- En Diff 2 : seeds cohérents insérés via migrations conditionnelles ou scripts `data-dev.sql`.  
  Diff 1 n’en a pas besoin au-delà d’`app_info`.

---

## (Nouveau) Schéma Diff 2

### Tables principales
- `agency(id uuid pk, name text unique not null)`
- `client(id uuid pk, name text not null, billing_email text)`
- `resource_type(id uuid pk, label text not null)`
- `resource(id uuid pk, type_id uuid fk, name text not null, license_plate text unique, color_rgb int, agency_id uuid fk, visible boolean not null)`
- `driver(id uuid pk, name text not null)`
- `contact(id uuid pk, client_id uuid fk, name text not null, email text, phone text)`
- `intervention(id uuid pk, agency_id uuid fk, resource_id uuid fk, client_id uuid fk, start_ts timestamp not null, end_ts timestamp not null, title text not null)`
- `document(id uuid pk, doc_type text not null, date_ts date not null, client_id uuid fk, total_cents bigint not null)`

### Index clés
- `idx_intervention_resource_start (resource_id, start_ts)`
- `idx_intervention_agency_start (agency_id, start_ts)`
- `idx_resource_agency (agency_id)`

### Règle métier (service)
Conflit si **intersection non vide** entre deux interv. de **même ressource** sur `[start,end)`.

### Seeds (dev)
- 2 agences (Agence 1, Agence 2)
- 3 clients (Alpha, Beta, Gamma)
- 3 ressources (Camion X, Grue Y, Remorque Z)
- 3 interventions non conflictuelles
