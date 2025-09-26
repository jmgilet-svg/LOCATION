# LOCATION — Sprint 1 (Backend + Client complets)

Base **Spring Boot (Java 17)** + **Swing (FlatLaf)** prête :

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
