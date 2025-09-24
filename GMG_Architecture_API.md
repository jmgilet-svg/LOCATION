# Architecture & API
_MàJ: 2025-09-24T14:21:17Z_


## 1. Vue d'ensemble
- **Client** : Java 17+, Swing. Service Locator (Mock/API). PDF preview partielle (PDFBox).
- **Serveur** : Spring Boot. Contrats **v1** conservés, **v2** pour extensions. OpenAPI documenté.
- **Agences** : header `X-Agency-Id` obligatoire côté client ; scoping côté serveur.
- **Sécurité** : rôles en base. Session/expiration côté client ; extension JWT possible côté serveur.

## 2. Clients HTTP (ServiceLocator)
- `interventions()` : list(filter), get(id), save(dto), delete(id)
- `resources()` : listAll(), get(id), save(dto), delete(id)
- `clients()` : listAll(), get(id), save(dto), delete(id)
- `quotes()`/`quotesV2()` : list(...), get, save, delete, **fromIntervention(id)**
- `invoices()` : list, get, save, delete
- `settings()` : get/put par agence (TVA/CGV/templates)
> Tous envoient `X-Agency-Id`.

## 3. API Serveur
### 3.1 V1 (legacy, inchangé)
- Interventions, Ressources, Clients : CRUD avec schémas historiques
  - Interventions: `dateHeureDebut`, `dateHeureFin`, `clientName`, etc.
  - Ventes (devis/factures) : `number`, `lines`, `totalHt`, `totalTtc`, …

### 3.2 V2 (nouvelles capacités)
- **POST** `/api/v2/quotes/from-intervention`
  - In: `{ "interventionId": "..." }` (+ `X-Agency-Id`)
  - Out: `{ "id": "...", "reference": "...", "interventionId": "..." }`
  - Effets : crée un devis, marque l’intervention comme `quoteGenerated=true` (liaison).
- **GET/PUT** `/api/v2/settings` (scopé agence)
  - `{ "tvaPercent": 20.0, "cgvHtml": "<p>...</p>", "emailTemplates": {...}, "partials": {...}, "assets": [...] }`
- **POST** `/api/v2/email/send`
  - `{ "to":[], "cc":[], "bcc":[], "subject":"...", "html":"...", "attachments":[...] }` + pixel tracking auto.
- **Assets/Templates** : upload/list/delete (clé d’asset, partials nommés).

## 4. Planning (client)
- Grille unique (7 colonnes jours × N lignes ressources).
- Listeners directs sur la grille : scroll, zoom, hover, drag, resize.
- Snap 30 min, tooltip live, surlignage ligne ressource.
- `save()` puis `reload()` au drop ; réaffectation ressource principale.

## 5. PDF & Templates
- Templates HTML (QUOTE/INVOICE/EMAIL/PARTIAL).
- Partials `{{>partial:name}}`, assets `{{asset:key}}`.
- Fallback `{{>partial:cgv}}` -> CGV Agence si absent.
- Rendu PDF côté serveur (wkhtmltopdf-like / flying-saucer / iText selon stack choisie).

## 6. Emails
- Rendu HTML depuis template agence + variables (client, devis, facture).
- Jointures PDF. CC/BCC multiples.
- Tracking pixel (image 1×1 servie par backend avec id d’email).

## 7. Sécurité & Rôles (client+serveur)
- Masquage fins des menus + lecture seule forcée sur dialogs ventes.
- Expiration de session + Déconnexion.
- Sur serveur : mapping rôle→autorisation endpoints (Spring Security). JWT optionnel.

## 8. Build & Qualité
- Maven multi-modules (client/server).
- Analyse “unused” (IDE + script ripgrep/jdeps). 
- Conventions JSON (`totalTtc` camelCase, fix via `@JsonProperty`).

## 9. Déploiement
- Serveur Spring Boot fat-jar ou Docker.
- Client Swing distribué en jpackage (optionnel).
