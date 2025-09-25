# LOCATION — Spring Boot + Swing

Base exécutable **backend Spring Boot (Java 17)** + **frontend Java Swing (FlatLaf)** avec **sélecteur de source Mock/Backend**, **SSE `/api/system/ping`**, **auth JWT `/auth/login`**, profils **dev(H2)** / **prod(Postgres)**, **Flyway**, **CI GitHub Actions**.

> **Diff 1** : squelette complet + docs + CI + mode Mock fonctionnel côté client + stratégie d’injection (DataSourceProvider).
>
> **Diff 2** : **Modèle & API** — entités, services avec **détection de chevauchement**, endpoints **`/api/v1/**`** (agences, clients, ressources, interventions), **DTOs stables**, **validation**, **erreurs structurées**, **migrations Flyway V2+** et **seeds** (dev). **Stubs export PDF & emailing**.

## Démarrage rapide

### Prérequis
- Java 17+
- Maven 3.9+

### Lancer le backend (profil dev / H2)
```bash
cd server
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```
Le backend expose :
- `POST /auth/login` → JWT (acceptation basique : identifiants non vides dans Diff 1)
- `GET  /api/system/ping` → SSE (un event ~toutes 15s)
- **API v1** (Diff 2) :
  - `GET /api/v1/agencies`
  - `GET /api/v1/clients`
  - `GET /api/v1/resources`
  - `GET /api/v1/interventions?from=...&to=...&resourceId=...`
  - `POST /api/v1/interventions` (validation + **anti-chevauchement**)
  - (stubs) `POST /api/v1/documents/{id}/export/pdf`, `POST /api/v1/documents/{id}/email`

Variables d’env utiles :
- `JWT_SECRET` (défaut: `dev-secret-please-change`)
- `SERVER_PORT` (via `server.port`, défaut: `8080`)
- `SPRING_PROFILES_ACTIVE` (`dev` ou `prod`)
- `SPRING_DATASOURCE_*` pour prod (Postgres)

### Lancer le client (Swing)
```bash
cd client
mvn -q exec:java -Dexec.mainClass=com.location.client.LocationClientApp
```
Au **premier lancement**, une **fenêtre de sélection** s’ouvre :
- **Mode Démo (Mock)** : données locales en mémoire (aucun réseau)
- **Mode Connecté (Backend)** : appels REST vers Spring Boot sécurisé (JWT)
- Case **“Mémoriser ce choix”** → persistance dans `~/.location/app.properties`

**Argument CLI prioritaire** :
```
--datasource=mock|rest
```
S’il est fourni, **la fenêtre est court-circuitée**.

**Badge d’état** en haut à droite de l’app : `MOCK` ou `REST`.

**Menu Paramètres → Source de données…** permet de changer la source (redémarrage conseillé).  
**Menu Données → Réinitialiser la démo** recharge le jeu de données Mock.

### Variables d’environnement côté client
- `LOCATION_BACKEND_URL` (défaut: `http://localhost:8080`)
- `LOCATION_DEMO_USER` / `LOCATION_DEMO_PASSWORD` (défaut: `demo` / `demo`)

## Profils & BDD
- **dev** : H2 embarqué, Flyway activé.
- **prod** : Postgres, Flyway activé. Config via `application-prod.yml` & variables d’env.

Flyway **V1** initialise le schéma minimal (placeholder). Le **modèle métier complet** arrivera en Diff 2 (migrations accréditives).

## CI
Une GitHub Action build & tests Maven :
- `mvn -B -ntp -DskipTests=false verify`
- Packaging des modules `server` et `client`.

## Sécurité (Diff 1)
- `/auth/login` : accepte tout couple `username/password` **non vides** et renvoie un JWT signé HS256 (secret env `JWT_SECRET`). Cette politique est **temporaire** pour accélérer la mise en place ; en **Diff 2**, on activera comptes/roles/validation.
- `/api/**` : protégé par filtre JWT ; `/auth/**` et `/api/system/ping` sont publics (ping SSE lisible sans auth pour observabilité de base).

## SSE Ping
`GET /api/system/ping` renvoie un **SSE** (content-type `text/event-stream`) avec un message `ping` ~toutes les 15s. Utile pour vérifier la connectivité depuis le client REST.

## Mode Mock (client)
- **Repositories/services in-memory** pour : `Agence`, `Client`, `Contact`, `Chauffeur`, `RessourceType`, `Ressource`, `Intervention`, `Document(Devis/Facture/BL)` (structures légères, étoffées en Diff 2).
- **Jeu de données cohérent** ré-initialisable via le menu.
- Parité visible avec REST : les écrans consommeront **uniquement** `DataSourceProvider` (interface) pour éviter les dépendances HTTP directes.

## Tests
- **Server** : tests WebMvc pour `/auth/login` et SSE `/api/system/ping`.
- **Client** : test unitaire sur la résolution du mode (argument CLI > préférences > dialogue).
- **Diff 2** :
  - Tests WebMvc `GET /api/v1/agencies` (200 + JSON)
  - Test DataJpa/Service création d’intervention **en conflit** → **exception 409** (via service + contrôleur)

## DTOs & Parité Mock/REST
Le client a été mis à jour pour consommer les listes **Agences** et **Clients** depuis `/api/v1/**` en mode REST. Le **mode Mock** expose les mêmes DTOs côté client (`Models.Agency`, `Models.Client`), garantissant la compatibilité d’affichage.

## Packaging
```bash
mvn -B -DskipTests package
```
Artifacts :
- `server/target/location-server.jar`
- `client/target/location-client.jar`

## Réinitialisation préférences client
Supprimer `~/.location/app.properties`.

## Prochaine itération (Diff 2)
- Modèle & API métier complets (`/api/v1/**`), validations, erreurs structurées, seeds, PDF HTML→PDF + emailing (abstraction) ; compat stricte DTO ↔ Mock.

---

Voir : `SPEC-FONCTIONNELLE.md`, `SPEC-TECHNIQUE.md`, `MODELE-DONNEES.md` (source de vérité).

## Notes Diff 2
- **Conflits d’affectation** : deux interventions d’une **même ressource** sont en conflit si les périodes `[start,end)` se chevauchent (intersection **non vide**).
- **Erreurs structurées** : format `application/json` : `{"timestamp": "...","status":409,"error":"Conflict","message":"...","path":"/api/v1/interventions"}`.
- **Seeds (dev)** : 2 agences, 3 clients, 3 ressources, 2 chauffeurs, 3 interventions non conflictuelles.
- **Exports** : endpoints présents, implémentation **stub** (retourne PDF minimal / email simulé en logs). 
