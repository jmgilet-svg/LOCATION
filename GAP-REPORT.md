# GAP REPORT — Audit Étape 0

Inventaire exhaustif de toutes les spécifications (fonctionnelles, techniques, modèle de données), avec statut actuel.

## SPEC-FONCTIONNELLE.md
- **Parcours démarrage / Mode Mock vs Backend** : ✅ conforme (fenêtre choix + CLI + prefs).
- **Planning interactif (drag, resize, hover, conflits)** : ⚠️ partiel (interactions ok, mais conflits visuels seulement).
- **Éditeurs intégrés (Intervention, Client, Ressource)** : ⚠️ partiel (Intervention présent, Client/Ressource à finir).
- **Cycle Devis → Commande → BL → Facture** : ❌ manquant (CRUD, états, PDF, envoi).
- **Filtres/recherche/sidebars épinglables** : ⚠️ partiel (filtres date/tag/ressource ok, recherche/sidebars non).
- **Exports PDF/CSV/Excel** : ⚠️ partiel (Intervention PDF ok, CSV ressources/clients/unav ok, devis/factures non).
- **Envoi email (unitaire et groupé)** : ⚠️ partiel (interventions ok, devis/factures non).
- **Internationalisation (FR par défaut, prêt i18n)** : ⚠️ partiel (FR uniquement, pas de bundles).
- **Accessibilité (clavier, contrastes, tooltips)** : ⚠️ partiel (raccourcis présents, audit manquant).
- **Multi-agences** : ⚠️ partiel (agences supportées, mais scoping/ACL incomplètes).
- **Indisponibilités récurrentes, tags, capacités** : ⚠️ partiel (tags + récurrentes ok, suggestions capacité manquantes).

## SPEC-TECHNIQUE.md
- **Architecture Spring Boot + Swing** : ✅ conforme (multi-modules parent/server/client).
- **Profils H2 dev / Postgres prod** : ✅ conforme (Flyway actif, seeds dev).
- **Endpoints versionnés /api/v1/** : ✅ conforme (tous exposés sous /api/v1).
- **Validation Bean Validation** : ⚠️ partiel (présente sur DTOs clés, manquante sur certains champs).
- **Sécurité JWT** : ⚠️ partiel (login ok, refresh/roles manquants).
- **SSE /api/system/ping** : ✅ conforme (ping 15s actif).
- **Erreurs structurées (codes HTTP, format uniforme)** : ⚠️ partiel (409 conflits ok, autres hétérogènes).
- **Exports PDF** : ⚠️ partiel (interventions ok, devis/factures à implémenter).
- **Emails via MailGateway** : ⚠️ partiel (stub dev ok, file/rétries manquants).
- **Multi-agences (X-Agency-Id)** : ⚠️ partiel (header défini, enforcement incomplet).
- **CI GitHub Actions build+test** : ✅ conforme (présent dans .github/workflows).
- **Qualité code (Spotless/Checkstyle/PMD)** : ❌ manquant (non intégré).
- **Packaging (Dockerfile, profiles, secrets env)** : ⚠️ partiel (profiles ok, docker/compose manquant).
- **Tests** : ⚠️ partiel (WebMvc/DataJpa ok, couverture < 80%). 
- **Perf (index, pagination, gzip)** : ⚠️ partiel (indexes manquants, pagination partielle).

## MODELE-DONNEES.md
- **Agence** : ✅ conforme (id, name, email templates).
- **Client** : ✅ conforme (id, name, billingEmail).
- **Contact** : ❌ manquant (entité non implémentée).
- **RessourceType** : ⚠️ partiel (implémenté minimal, champs capacité/tag à étendre).
- **Ressource** : ⚠️ partiel (id, type ok, capacité/tag à compléter).
- **Chauffeur/Main d’œuvre** : ❌ manquant (entités non définies).
- **Intervention** : ⚠️ partiel (notes ok, validations partiels).
- **Unavailability (simple & récurrente)** : ✅ conforme.
- **Devis/Commande/BL/Facture** : ❌ manquant (entités/lignes/migrations à créer).
- **Migrations Flyway** : ✅ en cours (V1..V7 présents, V8+ à prévoir pour manquants).
- **Invariants (unicité, contraintes)** : ⚠️ partiel (peu de contraintes DB, validations côté service seulement).

---

## Résumé des gaps
- Manquants critiques : cycle devis→facture, contacts, chauffeurs, lignes docs commerciaux, sécurité rôles/refresh, qualité code CI.
- Partiels : planning pro (perf, conflits bloquants), multi-agences, exports complets, i18n, accessibilité, packaging/perf. 
- Conformes : base démarrage, Mock/Backend parity visible, SSE, JWT simple, modules, seeds, CSV/PDF initiaux.

---

# Statut global
- **Conforme** : 30 %
- **Partiel** : 50 %
- **Manquant** : 20 %

Plan d’action détaillé dans BACKLOG.md.
