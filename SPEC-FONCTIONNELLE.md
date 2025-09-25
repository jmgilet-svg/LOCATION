# SPEC-FONCTIONNELLE — LOCATION

## Parcours de démarrage
1. L’utilisateur lance l’app Swing.
2. Si `--datasource=mock|rest` **n’est pas** fourni :
   - Une **fenêtre de sélection** s’affiche avec :
     - Bouton **Mode Démo (Mock)** — données locales, pas de réseau.
     - Bouton **Mode Connecté (Backend)** — REST vers Spring Boot (JWT).
     - Case **Mémoriser ce choix** — persistance dans `~/.location/app.properties`.
3. Si `--datasource=...` est fourni, **pas de fenêtre** (le choix CLI est prioritaire).
4. L’app affiche son **shell** avec un **badge d’état** `MOCK` ou `REST`.

## Exigences Mode Démo (Mock)
- Jeu de données **local**, **ré-initialisable** (menu Données → Réinitialiser la démo).
- **Aucune écriture disque** (hors préférences).
- Parité **visible** avec le mode Backend (listages, détails indispensables pour démo).
- Respect des **invariants métier** connus (unicité id, cohérence dates, etc.).

## Exigences Mode Backend (Connecté)
- Authentification via `POST /auth/login` → JWT (stocké côté client en mémoire).
- Les appels REST portent les en-têtes requis (ex: `Authorization: Bearer ...`, futurs `X-Agency-Id` en multi-agences).
- `GET /api/system/ping` permet de **sonder** le backend via SSE.

## Périmètre fonctionnel (base Diff 1)
- **Shell** d’application :
  - Barre de menu : **Fichier**, **Données**, **Paramètres**, **Aide**.
  - Badge d’état (Mock/Backend).
  - Zone centrale **placeholder** (les écrans riches arrivent Diff 3).
- **Paramètres** :
  - Sélecteur de **source de données** (Mock/Backend) — propose de redémarrer.
  - Affichage de l’URL backend courante (env `LOCATION_BACKEND_URL`).
- **Données** :
  - **Réinitialiser la démo** (Mock uniquement).

## Objets fonctionnels (léger, Diff 1)
- **Agence** : id, nom.
- **Client** : id, raison sociale, email facturation.
- **Contact** : id, clientId, nom, email, téléphone.
- **Chauffeur** : id, nom.
- **RessourceType** : id, libellé (ex: grue, camion, remorque).
- **Ressource** : id, typeId, nom, immatriculation, couleurRGB, agenceId, visible.
- **Intervention** : id, agenceId, ressourceId, clientId, start, end, titre.
- **Document** (Devis/Facture/BL) : id, type, date, clientId, total.

Ces structures suffisent pour **démonstration** et seront **stabilisées** en Diff 2 avec DTOs versionnés `/api/v1/**`.

## Règles métier (base)
- Identifiants uniques (UUID).
- **Chevauchements** et règles d’affectation : **placeholder** en Diff 1 (vérifications élémentaires côté Mock si besoin). **Implémentation stricte** en Diff 2 (côté service).

## Export & Email (capteurs)
- Non inclus fonctionnellement en Diff 1 (prévu en Diff 2/3). Les éléments techniques (SSE, JWT, injection) sont prêts.

## Accessibilité & i18n
- Application en **français** par défaut.
- Formats date/heure/monnaie FR.

## Erreurs & feedback
- Mode Backend : affichage d’erreurs de connexion (auth, HTTP).
- Mode Mock : aucune erreur réseau ; exceptions affichées avec message compréhensible.

## Données de démo
- 2 agences, 3 ressources, 3 clients, 2 chauffeurs, 3 interventions, 3 documents.
- Réinitialisation reconstruisant les listes (UUID nouveaux ou régénérés).

## Cas d’usage couverts par Diff 1
1. **Sélection du mode** au démarrage + mémorisation + forçage par CLI.
2. **SSE ping** lisible (connectivité backend).
3. **Auth** obtenue (JWT) côté client en mode Backend (stub credentials).
4. **Affichage d’un shell Swing** prêt pour intégrer les écrans (Diff 3).
5. **Données mock** disponibles via `DataSourceProvider` (listages basiques).

## Hors périmètre Diff 1 (reporté)
- Éditeurs intégrés, planning tuiles (drag/resize/hover), exports PDF/CSV, emailing, règles de chevauchement strictes, pagination/tri côté API — **prévu** Diff 2/3.

---

## (Nouveau) Périmètre Diff 2 — Modèle & API

### Endpoints **`/api/v1/**`**
- `GET /agencies` : liste des agences.
- `GET /clients` : liste des clients.
- `GET /resources` : liste des ressources (grue/camion/remorque…).
- `GET /interventions?from&to&resourceId` : liste filtrée par période et/ou ressource.
- `POST /interventions` : crée une intervention **avec détection de conflits**.
- (stubs) `POST /documents/{id}/export/pdf` : renvoie un PDF minimal (stub).
- (stubs) `POST /documents/{id}/email` : simule l’envoi (logs).

### Règles — **Chevauchement**
Deux interventions de **même ressource** (resourceId) sont déclarées en **conflit** si :

`[start, end)` **∩** `[start', end')` **≠ ∅** (intervalle demi-ouvert).

→ Le service refuse la création/modification et le contrôleur renvoie **409 Conflict** avec une **erreur structurée**.

### Validation
- `start < end` ; champs obligatoires `agenceId`, `resourceId`, `clientId`, `titre` (taille ≤ 140).

### Seeds (profil `dev`)
- 2 agences, 3 clients, 3 ressources, 2 chauffeurs, 3 interventions **non conflictuelles**.

### Parité Mock/REST (client)
- En mode REST, le client consomme `/api/v1/agencies` et `/api/v1/clients`.
- En mode Mock, même rendu via `MockDataSource`.

### Erreurs structurées
Format JSON générique :
```json
{
  "timestamp": "2025-09-24T10:37:42Z",
  "status": 409,
  "error": "Conflict",
  "message": "Intervention en conflit pour la ressource R-123",
  "path": "/api/v1/interventions"
}
```
