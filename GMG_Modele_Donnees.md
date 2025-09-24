# Modèle de Données (ERD logique)
_MàJ: 2025-09-24T14:21:17Z_


Notation: `Entity { field: type [constraints] }` ; FK → `->`

## Agence & Paramètres
- **Agency** { id: string [PK], name: string }
- **Settings** { agencyId -> Agency.id, tvaPercent: decimal(5,2), cgvHtml: text }
- **Asset** { id: string [PK], agencyId -> Agency.id, key: string [unique], mime: string, data: blob }
- **Template** { id: string [PK], agencyId -> Agency.id, kind: enum(QUOTE,INVOICE,EMAIL,PARTIAL), key: string, html: text }

## Utilisateurs & Sécurité
- **User** { id: string [PK], login: string [unique], passwordHash: string, role: enum(ADMIN,SALES,CONFIGURATEUR), agencyId -> Agency.id, active: bool }
- **Session** (si côté serveur) { id, userId -> User.id, expiresAt }

## Référentiels Ressource
- **ResourceType** { id: string [PK], name: string, iconKey: string }
- **Resource** { id: string [PK], name: string, typeId -> ResourceType.id, unitPrice: decimal(10,2), agencyId -> Agency.id }
- **ResourceUnavailability** { id: string [PK], resourceId -> Resource.id, start: datetime, end: datetime }

## Clients & Contacts
- **Client** { id: string [PK], name: string, address: string?, agencyId -> Agency.id }
- **Contact** { id: string [PK], clientId -> Client.id, firstName: string, lastName: string, email: string?, phone: string?, isPrimary: bool }

## Types d’intervention
- **InterventionType** { id: string [PK], name: string, orderIndex: int }

## Interventions
- **Intervention** {
    id: string [PK],
    title: string,
    clientId -> Client.id,
    typeId -> InterventionType.id?,
    address: string?,
    plannedStart: datetime,
    plannedEnd: datetime,
    actualStart: datetime?,
    actualEnd: datetime?,
    description: text?,
    internalNote: text?,
    endNote: text?,
    status: enum(DRAFT,PLANNED,IN_PROGRESS,DONE,CANCELLED),
    quoteGenerated: bool,
    signedBy: string?,
    signedAt: datetime?,
    signaturePng: blob?,
    agencyId -> Agency.id
}
- **InterventionResource** { interventionId -> Intervention.id, resourceId -> Resource.id, quantity: decimal(10,2) default 1, PRIMARY KEY(interventionId, resourceId) }
- **InterventionContact** { interventionId -> Intervention.id, contactId -> Contact.id, PRIMARY KEY(interventionId, contactId) }

## Devis & Factures
- **Quote** { id: string [PK], number: string [unique per agency], clientId -> Client.id, status: enum(DRAFT,SENT,ACCEPTED,REJECTED), totalHt: decimal(12,2), totalTtc: decimal(12,2), createdAt: datetime, agencyId -> Agency.id, sourceInterventionId -> Intervention.id? }
- **QuoteLine** { id: string [PK], quoteId -> Quote.id, designation: string, qty: decimal(10,2), unitPrice: decimal(10,2), totalHt: decimal(12,2) }
- **Invoice** { id: string [PK], number: string [unique per agency], clientId -> Client.id, status: enum(DRAFT,SENT,PAID,CANCELLED), totalHt: decimal(12,2), totalTtc: decimal(12,2), createdAt: datetime, agencyId -> Agency.id, sourceQuoteId -> Quote.id? }
- **InvoiceLine** { id: string [PK], invoiceId -> Invoice.id, designation: string, qty: decimal(10,2), unitPrice: decimal(10,2), totalHt: decimal(12,2) }

## Emails (tracking)
- **OutgoingEmail** { id: string [PK], agencyId -> Agency.id, to: json, cc: json?, bcc: json?, subject: string, html: text, createdAt: datetime }
- **EmailOpenEvent** { id: string [PK], emailId -> OutgoingEmail.id, openedAt: datetime, userAgent: string?, ip: string? }

### Indexation & contraintes clés
- Index par agence sur chaque entité métier (`agencyId`).
- Unicité {Agency, Template.key, Template.kind} ; {Agency, Asset.key} ; {Agency, Quote.number} ; {Agency, Invoice.number}.
- Intégrité: suppression d’une ressource interdit si liée à une intervention (ou cascade contrôlée).

## DTOs principaux (extraits)
### InterventionDTO
{
  "id":"...", "title":"...", "clientId":"...", "typeId":"...",
  "address":"...", "plannedStart":"2025-09-18T08:00:00Z", "plannedEnd":"2025-09-18T14:00:00Z",
  "resources":[{"id":"r1","name":"Grue LTM 965"}, { "id":"r2","name":"Camion 28T"}],
  "contacts":[{"id":"c1"},{"id":"c2"}],
  "status":"PLANNED", "quoteGenerated":false
}

### QuoteFromInterventionRequest (V2)
{ "interventionId":"..." }

### Settings (Agence)
{ "tvaPercent":20.0, "cgvHtml":"<p>...</p>" }

## Flux standards
1. **Planifier** Intervention → ajouter ressources → pré-devis auto.
2. **Générer Devis** (V2) → marquer `quoteGenerated=true` + lien sourceInterventionId.
3. **Envoyer** Email (HTML + PDF).
4. **Convertir** Devis → Facture (unitaire/multiple).
