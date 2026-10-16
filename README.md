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

Follows the diagram of the assignment: controller → service (interface + `Abis…Service`) → repository (interface + `DB…Repository`).

```
controller/   SandwichController, SandwichOrderController, GlobalExceptionHandler
service/      SandwichService, CourseService, PersonService, SandwichOrderService  (+ Abis… implementations)
repository/   SandwichRepository, CourseRepository, PersonRepository,
              SandwichOrderRepository, SandwichOrderDetailRepository             (+ DB… implementations, Spring JdbcClient)
model/        Sandwich, Course, Person, SandwichOrder, SandwichOrderDetail (+ enumeration/BreadType)
exception/    domain exceptions, mapped to HTTP 404 / 409
config/       AngularFrontendConfig (serves the frontend), OpenApiConfig
```

Tables (`schema.sql`): `sandwiches`, `abis_courses`, `abis_persons`, `sandwich_orders` (with `total_price`), `sandwich_order_details`.

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
* Orders are added one at a time (a lock in `AbisSandwichOrderService`), so students ordering at the same moment can't create duplicate persons/orders or exceed the limit. This protects a single running application, not several instances.
* No Spring Security (not required for this exercise).

## Database

Default: embedded H2 in Oracle mode (`application.properties`), no installation needed. `schema.sql` and `data.sql` run at startup.

Oracle: set the connection in `application-oracle.properties` and run with `--spring.profiles.active=oracle`.
Create the tables first by running `schema.sql` (valid on Oracle 12c+) and optionally `data.sql` in your schema.

The assignment asks for two schemas (one per API). This solution is one application with one datasource, so the
five tables live in one schema. To split them: sandwiches in one schema and the order tables in the other, grant
`REFERENCES` / `SELECT` on `sandwiches` to the order schema and prefix the table names in the SQL of the repositories.

## Tests (`./mvnw verify`)

* Repositories: `@JdbcTest` on the embedded database (`DBSandwichRepositoryTest`, `DBOrderRepositoriesTest`)
* Services: Mockito unit tests (`AbisSandwichServiceTest`, `AbisSandwichOrderServiceTest`, `FindOrCreateServicesTest`)
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
