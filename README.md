# LOCATION — Sprint 1 (Backend + Client complets)

Base **Spring Boot (Java 17)** + **Swing (FlatLaf)** prête :

## UX++ Tranche M+N — Tags & Capacités Ressources + Indisponibilités récurrentes

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

### Build & run (client)
```bash
mvn -pl client -DskipTests package
java -jar client/target/location-client.jar --datasource=mock   # ou --datasource=rest
```

## Auth & SSE
- `POST /auth/login` → `{ "token": "..." }`
