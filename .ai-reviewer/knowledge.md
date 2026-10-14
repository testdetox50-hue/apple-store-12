# apple-store-12 reviewer notes

## Architecture

ShopFlow is a dependency-free Java 17 inventory/order system split into `common`, `backend`, and Swing `frontend` modules (`shopflow/README.md`). The backend wires repositories, services, controllers, routing, persistence, and the JDK HTTP server in `backend/src/com/shopflow/backend/Main.java`. Repositories hold in-memory entities backed by per-entity JSON files; report endpoints additionally invoke the sole Python service script.

## Conventions

- Controllers are thin HTTP adapters: register routes, read `RequestContext`, delegate business logic to services, and shape JSON responses. Follow examples in `backend/src/com/shopflow/backend/controller/ProductController.java` and `OrderController.java`.
- Routes use explicit HTTP verbs and `/api/<entity>` paths, with `{id}` path parameters. Each controller owns a `register(Router)` method; new endpoints should follow this pattern.
- Collection responses use `JsonResponse.page(items, total, offset, limit)`, even where the current implementation returns the complete collection. See `CategoryController`, `CustomerController`, and `OrderController`.
- Successful creates set HTTP 201; deletes set 204 and return `null`. See `CategoryController.java` and `AuthController.java`.
- JSON conversion belongs at the boundary or model layer: use `toJson()`/`toPublicJson()` and helpers such as `Json.toJsonList`, rather than exposing entities indiscriminately. Authentication responses deliberately use `User.toPublicJson()`.
- Repositories extend `InMemoryRepository<T>`, provide a JSON factory, and expose domain-specific lookups. Persistence names are stable file names such as `categories.json`, `products.json`, and `orders.json`; see `CategoryRepository.java` and `FileStore.java`.
- Search is case-insensitive and uses `Locale.ROOT`; uniqueness helpers trim or compare case-insensitively where appropriate (`ProductRepository`, `CustomerRepository`, `CategoryRepository`).
- Services, rather than controllers, should enforce domain rules and coordinate repositories. `Main.java` demonstrates the dependency order and composition.
- All endpoints are authenticated by default; only explicitly registered `open` routes such as login and `/api/health` bypass authentication (`AuthController.java`, `ReportController.java`).

## Intentional non-standard choices

- The backend intentionally has no framework or third-party dependencies and uses `com.sun.net.httpserver.HttpServer` (`Main.java`).
- Data is intentionally stored as pretty-printed JSON arrays, not a database. `FileStore.save()` writes a temporary file and atomically moves it into place because the Python report script reads the same files (`FileStore.java`, `shopflow/README.md`).
- Reports intentionally spawn the Python script for each report request; Python failure affects only report/dashboard functionality (`ReportController.java`, `shopflow/README.md`).
- `InMemoryRepository.save/delete` rolls back its in-memory mutation when persistence fails, preserving memory/disk consistency (`InMemoryRepository.java`).

## Watch out for

- Do not bypass repositories or mutate entities without saving them; the change listener persists only repository writes (`InMemoryRepository.java`).
- Preserve JSON field shapes and entity filenames: they are shared with `report_service.py` and loaded through each repository’s `fromJson`.
- Do not expose passwords or password hashes; authentication user responses must remain public projections (`AuthController.java`).
- Preserve order-specific behavior: orders are newest-first (`OrderRepository.java`), open-order references constrain product/customer operations, and seeded orders reserve stock (`SeedData.java`).
- Maintain route registration order where static paths such as `/api/orders/statuses` coexist with `/{id}` routes (`OrderController.java`).
- Keep pagination bounds and validation when changing product listing or mutation endpoints; `ProductController` caps `limit` at 500 and rejects missing stock `delta`.