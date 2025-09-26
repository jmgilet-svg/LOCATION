# LOCATION — Sprint 1 (Backend + Client complets)

Base **Spring Boot (Java 17)** + **Swing (FlatLaf)** prête :

## Étape 11 — Modèles documents versionnés (HTML) + Email groupé (full Back/Front/Mock)

### Nouveautés
**Backend**
- **DocumentTemplate** (par **agence** + **type de document**) : stockage HTML (version active).
- API `/api/v1/templates/doc/{docType}` :
  - `GET` → modèle actif (ou vide).
  - `PUT` → enregistre/active un modèle (remplace l’actif).
- **Email groupé** : `POST /api/v1/docs/email-batch` avec `{ "ids":[...], "to":"client@x.tld", "subject":"", "message":"" }`.
  - Utilise l’API de génération PDF existante + fusion des modèles email si `subject/message` vides (cf. Étape 5).
- **Flyway** `V11__document_template.sql`.

**Client Swing**
- **Éditeur de modèles document** (HTML) : **Paramètres → Modèles document** (par type : Devis/Commande/BL/Facture).
  - Sauvegarde côté backend (ou **mock** en mémoire).
- **Email groupé** : dans la fenêtre **Documents**, bouton **Email groupé** → saisie d’un destinataire, envoi pour tous les IDs indiqués (sélection via champ texte — minimaliste et fiable dans toutes les variantes de table).

**Mock**
- Persistance **in‑memory** des modèles document + no‑op pour les emails groupés.

### Remarques
Le rendu PDF continue d’utiliser le gabarit par défaut. Les modèles HTML stockés sont prêts pour intégration au moteur HTML→PDF (prochaine étape d’activation transparente).

## Étape 10 — Indisponibilités récurrentes + Tags & Capacité des ressources (full Back/Front/Mock)

### Livré dans cette étape
**Backend**
- Gestion d’**indisponibilités ponctuelles** + **hebdomadaires** par ressource :
  - Entités `Unavailability` (créneaux datés) et `RecurringUnavailability` (jour + horaire).
  - Service `UnavailabilityQueryService` qui **développe** les récurrences dans un intervalle `[from,to]`.
- API `/api/v1` enrichie :
  - `GET /unavailabilities?from&to&resourceId` → occurrences (ponctuelles + hebdo expandées).
  - `POST /unavailabilities` → créer une indispo ponctuelle.
  - `POST /recurring-unavailabilities` → créer une indispo hebdomadaire.
- Ressources : champs `tags` (filtrables) + `capacityTons` exportés dans le CSV.
- Migration Flyway `V11__unavailabilities_and_resource_extras.sql` (tables + colonnes).

**Client Swing**
- **PlanningPanel** consomme les indispos ponctuelles et récurrentes via `DataSourceProvider.listUnavailabilities`.
- Écran **Indisponibilités** : création d’un créneau ponctuel ou récurrent (Mock + REST).
- Ressources : modèles enrichis (`tags`, `capacityTons`) exploitables dans les filtres et tooltips.

**Mock**
- Stockage en mémoire avec logique d’**expansion hebdomadaire** identique au backend.

### API rapide
```http
GET /api/v1/unavailabilities?resourceId=R1&from=2025-09-01T00:00:00Z&to=2025-09-08T00:00:00Z
POST /api/v1/unavailabilities
POST /api/v1/recurring-unavailabilities
```

## Étape 9 — Exports CSV généralisés (Clients, Ressources, Interventions) + UI d’export

### Ce que cette étape apporte
**Backend**
- Nouveaux endpoints **CSV** (UTF‑8, `;`, filtrés par **X-Agency-Id**) :
  - `GET /api/v1/clients.csv`
  - `GET /api/v1/resources.csv`
  - `GET /api/v1/interventions.csv?from=ISO&to=ISO`

**Client Swing**
- Nouveau menu **Fichier → Exporter…** ouvrant un **ExportDialog** :
  - Choix du type (**Clients** / **Ressources** / **Interventions**).
  - Pour **Interventions**, sélection d’un **intervalle de dates**.
  - Télécharge le CSV en REST et l’**ouvre** automatiquement.
  - En **Mock**, un message explique que l’export disque est désactivé (conformément aux invariants).

**Mock**
- Pas d’écriture fichier : levée `UnsupportedOperation` identique à l’étape 5.

### Utilisation
1) Lancer le backend en `dev` ou la stack Docker.
2) Côté client en REST, **Fichier → Exporter…**, choisir un type, valider : un `.csv` s’ouvre.

## Étape 8 — Planning interactif (drag, move, resize, hover) + détection visuelle des conflits

### Livrables
**Client Swing**
- Nouveau **PlanningPanel** interactif (jour / semaine) :
  - **Glisser‑déposer** : déplacer une intervention sur la grille ou la **redimensionner** (poignées haut/bas).
  - **Création rapide** par **double‑clic** sur un créneau (choix client/ressource/chauffeur).
  - **Conflits** : si le backend (ou le mock) renvoie **409**, la tuile clignote en **rouge** et l’action est **annulée** avec un message.
  - **Tooltips** riches au survol (client, ressource, chauffeur, durée, HT estimée si dispo).
  - **Zoom Jour/Semaine** via `Affichage → Zoom` (existant), le layout s’adapte.
- Intégration avec `DataSourceProvider` existant (`listInterventions`, `saveIntervention`, `deleteIntervention`).  
  Compatible **REST** et **Mock** (les règles de conflit sont déjà présentes côté Mock).

**Backend / Mock**
- Aucun changement d’API requis dans cette étape (on s’appuie sur `/api/v1/interventions` livrée en Étape 7).

### Utilisation
- Ouvrir l’appli, aller dans **Affichage** → **Zoom** (Jour / Semaine), puis interagir :
  - **Déplacer** : drag sur la tuile → la requête `PUT /interventions/{id}` est appelée.
  - **Redimensionner** : saisir les poignées haut/bas.
  - **Double‑clic** pour créer une intervention : un petit formulaire s’ouvre.

## Étape 7 — Interventions (API v1) + Détection de conflits (ressource & chauffeur)

### Ce que cette étape livre
**Backend (Spring Boot)**
- Entité `Intervention` enrichie (agence, client, ressource, chauffeur, titre, début/fin, notes).
- Service `InterventionService` avec **détection de chevauchements** (ressource **et** chauffeur) :
  - Un conflit est levé si un créneau `[start,end)` intersecte un autre sur la même ressource ou le même chauffeur.
  - Erreur structurée **409 CONFLICT** (`AssignmentConflictException`) avec le détail.
- Endpoints `/api/v1/interventions` :
  - `GET /interventions?from=...&to=...&resourceId=...` (ISO date-time) — filtré par agence (`X-Agency-Id`).
  - `POST /interventions` — crée après validation de non-conflit.
  - `PUT /interventions/{id}` — met à jour avec validation.
  - `DELETE /interventions/{id}` — supprime.
- Migration Flyway `V10__drivers_and_intervention_conflicts.sql` (table `driver`, colonne `driver_id` + index).

**Client Swing**
- Les appels REST (`RestDataSource`) gèrent désormais le champ `driverId` pour lister/créer/mettre à jour/supprimer.
- `MockDataSource` reproduit la **logique de conflit** (ressource ou chauffeur) pour garder la parité fonctionnelle.

**Mock**
- Jeux de données enrichis avec chauffeurs fictifs et affectations pour tester les collisions.

### Exemples rapides
```bash
# Liste (jour/semaine)
curl -H "Authorization: Bearer <JWT>" -H "X-Agency-Id: A" \
  "http://localhost:8080/api/v1/interventions?from=2025-09-01T00:00:00Z&to=2025-09-08T00:00:00Z"

# Création (409 si conflit ressource/chauffeur)
curl -X POST -H "Authorization: Bearer <JWT>" -H "X-Agency-Id: A" -H "Content-Type: application/json" \
  -d '{"agencyId":"A","clientId":"C1","resourceId":"R1","driverId":"D1","title":"Levage X","start":"2025-09-02T07:00:00Z","end":"2025-09-02T12:00:00Z"}' \
  http://localhost:8080/api/v1/interventions
```

## Étape 6 — Auth côté client (auto-renouvellement) + Dialog de connexion (full Front)

### Pourquoi maintenant ?
Pour stabiliser l’usage **Backend sécurisé JWT** côté client : si le jeton expire, le client
**se reconnecte automatiquement** en réutilisant les identifiants, puis **rejoue** la requête.
On ajoute aussi une **fenêtre de connexion** pour saisir/modifier les identifiants à chaud.

> Remarque : le backend conserve `/auth/login` tel qu’existant. Cette étape se concentre
> sur la **robustesse côté client** (auto re-login + UI) et ne modifie pas la sécurité serveur.

### Ce que ça livre
- **Client Swing**
  - `LoginDialog` : saisie **username/password** et mémorisation en mémoire du process.
  - `RestDataSource` :
    - Stocke identifiants + **token**.
    - Sur **401 Unauthorized**, effectue un **re-login** automatique, puis **retry** (1 fois).
    - Méthode `setCredentials(user, pass)` exposée, et menu **Paramètres → Connexion…**.
  - Badges/menus existants inchangés.

### Variables d’environnement (fallback par défaut)
- `LOCATION_USERNAME` (par défaut: `demo`)
- `LOCATION_PASSWORD` (par défaut: `demo`)

### Utilisation
1) Lancer l’app en **REST**.
2) Menu **Paramètres → Connexion…** : saisir `username/password` si besoin.
3) En cas d’expiration, le client se reconnecte et **rejoue** la requête automatiquement.

## Étape 5 — Numérotation, Modèles d’email par agence, Export CSV (full Back/Front/Mock)

### Nouveautés clés
**Backend**
- **Numérotation documentaire par agence + année** (`doc_sequence`) :
  - `DV-YYYY-####` pour **Devis**, `BC-YYYY-####` **Commandes**, `BL-YYYY-####` **BL**, `FA-YYYY-####` **Factures**.
  - Attribution automatique : **Devis** à la création, **Commande/BL/Facture** lors de la **transition**.
- **Modèles d’email par agence & type** (`email_template`) avec fusion `{{agencyName}}`, `{{clientName}}`, `{{docRef}}`, `{{docTitle}}`, `{{docType}}`, `{{docDate}}`, `{{totalTtc}}`.
- **Export CSV** des documents : `GET /api/v1/docs.csv?type&from&to&clientId` (encodage UTF‑8, séparateur `;`).

**Client Swing**
- **Bouton Export CSV** dans la fenêtre **Documents** (REST uniquement, Mock affiche message).
- **Éditeur de modèles email** (menu **Paramètres → Modèles email**).
  - Choix agence (courante) + type, édition `Sujet` / `Corps`, sauvegarde côté backend (ou mock in‑memory).

**Mock**
- Numérotation simulée et persistée en mémoire par agence+année+type.
- Modèles email stockés en mémoire avec exemples par défaut.

### Démarrage rapide
```bash
mvn -B -ntp verify
mvn -pl server spring-boot:run -Dspring-boot.run.profiles=dev
mvn -pl client -DskipTests package && java -jar client/target/location-client.jar --datasource=rest
```

## Étape 4 — Qualité & Packaging (CI + Docker + Compose + Gzip)

### Ce que cette étape livre
- **CI GitHub Actions** : build Maven, tests, **Checkstyle** & **PMD** (qualité), build image Docker du **server**.
- **Packaging Docker** :
  - `server/Dockerfile` multi‑stage (build JAR puis image runtime légère).
  - `docker-compose.yml` avec **Postgres** (prod), **Mailhog** (dev emails), et **server** attaché au réseau.
- **Configs Spring** :
  - `application.yml`: **compression gzip** activée pour les réponses HTTP.
  - `application-prod.yml`: datasource **Postgres** via variables d’environnement.

### Démarrage rapide (prod-like)
```bash
# 1) Construire le JAR
mvn -B -ntp -DskipTests package

# 2) Construire l'image serveur
docker build -t location-server:latest ./server

# 3) Lancer l'ensemble (Postgres + Mailhog + Server)
docker compose up -d

# Server: http://localhost:8080
# Mailhog UI: http://localhost:8025
```

### Variables d’environnement (server)
- `SPRING_PROFILES_ACTIVE=prod`
- `DB_URL=jdbc:postgresql://db:5432/location`
- `DB_USER=location`
- `DB_PASS=location`
- `JWT_SECRET` (obligatoire en prod)

## Étape 3 — Multi-agences opérationnel (enforcement `X-Agency-Id` + UI de bascule)

### Ce que cette étape apporte
- **Backend**
  - Intercepteur global qui **exige** l’en-tête `X-Agency-Id` sur **toutes** les routes `/api/v1/**`.
  - Contexte requête `AgencyContext` (ThreadLocal) pour accéder à l’agence courante dans les contrôleurs/services.
  - Requêtes **scopées** par agence (exemple livré : `/api/v1/docs` inclut un filtre `agencyId`).
  - Réponse **400** si l’en-tête est absent, **403** si l’agence n’existe pas.

- **Client Swing**
  - **Sélecteur d’agence** (menu **Contexte** → **Agence**).
  - **Badge** d’état indiquant **Mock/REST** + **Agence**.
  - Le client **envoie** `X-Agency-Id` à chaque appel REST.

- **Mock**
  - Contexte agence côté client respecté ; les listes sont **filtrées** sur l’agence courante.

### Utilisation
1) Choisir l’agence via **Contexte → Agence**.  
2) En mode REST, toutes les requêtes porteront `X-Agency-Id` et seront filtrées côté serveur.

## Étape 2 — Cycle Devis → Commande → BL → Facture (Full, Back + Front + Mock)

### Objectif
Livrer un **cycle documentaire complet** avec **documents commerciaux** (Devis/Commande/BL/Facture) + **lignes**,
**PDF** (OpenPDF) et **envoi email**, côté **Backend**, **Client Swing** et **Mock**.

### Backend
- **Modèle unique** `CommercialDocument` avec `DocType {QUOTE, ORDER, DELIVERY, INVOICE}` et `DocStatus` (DRAFT, SENT, ACCEPTED, CANCELLED, ISSUED, PAID).
- **Lignes** `CommercialDocumentLine` (designation, qty, unitPrice, vatRate).
- **Calculs** : HT/TVA/TTC recalculés service-side.
- **Migrations**: `V8__commercial_documents.sql`.
- **Endpoints** `/api/v1/docs` :
  - `GET /docs` (filtre type, clientId, from/to, q), `POST /docs`, `GET /docs/{id}`, `PUT /docs/{id}` (maj lignes & méta), `DELETE /docs/{id}`.
  - `POST /docs/{id}/transition?to=ORDER|DELIVERY|INVOICE` : duplique et convertit (ex: devis→commande).
  - `GET  /docs/{id}/pdf` → `application/pdf` (gabarit simple, logo en option non inclus pour l’instant).
  - `POST /docs/{id}/email` (envoi PDF du doc au client).
- **Tests WebMvc** de base : création, pdf, transition.

### Client Swing
- Nouveau menu **Documents** :
  - **Liste** filtrable par type (Devis/Commande/BL/Facture) et par client.
  - **Édition rapide** (titre, ref, lignes) — minimal mais fonctionnel.
  - Actions : **Créer**, **Dupliquer/Convertir**, **Exporter PDF**, **Envoyer email**.

### Mock
- Jeu de données démos (2 devis, 1 facture).
- Implémentation des opérations (CRUD, transition, export simulé indisponible en fichier — cf. règle Mock : pas d’écriture disque).

### Démarrage rapide
```bash
mvn -B -ntp verify
mvn -pl server spring-boot:run -Dspring-boot.run.profiles=dev
mvn -pl client -DskipTests package && java -jar client/target/location-client.jar --datasource=rest
```

## Étape 1 — Planning “pro” (drag, resize, hover, zoom jour/semaine, filtres)

### Ce que ça livre (résumé)
- **Client Swing**
  - **Drag & Drop** des interventions (déplacement horaire et changement de ressource au vol).
  - **Resize** au bord gauche/droit de la tuile pour ajuster la durée.
  - **Tooltips enrichis** (titre, client, ressource, date/heure, notes).
  - **Zoom Jour/Semaine** via menu *Affichage* ou raccourcis `Ctrl+1` (jour) / `Ctrl+2` (semaine).
  - **Filtres rapides** (agence, ressource, client, recherche texte, tags) synchronisés avec la barre supérieure.
  - **Détection de conflits** : tentative de sauvegarde → appel REST ; conflit (`409`) ⇒ rollback local + message ou bip clavier.
  - **Accessibilité** : flèches `← → ↑ ↓` pour décaler de 15 min, `Alt+↑/↓` pour changer de ressource.

> Côté **Backend**, la logique de conflit existante (`InterventionService`) est réutilisée ; aucune évolution serveur n'est requise.

### Raccourcis
- Drag: maintenir la souris sur la tuile, déplacer horizontalement pour changer l'horaire, verticalement pour changer de ressource.
- Resize: saisir un bord (curseur ↔) et tirer.
- `Ctrl+1` jour, `Ctrl+2` semaine ; `Ctrl+E` notes ; `Ctrl+M` email PDF ; flèches pour nudges, `Alt+↑/↓` pour changer de ressource.

### Limitations connues
- La vue s'appuie sur une plage de 12h (07h→19h) et un pas de 15 minutes.
- La vue semaine affiche 7 colonnes (lun→dim) avec la même granularité.

## Sprint 12 — CSV Clients & Indispos + Feature Flags API + Aide/À propos (Full, Mock-safe)

Dernier sprint d’endurcissement et de “petits plus” utiles.

### Backend
- **Exports CSV additionnels** :
  - `GET /api/v1/clients/csv` → liste complète des clients (`id;name;billingEmail`).
  - `GET /api/v1/unavailabilities/csv?from=&to=&resourceId=` → exports des indispos étendues (inclut les récurrentes dépliées quand `from..to` fournis).
- **Feature Flags** : `GET /api/v1/system/features` retourne les flags calculés depuis les variables d’environnement (préfixe `FEATURE_...`).
  - Exemples: `FEATURE_EMAIL_BULK`, `FEATURE_RESOURCES_CSV`, `FEATURE_INTERVENTION_PDF`.

### Client Swing
- **Menu Fichier** : nouveaux exports **Clients CSV** et **Indisponibilités CSV** (REST uniquement). En mode **Mock**, un message explique la limitation (pas d’écriture disque).
- **Menu Aide** → **À propos & fonctionnalités serveur** : affiche les versions, la source de données active, et les **feature flags** exposés par le backend. En Mock, affiche une liste par défaut.

### Mock
- Aucune écriture disque : les exports REST sont **non disponibles** (message clair). Les features sont **simulées** : `EMAIL_BULK=true`, `RESOURCES_CSV=true`, `INTERVENTION_PDF=true`.

### Tests
- WebMvc : entêtes et content-type pour `clients/csv` et `unavailabilities/csv`.


### Démarrage rapide
```bash
mvn -B -ntp verify
mvn -pl server spring-boot:run -Dspring-boot.run.profiles=dev
mvn -pl client -DskipTests package && java -jar client/target/location-client.jar --datasource=rest
```

## Sprint 10 — Export PDF Intervention + Envoi Email (Full, REST) — Mock sûr (sans écriture disque)

### Nouveautés
**Backend (Spring Boot)**
- **PDF Intervention** : `GET /api/v1/interventions/{id}/pdf` → `application/pdf` (OpenPDF).
- **Email PDF** : `POST /api/v1/interventions/{id}/email` avec `{ "to": "...", "subject": "...", "message": "..." }` → `202 Accepted` et envoi via `MailGateway` (pièce jointe PDF).
- **Service PDF** : rendu simple (titre, client, ressource, dates).
- **Tests WebMvc** : vérif `application/pdf` + email `202`.

**Client (Swing)**
- Menu **Fichier → Exporter Intervention (PDF)** (REST uniquement) : télécharge le PDF et l’ouvre.
- Menu **Données → Envoyer PDF intervention par email** (REST & Mock) :
  - En REST : appel direct de l’API d’envoi.
  - En Mock : **simulation** (succès immédiat, aucune écriture disque conformément aux règles Mock).

**Mode Mock**
- Pas d’écriture fichier. L’envoi email est simulé côté client.

### Utilisation
1. Sélectionnez une intervention dans le planning.
2. **Exporter PDF** pour obtenir le fichier (REST).
3. **Envoyer PDF par email** pour expédier au destinataire (REST réel, Mock simulé).

### Démarrage
```bash
mvn -B -ntp verify
mvn -pl server spring-boot:run -Dspring-boot.run.profiles=dev
mvn -pl client -DskipTests package && java -jar client/target/location-client.jar --datasource=rest

```

## Sprint 8 — Indisponibilités récurrentes + Tags/Capacité ressources + Export CSV (Full, Mock-ready)

### Nouveautés
**Backend**
- **Indisponibilités récurrentes** hebdomadaires (jour de semaine + heure début/fin, motif).
  - Entité `RecurringUnavailability`, endpoints :
    - `GET  /api/v1/recurring-unavailabilities?resourceId`
    - `POST /api/v1/recurring-unavailabilities`
  - **Recherche d’indisponibilités** (`GET /api/v1/unavailabilities`) agrégée : retourne **à la fois** fixes et récurrentes **dépliées** sur la fenêtre `from..to`.
- **Ressources : tags & capacité** (p. ex. `["grue","90t"]`, capacité en tonnes).
  - Migration DB + DTO + filtre `tags` optionnel : `GET /api/v1/resources?tags=grue,90t`
  - **Export CSV** des ressources : `GET /api/v1/resources/csv?tags=`

**Client Swing**
- Top bar : **filtre par tag** (champ texte, séparés par virgules).
- Menu Fichier : **Exporter Ressources (CSV)** via REST.
- Menu Données : **Nouvelle indisponibilité récurrente** (dialog).
- Planning : les **indisponibilités récurrentes** apparaissent comme bandes hachurées (couleur plus claire).

**Mode Mock**
- Parité : stockage **in-memory** des récurrentes, expansion dans `listUnavailabilities`, tags/capacité sur ressources, filtre par tags.

### Endpoints ajoutés
- `GET/POST /api/v1/recurring-unavailabilities`
- `GET /api/v1/resources/csv?tags=`
- `GET /api/v1/resources?tags=`

### Migrations DB
- `V5__resource_tags_capacity_and_recurring_unav.sql`

### Tests
- Service : expansion récurrente dans une fenêtre donnée.
- WebMvc : `resources/csv` renvoie `text/csv` + header `attachment`.

### Utilisation rapide
```bash
mvn -B -ntp verify
mvn -pl server spring-boot:run -Dspring-boot.run.profiles=dev
mvn -pl client -DskipTests package && java -jar client/target/location-client.jar --datasource=mock
```

## Sprint 7 — Édition REST (drag/resize), Suppression & Conflits (Full)

### Nouveautés
- **Backend REST**
  - `PUT   /api/v1/interventions/{id}` : mise à jour (titre, agence, ressource, client, start/end).
  - `DELETE /api/v1/interventions/{id}` : suppression d’une intervention.
  - **Règles de conflit** conservées : recouvrement entre interventions et **indisponibilités** (`409 Conflict`).
  - Repo : `existsOverlapExcluding(id, resourceId, start, end)` pour éditer sans se confondre soi‑même.
  - Tests WebMvc : update OK + update en conflit → 409 ; delete → 204.
- **Client Swing**
  - **Drag/resize** maintenant **persistants en REST** (PUT).
  - **Suppression** d’une intervention sélectionnée (touche `Suppr`/`Delete` ou menu Données).
  - **Mock** : mêmes opérations en mémoire.
  - Retours utilisateurs (toast / erreurs) et rechargement du planning.

### Raccourcis
- `Ctrl+N` nouvelle intervention (Mock déjà pris en charge).
- **Drag/Resize** sur les tuiles → sauvegarde (REST/Mock).
- `Suppr` / `Delete` → supprimer l’intervention sélectionnée.

### Endpoints ajoutés
- `PUT /api/v1/interventions/{id}`
- `DELETE /api/v1/interventions/{id}`

### Tests
- `InterventionUpdateWebTest` (update OK + conflit + delete OK).

### Démarrage rapide
```bash
mvn -B -ntp verify
mvn -pl server spring-boot:run -Dspring-boot.run.profiles=dev
mvn -pl client -DskipTests package && java -jar client/target/location-client.jar --datasource=rest
```

## Sprint 6 — Emailing PDF (stub), Sélection & Actions (Full)

### Nouveautés
- **Emailing de PDF** (stub **dev**) côté backend :
  - Endpoint `POST /api/v1/documents/{id}/email` body `{to,subject,body}` → `202 Accepted`.
  - Service `MailGateway` abstrait ; impl **DevMailGateway** loggue l’envoi (sans SMTP).
  - Réutilise l’export PDF existant (stub) pour simuler la pièce jointe.
- **Client Swing** :
  - **Sélection d’intervention** par clic dans le planning (tuile avec halo).
  - Action **“Envoyer PDF par email…”** (menu *Fichier*) : saisie destinataire/objet/corps, envoi via REST.
  - En **mode Mock**, l’action affiche un message de simulation.
- **Prefs** : mémorisation du dernier destinataire (`lastEmailTo`).

### Comment utiliser
1. Lancer le serveur (profil dev) et le client en REST.
2. Cliquer une tuile pour la sélectionner.
3. Menu **Fichier → Envoyer PDF par email…** ; renseigner le mail ; valider.

### Endpoints ajoutés
- `POST /api/v1/documents/{id}/email`

### Tests
- WebMvc : email → `202 Accepted` et payload requis.

```bash
mvn -B -ntp verify
mvn -pl server spring-boot:run -Dspring-boot.run.profiles=dev
mvn -pl client -DskipTests package && java -jar client/target/location-client.jar --datasource=rest
```

## Auth & SSE
- `POST /auth/login` → `{ "token": "..." }`
