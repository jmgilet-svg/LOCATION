# LOCATION — Sprint 1 (Backend + Client complets)

Base **Spring Boot (Java 17)** + **Swing (FlatLaf)** prête :

## Sprint 4 — Filtres, Navigation Jour & Export CSV REST (Full)

### Nouvelles fonctionnalités
- **Navigation jour** (← / → et sélecteur de date) dans le planning.
- **Filtres** : Agence, Ressource, Client, Recherche texte (titre).
- **Export CSV côté serveur** : `GET /api/v1/interventions/csv?from&to&resourceId&clientId&q` (UTF‑8, `text/csv` avec `Content-Disposition`).
- **Export CSV côté client** (Mock ou REST) depuis menu Fichier ou `Ctrl+E`.
- **Préférences** : persistance des derniers filtres et de la dernière date (`~/.location/app.properties`).
- **Accessibilité** : focus clair, raccourcis `Ctrl+←/→` pour naviguer dans les jours.

### Démarrage rapide
```bash
mvn -B -ntp verify
mvn -pl server spring-boot:run -Dspring-boot.run.profiles=dev
# Client
mvn -pl client -DskipTests package && java -jar client/target/location-client.jar --datasource=mock
```

### Endpoints ajoutés
- `GET /api/v1/interventions/csv?from&to&resourceId&clientId&q` → CSV

### Tests
- WebMvc : CSV 200 + header `attachment`.
- UI : simple test de persistance des préférences.

## Auth & SSE
- `POST /auth/login` → `{ "token": "..." }`
