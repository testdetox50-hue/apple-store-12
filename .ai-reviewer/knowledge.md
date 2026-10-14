# shopflow reviewer notes

## Architecture

ShopFlow is a dependency-free Java 17 inventory and order system split into `common`, `backend`, and `frontend` modules (`shopflow/README.md`). The backend wires repositories, services, controllers, persistence, and the JDK HTTP server in `backend/src/com/shopflow/backend/Main.java`. Domain data lives in thread-safe in-memory repositories and is persisted as one JSON array per entity; reporting is the deliberate exception, invoking the Python script through `ReportService`.

## Conventions

- Controllers are thin HTTP adapters: register routes, parse request data, set status codes, and delegate business logic to services. Examples include `CategoryController.java`, `ProductController.java`, and `OrderController.java`.
- Every controller exposes a `register(Router router)` method and uses path templates such as `/api/products/{id}`. Keep route registration order in mind; `/api/orders/statuses` is registered before `/api/orders/{id}` in `OrderController.java`.
- Successful creation returns HTTP `201`; deletes return `204` with a `null` body. This pattern is consistent across `CategoryController`, `CustomerController`, `ProductController`, `OrderController`, and `AuthController`.
- Collection responses use `JsonResponse.page(items, total, offset, limit)`. Most endpoints return the complete collection with offset `0`; product listing additionally clamps `offset` and `limit` (`ProductController.java`).
- JSON conversion belongs at the controller/model boundary: use model methods such as `Order::toJson`, `User::toPublicJson`, and enrichment methods such as `ProductService.enrich`, rather than exposing persistence objects directly.
- Repositories extend `InMemoryRepository<T>`, provide a short ID prefix and a stable persistence name, and implement `fromJson` (`ProductRepository.java`, `CustomerRepository.java`). Entity searches and uniqueness checks are repository helpers.
- `InMemoryRepository` sorts normal entity lists by ID; `OrderRepository` intentionally overrides this to return newest orders first. Preserve that ordering when adding order queries.
- Mutations must go through repository `save`/`delete`. The attached `FileStore` listener persists every change, and `InMemoryRepository` rolls back the in-memory mutation if persistence fails (`InMemoryRepository.java`, `FileStore.java`).
- Persisted files must remain plain JSON arrays because both Java loading and the Python report script consume them (`FileStore.java`, `shopflow/README.md`).
- Authentication is enforced centrally by the router except for explicitly open endpoints. Controllers still perform operation-specific checks, such as admin-only order deletion in `OrderController.java`.

## Intentional non-standard choices

- Reports intentionally depend on an external Python subprocess for every report request; this is not a Java library integration bug (`ReportController.java`, `shopflow/README.md`). Python failure should affect report views, not unrelated API screens.
- “Seed” behavior is conditional and asymmetric: catalog data is inserted only when categories and products are both empty, while bootstrap admin creation is refused without an explicitly configured password (`SeedData.java`).
- All writes use a temporary file followed by an atomic move, and repository changes are rolled back when saving fails. Do not simplify this into best-effort persistence (`FileStore.java`, `InMemoryRepository.java`).

## Watch out for

- Do not add direct file writes, alternate JSON shapes, or repository mutations that bypass the change listener; this can desynchronize memory, disk, and Python reports.
- Preserve stock/order invariants in service-layer changes: seeded orders reserve stock (`SeedData.java`), and open-order references affect deletion behavior (`OrderRepository.java`).
- Avoid returning password hashes or internal user fields; authentication endpoints deliberately use `User.toPublicJson()` (`AuthController.java`).
- Validate numeric and required request fields before service calls, following `ProductController.adjustStock` and `OrderController.changeStatus`.
- Keep case-insensitive uniqueness/search semantics using `Locale.ROOT`, as implemented by `ProductRepository` and `CustomerRepository`.