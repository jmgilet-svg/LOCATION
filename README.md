# LOCATION — Sprint 1 (Backend + Client complets)

Base **Spring Boot (Java 17)** + **Swing (FlatLaf)** prête :

## Sprint 5 — Indisponibilités Ressource, Conflits & UI (Full)

### Nouveautés
- **Indisponibilités** (maintenance, panne, congés chauffeur) par ressource :
  - Backend : entité `Unavailability` + endpoints **REST** `/api/v1/unavailabilities` (`GET`, `POST`).
  - **Règles de conflit** : création d'une **intervention** refusée si elle chevauche une indisponibilité de la ressource (**409 Conflict**).
  - **Règles d'intégrité** : interdiction de créer une indisponibilité qui chevauche une intervention existante (409).
- **UI Planning** : affichage des indisponibilités en **bandes rouges hachurées** derrière les tuiles, tooltip, et **menu** pour en créer.
- **Mode Mock** : parité fonctionnelle (liste/création + détection de conflits).
- **Prefs** : inchangées (les filtres/date continuent d'être persistés).

### Endpoints ajoutés
- `GET  /api/v1/unavailabilities?from&to&resourceId`
- `POST /api/v1/unavailabilities` (validation + anti-chevauchement avec interventions et autres indispos)

### Migrations DB
- `V4__unavailability.sql`

### Tests
- Service : intervention refusée si chevauche **indisponibilité** (409).
- WebMvc : création d’indisponibilité refusée en cas de chevauchement avec intervention existante (409).

### Utilisation rapide
```bash
mvn -B -ntp verify
mvn -pl server spring-boot:run -Dspring-boot.run.profiles=dev
mvn -pl client -DskipTests package && java -jar client/target/location-client.jar --datasource=mock
```

## Auth & SSE
- `POST /auth/login` → `{ "token": "..." }`
