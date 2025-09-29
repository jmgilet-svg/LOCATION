# LOCATION — Sprint 1 (Backend + Client complets)

Base **Spring Boot (Java 17)** + **Swing (FlatLaf)** prête :

## ✨ Pack Imagination — Couleurs par ressource, export iCalendar/PNG, signets & générateur de données

### Ce que livre ce patch (exécutable, côté **client**)
- **Couleurs par ressource** : chaque grue/camion/remorque est colorée dans le planning (édition rapide via Outils → Couleurs des ressources).
- **Exports supplémentaires** :
  - **ICS** (iCalendar) du **planning du jour** — importable dans Google/Outlook/Apple Calendar.
  - **PNG** du planning — capture instantanée du composant Swing.
- **Signets de jour** ⭐ : ajoute le jour courant à une liste de signets pour naviguer instantanément.
- **Générateur de données** (stress test) : création de lots d’interventions aléatoires (Mock/REST via `createIntervention`) pour tester performance et conflits.

### Menus
- **Fichier → Export ICS (Planning jour)** ; **Fichier → Export PNG (Planning)**.
- **Affichage → Signets → Ajouter le jour courant** + navigation vers les jours enregistrés.
- **Outils → Générer des interventions…** (choix du nombre et de la fenêtre horaire) et **Outils → Couleurs des ressources…**.

## UX++ Tranche O+P — Historique (Undo/Redo) & Reprise de session

### Ce que livre ce patch (exécutable, côté **client**)
**O — Historique d’actions (Undo/Redo)**
- `Ctrl+Z` **Annuler** et `Ctrl+Y` **Rétablir** sur les opérations **Planning** suivantes :
  - Déplacement, Création (`Ctrl+N`/double-clic), Duplication (`Ctrl+D`), Suppression (**Suppr**), Édition rapide (double-clic).
- Barre de status affiche des **toasts** “Annulé” / “Rétabli”. Pile d’historique par fenêtre.

**P — Reprise de session & préférences UI**
- **Reprise** du dernier jour de planning, **géométrie** de la fenêtre principale (taille/position), **thème/taille police/contraste** (déjà livrés) restaurés au démarrage.
- Persistance via `~/.location/app.properties` :
  - `window.x`, `window.y`, `window.w`, `window.h`, `planning.lastDay`.

## UX++ Tranche K+L — Exports CSV & Accessibilité + i18n (FR/EN)


### Ce que livre ce patch (exécutable, Mock complet)
**M — Ressources enrichies**
- Éditeur **Ressource** (nom, plaque/type, **capacité**, **tags**).
- **Recherche** globale qui s’appuie sur les tags (déjà exploités par la recherche globale).
- Export CSV existant enrichi automatiquement ; ici on ajoute l’édition et la persistance Mock.

**N — Indisponibilités récurrentes (ressources)**
- Nouveau gestionnaire **Indisponibilités** : ajout/suppression de plages pour une ressource, **récurrence** `Aucune` ou `Hebdo` (jour/heure).
- Intégration **Planning** :
  - Rendu des plages indisponibles (bandeaux rouges semi-transparents).
  - **Blocage côté client** : avant enregistrement/déplacement, contrôle anti‑chevauchement avec les indispos (message clair).

> REST : les méthodes existent mais renvoient une erreur explicite si non implémentées côté backend ; tout fonctionne en **Mock**.


## Backend Spring Boot — Ressource Types persistés

### Démarrer le backend (profil **dev** H2 + Flyway)
```bash
mvn -pl server -DskipTests spring-boot:run
```
Le backend écoute sur `http://localhost:8080` et expose notamment :
- `GET /api/v1/resource-types`
- `POST /api/v1/resource-types` (création/mise à jour)
- `DELETE /api/v1/resource-types/{id}`
- `GET /api/v1/resources/{id}/type`
- `PUT /api/v1/resources/{id}/type`
- `DELETE /api/v1/resources/{id}/type`

### Production (PostgreSQL)
Configurer les variables d’environnement puis lancer le jar :
```bash
export DB_URL='jdbc:postgresql://host:5432/location'
export DB_USER='location'
export DB_PASS='***'
mvn -pl server -DskipTests package
java -Dspring.profiles.active=prod -jar server/target/location-server.jar
```

### Client en mode REST
Dans le client, choisissez **Mode Connecté (Backend)** au démarrage ou lancez :
```bash
java -jar client/target/location-client.jar --datasource=rest
```
Le client consommera les *Resource Types* depuis le backend.

### Build & run (client)
```bash
mvn -pl client -DskipTests package
java -jar client/target/location-client.jar --datasource=mock   # ou --datasource=rest
```

## Auth & SSE
- `POST /auth/login` → `{ "token": "..." }`
