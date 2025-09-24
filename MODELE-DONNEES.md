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
