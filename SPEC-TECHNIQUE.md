# SPEC-TECHNIQUE — LOCATION

## Architecture
Monorepo **Maven multi-modules** :
```
LOCATION/
  pom.xml (parent)
  server/ (Spring Boot)
  client/ (Swing)
```

### Server (Spring Boot)
- Java 17, `spring-boot-starter-web`, `spring-boot-starter-security`, `spring-boot-starter-validation`, `spring-boot-starter-data-jpa`
- DB : H2 (dev), Postgres (prod)
- Migrations : Flyway
- Auth : JWT HS256 (JJWT)
- SSE : `/api/system/ping` via `SseEmitter`
- Sécurité : `/api/**` protégé, `/auth/**` & `/api/system/ping` publics (Diff 1)

### Client (Swing)
- Java 17, FlatLaf
- **Injection** via `DataSourceProvider` :
  - `MockDataSource` (in-memory, parité visible)
  - `RestDataSource` (HTTP + JWT)
- Sélecteur de source au démarrage + mémorisation (`~/.location/app.properties`)
- Argument CLI `--datasource=mock|rest` **prioritaire**

## Diagramme (sélection de source)
```
          +---------------------------+
          |   Args --datasource=?     |
          +-------------+-------------+
                        |
                 (si présent)
                        v
                 +-------------+
                 |  Use arg    |
                 +-------------+
                        |
                        v
  (sinon)     +--------------------------+
              | read ~/.location/app...  |
              +-------------+------------+
                            |
                     (si présent)
                            v
                        +-------+
                        | Use   |
                        +-------+
                            |
                            v
                 +------------------------+
                 | Show selection dialog  |
                 +------------------------+
```

## Endpoints (Diff 1)
- `POST /auth/login` : corps `{ "username": "...", "password": "..." }` (non vides) → `{"token":"...","expiresAt":...}`
- `GET /api/system/ping` : `text/event-stream`, event `ping:{timestamp}`

> **Note** : En Diff 1, l’auth valide tout couple non vide (objectif bootstrap). En Diff 2, branchement sur comptes/roles (DB) + password hash.

## Sécurité
- JWT signé HS256 avec secret env `JWT_SECRET` (défaut dev).
- Header : `Authorization: Bearer <jwt>`.
- Filtre JWT personnalisée : parse/valide, propage l’auth dans le `SecurityContext`.
- CORS dev permissif (origines `*`) — restreindre en prod.

## Perf & Packaging
- JVM 17, `server.port=8080` par défaut.
- Index & pagination : à implémenter en Diff 2 (modèle).
- Packaging : `mvn package` → `location-server.jar` & `location-client.jar`.

## Stratégies d’environnement
- `application.yml`, `application-dev.yml`, `application-prod.yml`.
- Secrets **uniquement** via **variables d’environnement**.
- Postgres prod : `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`.

## Cycle de vie client
1. Résolution de la source (CLI > prefs > dialog).
2. Instanciation du `DataSourceProvider` choisi.
3. Affichage shell + badge état.
4. En mode REST, login initial (décision Diff 1 : auto-login `demo/demo` à la première requête).
5. Les écrans consomment **exclusivement** `DataSourceProvider`.

## Interfaces (client)
```java
public interface DataSourceProvider extends AutoCloseable {
  String getLabel(); // "MOCK" | "REST"
  void resetDemoData(); // no-op en REST
  List<Agency> listAgencies();
  List<Client> listClients();
  // évolutif en Diff 2: interventions, ressources, etc.
}
```

## DTOs partagés (client)
Objets simples `Agency`, `Client`, etc. **communs** aux implémentations Mock/REST pour garantir la parité perçue. Mapping REST en Diff 2.

## Erreurs
- REST : gestion de `401/403/5xx`, retries basiques (non bloquant).
- Mock : exceptions runtime avec message utilisateur.

## Tests
- **Server** : `@WebMvcTest` pour `/auth/login`, test SSE via `MockMvc`.
- **Client** : test résolution source (CLI > prefs > dialog).

## CI
GitHub Actions (`.github/workflows/ci.yml`) :
- Java 17 setup
- `mvn -B -ntp verify`
- Cache Maven

## Qualité
- Spotless (Google Java Format) activé sur le parent (optionnellement désactivable via `-Dspotless.skip=true`).

## Évolutions prévues (Diff 2/3)
- `/api/v1/**` versionné, DTOs stables, validations Bean Validation.
- Règles de chevauchement en service.
- HTML→PDF (OpenPDF/Flying Saucer) + abstraction emailing.
- UI planning (drag/drop/resize/hover), éditeurs intégrés, exports.
