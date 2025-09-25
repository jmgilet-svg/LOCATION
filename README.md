# LOCATION — Sprint 1 (Backend + Client complets)

Base **Spring Boot (Java 17)** + **Swing (FlatLaf)** prête :

## Sprint 3 — UI Swing & Intégration (Full)

### Ce qui arrive dans ce patch

- **Planning** interactif (tuiles) par ressource (jour) avec **hover**, **drag**, **resize** (Mock actif ; REST en lecture seule pour éviter les API non disponibles).
- **Éditeurs intégrés** : création simple d’intervention depuis le planning (Mock) et via REST (POST) si pas de conflit (lecture/écriture limitée au POST).
- **Exports** : PDF (via endpoint REST de stub) + **CSV** local depuis le client.
- **Login REST** : boîte de dialogue (URL + credentials), JWT géré par `RestDataSource`.
- **Menus** :
  - **Paramètres** → *Changer de source de données* (Mock/REST) et *Configurer le backend (URL/identifiants)*.
  - **Données** → *Réinitialiser la démo* (Mock).
  - **Fichier** → *Exporter planning en CSV*, *Exporter document PDF (stub)*.
- **Accessibilité** : navigation clavier basique, contraste FlatLaf, tooltips.
- **Erreurs & retries** : toasts non intrusifs, messages clairs (409 conflit, 400 validation).

### Limitations connues (transparentes)
- **Drag/resize** persistants uniquement en **mode Mock** (pas d’endpoint PATCH/PUT côté REST dans le périmètre Sprint 2). En mode REST, les interactions sont simulées visuellement et **non enregistrées** – un toast l’indique.
- Export **PDF** utilise le stub `/api/v1/documents/{id}/export/pdf` (génère un mini-PDF de test).

### Démarrage rapide
```bash
mvn -B -ntp verify
mvn -pl server spring-boot:run -Dspring-boot.run.profiles=dev
# Autre terminal
mvn -pl client -DskipTests package && java -jar client/target/location-client.jar --datasource=mock
# ou REST
java -jar client/target/location-client.jar --datasource=rest
```

### Raccourcis (client)
- `Ctrl+N` : Nouvelle intervention (Mock ou REST via POST).
- `Ctrl+E` : Export CSV du planning affiché.
- `Ctrl+L` : Login/Config REST.

### Tests inclus
- Tests unitaires de **layout temporel** (pixel ↔ temps), et **détection de conflits** côté Mock.

## Auth & SSE
- `POST /auth/login` → `{ "token": "..." }`
