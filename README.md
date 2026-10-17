# Sandwich solution

Reference solution of the Abis sandwich ordering exercise (Spring Boot 4, Java 17+). **Instructors only.**

## Run it

```bash
./mvnw spring-boot:run        # Windows: mvnw.cmd spring-boot:run
./mvnw verify                 # build + all tests
```

* Application with the Angular frontend: <http://localhost:8080>
* API documentation (Swagger UI): <http://localhost:8080/swagger-ui.html>, OpenAPI JSON: <http://localhost:8080/v3/api-docs>
* Needs only a JDK (17 or newer). The database is an embedded H2 in Oracle mode, filled with the menu at startup.

## Architecture

Follows the diagram of the assignment: controller → service (interface + `Abis…Service`) → repository. The repositories are Spring Data JPA interfaces: Spring generates the implementation, so there are no `DB…Repository` classes.

```
controller/   SandwichController, SandwichOrderController, GlobalExceptionHandler
dto/          request and response records (what goes over HTTP) with their validation, and the mapping to/from entities
service/      SandwichService, CourseService, PersonService, SandwichOrderService  (+ Abis… implementations),
              OrderPrinter (writes the .txt file)
repository/   SandwichRepository, CourseRepository, PersonRepository,
              SandwichOrderRepository, SandwichOrderDetailRepository             (Spring Data JpaRepository interfaces)
model/        JPA entities: Sandwich, Course, Person, SandwichOrder, SandwichOrderDetail (+ enumeration/BreadType)
exception/    domain exceptions, mapped to HTTP 404 / 409
config/       AngularFrontendConfig (serves the frontend), OpenApiConfig, TransactionConfig (SERIALIZABLE), RetryConfig
```

Controllers only talk DTOs: the entities of `model/` never go over HTTP. A request DTO validates the input (Bean Validation, clear messages) and converts itself to an entity (`toEntity()`); a response DTO is built from an entity (`from(entity)`). That way the database model can change without breaking the API, a client can't set fields it shouldn't (ids, prices, totals) and can't see fields it shouldn't.

Tables (created by Hibernate from the entities): `sandwiches`, `abis_courses`, `abis_persons`, `sandwich_orders` (with `total_price`), `sandwich_order_details`.

## The API

| Request | What it does |
|---|---|
| `GET /api/sandwiches[?category=&name=]` | full list, by category (exact), by name (part of it, ignoring case) |
| `GET /api/sandwiches/{id}` | one sandwich (404 if unknown) |
| `POST /api/sandwiches` | add a sandwich (201 + `Location`; 400 if invalid; 409 if the name exists) |
| `PUT /api/sandwiches/{id}/price` | body `{"basePrice": 3.5}` |
| `DELETE /api/sandwiches/{id}` | remove (204; 409 if it is part of an order) |
| `POST /api/orders/details` | order a sandwich (see rules below) |
| `POST /api/orders/print` | body `{orderDate, course}`: writes `orders/sandwich-order_<date>_<course>.txt` and returns the text |
| `POST /api/orders/price` | body `{orderDate, course}`: calculates the total, **saves it in `sandwich_orders.total_price`**, returns it |

Errors are JSON problem details (`{"status": 409, "detail": "..."}`), the frontend shows `detail`.

Rules and choices:

* There is **one order per course per day**: the first sandwich creates the order. Course and person are created when they don't exist yet (matched ignoring case).
* A person can order **at most 2 sandwiches per order** (amount 1 or 2 per request, 2 in total): HTTP 409 otherwise.
* The sandwich data sent by the client is ignored except for the id; the price always comes from the database.
* Total price = sum of `basePrice × amount` (calculated with `BigDecimal`, 2 decimals). Bread type and vegetables don't change the price.
* **Students ordering at the same moment** (see `AbisSandwichOrderService.addSandwichOrderDetail`): adding a sandwich runs in a
  `SERIALIZABLE` transaction (`TransactionConfig`), which makes the database behave as if simultaneous orders ran one after the
  other. When two orders collide, the database refuses one of them (a concurrency failure, or a unique constraint because the
  person/course/order was just created by the other order) and the service **retries** it (Spring's `RetryTemplate`, configured in `RetryConfig`: up to 10 attempts, a random pause that doubles each time).
  The second time it sees what the other order saved. Business errors (max 2, unknown sandwich) are never retried.
  Without this, two simultaneous first orders would both try to create the same person/course/order (a 500), and one person could
  exceed the limit of 2.
* Every order also updates the **total price** saved on the order. That is a read-modify-write on one row, so simultaneous orders
  for the same order really conflict. This matters: `SERIALIZABLE` only detects conflicts between transactions that touch the
  same rows, so without that shared row two orders of the same person could both insert a sandwich and pass the "max 2" check
  (this was seen in the tests on H2; Oracle's `SERIALIZABLE` has the same limit).
* The price of this: all orders of one course queue up on one row. For a class this is fine (the tests place 15 simultaneous orders);
  after 10 failed attempts the client gets a 409 "server is busy, try again".
* No Spring Security (not required for this exercise).

## Database

Default: embedded H2 in Oracle mode (`application.properties`), no installation needed. Hibernate creates the tables (`ddl-auto=create`), then `data.sql` fills the menu.

Oracle: set the connection in `application-oracle.properties` and run with `--spring.profiles.active=oracle`.
Hibernate then creates the missing tables itself (`ddl-auto=update`, needs Oracle 12c+ for identity columns). `data.sql` (the menu) is not run automatically on Oracle: run it once by hand in your schema.

The assignment asks for two schemas (one per API). This solution is one application with one datasource, so the
five tables live in one schema. To split them: sandwiches in one schema and the order tables in the other, grant
`REFERENCES` / `SELECT` on `sandwiches` to the order schema and put `schema = "..."` in the `@Table` annotations of the entities.

## Tests (`./mvnw verify`)

* Repositories: `@DataJpaTest` on the embedded database (`SandwichRepositoryTest`, `OrderRepositoriesTest`)
* Services: Mockito unit tests with `@InjectMocks` (`AbisSandwichServiceTest`, `AbisSandwichOrderServiceTest`, `FindOrCreateServicesTest`, `OrderPrinterTest`)
* Controllers: `@WebMvcTest` (`SandwichControllerTest`, `SandwichOrderControllerTest`)
* Whole application over HTTP incl. parallel orders, print file and API docs: `SandwichApiIntegrationTest`

## Faster restarts (optional: Spring Boot DevTools)

Off by default. IntelliJ: *Maven* tool window → *Profiles* → tick **devtools** → reload Maven. Command line: `-Pdevtools`.
Then rebuild with Ctrl+F9 and the app restarts by itself.

## The frontend

Angular source in `frontend/`, compiled output committed in `src/main/resources/static` (don't edit). The frontend calls
the endpoints above (relative URLs, same origin); they are listed in `frontend/src/app/api.service.ts`.

```bash
cd frontend
npm install                  # once
npm start                    # dev server on http://localhost:4200, proxies /api to http://localhost:8080
npm run publish-to-spring    # production build -> copied into ../src/main/resources/static (commit the result)
```
