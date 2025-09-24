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
