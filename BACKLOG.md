# BACKLOG FINAL — Étape 0

Liste exhaustive des tâches issues du GAP-REPORT, classées Must / Should / Could.

## Must (bloquants avant Go-Live)
- Implémenter cycle **Devis → Commande → BL → Facture** complet (CRUD, états, numérotation, PDF/email). 
- Créer entités manquantes : **Contact**, **Chauffeur/Main d’œuvre**, lignes docs commerciaux.
- Sécurité : **JWT refresh + rôles (admin/planif/compta/lecture)**, enforcement **X-Agency-Id** strict. 
- Qualité : intégrer **Spotless/Checkstyle/PMD**, >80% couverture tests.
- Packaging : **Dockerfile multi-stage**, `docker-compose` (server+db+mailhog), secrets via env vars.
- Multi-agences : scoping stricte, ACL, badge de contexte permanent.

## Should (nécessaires pour confort/valeur)
- Planning : conflits bloquants, zoom temporel, perfs (5k tuiles/semaine).
- Filtres avancés : recherche globale, sidebars épinglables, autocomplétion. 
- Exports : devis/factures/commandes PDF + Excel, import CSV/Excel clients/contacts. 
- Emails : file d’attente, retries, boîte d’envoi, pièces jointes multiples. 
- Ressources : champs capacité/tonnage, tags multi, suggestions automatiques.
- Internationalisation : bundles i18n, formats date/monnaie corrects.
- Accessibilité : audit contrastes, navigation clavier complète, tooltips normalisés.
- Perf : index SQL sur dates/ressources/agency, pagination généralisée, gzip.
- Tests de charge (k6/JMH) avec budgets p95/p99.

## Could (améliorations à plus long terme)
- Feature flags dynamiques (basés env/DB). 
- Suggestion de ressources intelligentes (algorithme de matching capacités/indispos). 
- Export Excel multi-feuilles (planning + ressources). 
- Application mobile (client léger Android) branchée sur API REST. 
- Observabilité : traces distribuées (OpenTelemetry), dashboard Grafana.
- Packaging avancé : jlink/jpackage du client Swing.

---

# Planning par étape (suggéré)
1. Étape 1 : Planning pro (conflits bloquants, zoom, perfs, filtres). 
2. Étape 2 : Cycle Devis→Facture complet (entités, endpoints, PDF/email). 
3. Étape 3 : Sécurité JWT refresh + rôles + multi-agences strict. 
4. Étape 4 : Packaging Docker/compose + qualité code CI. 
5. Étape 5 : Emails avancés (file/retries/boîte d’envoi). 
6. Étape 6 : Internationalisation + Accessibilité. 
7. Étape 7 : Imports CSV/Excel + suggestions ressources. 
8. Étape 8 : Perf + observabilité. 
9. Étape 9 : Documentation finale & recette.

---

# Prochaine étape
- Livrer Étape 1 (planning pro) avec drag&drop robuste, zoom, conflits bloquants et perfs validées.
