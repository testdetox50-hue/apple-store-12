# apple-store-12 reviewer notes

## Architecture

ShopFlow is a dependency-free Java 17 inventory and order-management system split into `common`, `backend`, and Swing `frontend` modules (`shopflow/README.md`). The backend wires repositories, services, controllers, persistence, and the JDK HTTP server in `backend/src/com/shopflow/backend/Main.java`. Data is held in thread-safe in-memory repositories and persisted as one JSON array per entity; report endpoints additionally invoke the single Python report script.

## Conventions

- Controllers are thin HTTP adapters: register routes, parse `RequestContext`, delegate business logic to services, and shape responses. See `CategoryController`, `ProductController`, and `OrderController`.
- Routes use explicit REST-style method/path registration, with path variables written as `{id}` and query values read through `ctx.query(...)`; each controller owns its `/api/<resource>` routes.
- Successful creates set HTTP 201, and deletes set 204 with a `null` body, consistently in `CategoryController`, `CustomerController`, `ProductController`, and `OrderController`.
- Collection responses generally use `JsonResponse.page(items, total, offset, limit)`, even for currently unpaginated lists (`CategoryController`, `CustomerController`, `OrderController`). Product listing clamps offset and limit and caps limit at 500 (`ProductController.list`).
- JSON conversion belongs to models or shared helpers: controllers use methods such as `Product.toJson()`, `Order.toJson()`, `User.toPublicJson()`, and `Json.toJsonList(...)` rather than manually serializing fields.
- Business rules belong in services/repositories, not controllers. For example, order creation and status transitions are delegated to `OrderService`, while repository classes provide domain lookups such as `ProductRepository.findLowStock()` and `OrderRepository.referencesProduct()`.
- Repository classes extend `InMemoryRepository<T>`, use short ID prefixes (`"prd"`, `"cus"`, `"ord"`), and expose full JSON file names through `getName()` (`ProductRepository`, `CustomerRepository`, `OrderRepository`).
- Entity ordering is deliberate: base repositories sort by ID, while `OrderRepository` overrides this to return newest orders first.
- Persistence files must remain plain JSON arrays of entity objects because both `FileStore` and the Python report script consume them (`FileStore.java`).
- Authentication is enforced centrally by `Router`/`RequestContext`; only explicitly registered `open(...)` routes, such as login and `/api/health`, are unauthenticated (`AuthController`, `ReportController`).
- Configuration is read through `ServerConfig` from system properties/environment variables; avoid hardcoding deployment paths or ports (`shopflow/README.md`, `Main.java`).

## Intentional non-standard choices

- The project deliberately has no third-party dependencies and uses `com.sun.net.httpserver.HttpServer` plus a custom JSON implementation (`shopflow/README.md`).
- Report generation intentionally launches Python for every report request; Python is not a replacement backend and failures should be isolated to report/dashboard functionality (`ReportController.java`, `shopflow/README.md`).
- Repository writes persist synchronously on every mutation. `InMemoryRepository` rolls back the in-memory change if the atomic `FileStore` write fails; this behavior is intentional consistency protection.

## Watch out for

- Do not bypass repository `save`/`delete`; doing so skips persistence and rollback behavior (`InMemoryRepository.java`).
- Preserve atomic temp-file replacement and JSON-array format in `FileStore`; changing either can corrupt data or break Python reports.
- Avoid exposing password fields: user responses should use `User.toPublicJson()`, as in `AuthController`.
- Preserve order stock and lifecycle rules in services; seeded orders explicitly reserve stock (`SeedData.reserveStock`), and open-order references affect deletion logic.
- When adding routes, ensure protected endpoints are not registered with `router.open(...)`, and place specific routes such as `/api/orders/statuses` before parameterized order routes (`OrderController.register`).