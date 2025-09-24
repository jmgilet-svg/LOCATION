# Spécifications Fonctionnelles — Gestion Matériel
_MàJ: 2025-09-24T14:21:17Z_


## 1. Vision & périmètre
Application de gestion des opérations (levage, transport, manutention) : planifier, suivre et facturer des interventions, avec un référentiel de ressources/clients, multi-agences, modèles PDF & emails.

## 2. Personae & rôles
- **ADMIN** : tous droits.
- **SALES** : devis/BC/BL/factures, lecture planning.
- **CONFIGURATEUR** : lecture générale, écriture sur Ressources/Types/Paramètres.

## 3. Parcours clés
### 3.1 Planning (écran pivot)
- Affichage hebdo (7 colonnes) par ressources (lignes).
- Tuiles intervention : titre, client/chantier, adresse, horaire, pictos ressources, statut.
- Interactions : scroll, zoom (Ctrl+molette), drag & drop (heure/jour/ressource), resize (haut/bas), snap (30 min), tooltip live, hover ligne ressource.
- Filtres : statut, "À deviser"/"Déjà devisé", type de ressource, agence; densité compact/confort; recherche.
- Palette contextuelle : actions groupées (générer devis…), rappel des filtres, aide raccourcis.
- Sélection multiple.

### 3.2 Intervention (fiche)
- **Général** : titre, client, adresse, description, dates prévues, type intervention, note interne.
- **Intervention** : sélection ressources multiples (filtre type + recherche), note de fin, signature PNG (signé par, date).
- **Facturation** : pré-devis auto depuis ressources (PU de la ressource), lignes éditables, totaux.
- Actions : enregistrer, plein écran, générer devis, marquer terminé/annulé (selon rôle).

### 3.3 Ventes
- **Devis** : liste tri/recherche, édition inline, export PDF/CSV/Excel, envoi email (HTML + PJ PDF, CC/BCC, modèles par agence), conversion en facture (unitaire/multiple).
- **Factures** : idem devis, génération depuis devis (unitaire/multiple).

### 3.4 Référentiels
- **Ressources** : édition inline, type (combo), PU, indisponibilités (périodes), icône héritée du type.
- **Types de ressources** : CRUD + icône SVG colorée.
- **Types d’intervention** : CRUD, tri/ordre, duplication.
- **Clients** : CRUD + contacts (2–4), contact principal.
- **Utilisateurs/Comptes** : CRUD, rôles, reset MDP; changement MDP utilisateur.

### 3.5 Paramètres (par Agence)
- **Général** : TVA %, CGV (HTML), intervalle autosave (15/30/60 s).
- **Templates** : éditeur HTML → rendu PDF, partials, variables de fusion.
- **Assets** : logos/tampons (PNG/JPG/SVG) référencés dans templates.
- **Modèles d'emails** : HTML, variables de fusion, tracking pixel.

## 4. Statuts & pictos
- 🟢 Terminé, 🔵 Planifié/En cours, 🟡 Brouillon, 🔴 Annulé/Problème.
- Pictos ressources : SVG colorés par type (camion, grue, chariot, technicien…).

## 5. Règles métier (extraits)
- Génération de devis à 95% depuis l’intervention ; si déjà existant, ne pas régénérer.
- PU porté par **Ressource** (pas le type). Pré-devis = somme des ressources sélectionnées × quantités.
- Contact principal coché par défaut dans la fiche intervention.
- "À deviser" = intervention sans lien vers un devis.
- Drag & drop : seule la ressource principale (index 0) change ; autres ressources conservées.
- Verrouillages UI selon rôle (ex. éditions ventes interdites hors SALES/ADMIN).

## 6. Exports & Emails
- PDF : devis/factures détaillés (logo, tableau lignes, totaux, CGV).
- CSV/Excel depuis listes ventes.
- Emails HTML (par agence) avec variables, PJ PDF, CC/BCC, tracking d’ouverture.

## 7. Données démo
- ~60 ressources, ~60 interventions (2 semaines), ~20 clients × 2–4 contacts, ~15 utilisateurs (3 rôles).
