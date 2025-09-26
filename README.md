# LOCATION — Sprint 1 (Backend + Client complets)

Base **Spring Boot (Java 17)** + **Swing (FlatLaf)** prête :

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

### Ce que livre ce patch (exécutable, côté **client**)
**K — Exports CSV**
- **Export Planning (jour)** en CSV (ressource, début, fin, titre, client).
- **Export Clients** en CSV.
- Accès via **Fichier → Export CSV (Planning jour)** et **Export CSV (Clients)**.

**L — Accessibilité & Internationalisation**
- **Taille de police ajustable** : `Paramètres → Police +`, `Police −`, `Police par défaut` (persistée).
- **Contraste élevé** (clair/sombre compatible) : `Paramètres → Contraste élevé` (persisté).
- **Bascule de langue** **Français/English** (persistée). Les libellés nouveaux utilisent le système i18n.

### Build & run (client)
```bash
mvn -pl client -DskipTests package
java -jar client/target/location-client.jar --datasource=mock   # ou --datasource=rest
```

## Auth & SSE
- `POST /auth/login` → `{ "token": "..." }`
