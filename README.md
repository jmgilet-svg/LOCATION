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

### Build & run (client)
```bash
mvn -pl client -DskipTests package
java -jar client/target/location-client.jar --datasource=mock   # ou --datasource=rest
```

## Auth & SSE
- `POST /auth/login` → `{ "token": "..." }`
