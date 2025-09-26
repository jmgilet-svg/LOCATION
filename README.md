# LOCATION — Sprint 1 (Backend + Client complets)

Base **Spring Boot (Java 17)** + **Swing (FlatLaf)** prête :

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
