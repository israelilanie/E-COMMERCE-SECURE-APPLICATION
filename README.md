# E-Commerce Secure Application

A secure, JWT-protected e-commerce REST API built with Spring Boot. The application demonstrates user registration and authentication, product browsing, cart management, and order history while enforcing ownership checks so users can only access and modify their own account, cart, and orders.

## What I Found During Review

This project is an educational but very practical Spring Boot API with a clean domain model and a security-first direction. The most exciting part is that it already includes the core flow expected from a secure commerce backend:

- Users can sign up with password validation and BCrypt password hashing.
- Users authenticate through `/login` and receive a bearer JWT.
- Protected endpoints require authentication.
- Profile, cart, and order operations verify that the authenticated token subject matches the requested username.
- Items are seeded automatically into an in-memory H2 database.
- Integration tests exercise the most important authentication and authorization flows.
- JaCoCo is configured to generate test coverage reports.

The codebase is compact and easy to study, making it a strong foundation for learning secure API design or for expanding into a more production-ready storefront service.

## Table of Contents

- [Tech Stack](#tech-stack)
- [Architecture Overview](#architecture-overview)
- [Domain Model](#domain-model)
- [Security Model](#security-model)
- [API Reference](#api-reference)
- [Getting Started](#getting-started)
- [Example API Flow](#example-api-flow)
- [Testing](#testing)
- [Configuration](#configuration)
- [Project Structure](#project-structure)
- [Improvement Roadmap](#improvement-roadmap)
- [Production Readiness Checklist](#production-readiness-checklist)

## Tech Stack

| Area | Technology |
| --- | --- |
| Language | Java 21 |
| Framework | Spring Boot 3.3.5 |
| Web | Spring Web MVC |
| Security | Spring Security, BCrypt, JWT |
| JWT Library | Auth0 Java JWT |
| Persistence | Spring Data JPA / Hibernate |
| Database | H2 in-memory database |
| Build Tool | Maven Wrapper |
| Testing | JUnit 5, Spring Boot Test, MockMvc, Mockito |
| Coverage | JaCoCo |

## Architecture Overview

The application follows a straightforward layered Spring Boot structure:

1. **Controllers** expose REST endpoints for users, items, carts, and orders.
2. **Request models** represent incoming JSON payloads.
3. **Persistence entities** model users, carts, items, and orders.
4. **Repositories** provide Spring Data JPA database access.
5. **Security filters** handle JSON login, JWT generation, and JWT verification.
6. **Tests** validate authentication, authorization, entity behavior, and core e-commerce flows.

## Domain Model

### User

Represents an application customer.

- Has a unique username.
- Stores a BCrypt-hashed password.
- Owns one cart.
- Password is write-only in JSON responses.

### Item

Represents a product in the catalog.

- Includes an ID, name, price, and description.
- Seed data currently includes:
  - `Round Widget` at `2.99`
  - `Square Widget` at `1.99`

### Cart

Represents a user's active shopping cart.

- Belongs to one user.
- Contains many items.
- Tracks a total price.
- Supports adding and removing item quantities.

### UserOrder

Represents a submitted order.

- Created from the user's current cart.
- Copies cart items and total into an order record.
- Linked back to the user for order history lookups.

## Security Model

The project uses stateless JWT authentication:

1. A user signs up through `POST /api/user/create`.
2. The user logs in through `POST /login` with JSON credentials.
3. A successful login returns an `Authorization` response header containing a `Bearer <token>` value.
4. Protected requests must include that header.
5. The JWT authorization filter validates the token and places the username into Spring Security's context.
6. User-specific endpoints compare the authenticated username against the requested username and return `403 Forbidden` when they do not match.

Public endpoints:

- `POST /api/user/create`
- `POST /login`

All other endpoints require authentication.

> Important: the current JWT secret is hard-coded for demo purposes. Move it to environment-based configuration before production use.

## API Reference

### Authentication

#### Create User

```http
POST /api/user/create
Content-Type: application/json
```

Request body:

```json
{
  "username": "alice",
  "password": "password1",
  "confirmPassword": "password1"
}
```

Responses:

- `200 OK` when the user is created.
- `400 Bad Request` when username/password validation fails.
- `409 Conflict` when the username already exists.

#### Login

```http
POST /login
Content-Type: application/json
```

Request body:

```json
{
  "username": "alice",
  "password": "password1"
}
```

Successful response:

```http
Authorization: Bearer <jwt>
```

### Users

#### Get User by Username

```http
GET /api/user/{username}
Authorization: Bearer <jwt>
```

Returns the requested user only when `{username}` matches the authenticated JWT subject.

#### Get User by ID

```http
GET /api/user/id/{id}
Authorization: Bearer <jwt>
```

Returns the requested user only when the user ID belongs to the authenticated JWT subject.

### Items

#### List Items

```http
GET /api/item
Authorization: Bearer <jwt>
```

#### Get Item by ID

```http
GET /api/item/{id}
Authorization: Bearer <jwt>
```

#### Search Items by Name

```http
GET /api/item/name/{name}
Authorization: Bearer <jwt>
```

### Cart

#### Add Items to Cart

```http
POST /api/cart/addToCart
Authorization: Bearer <jwt>
Content-Type: application/json
```

Request body:

```json
{
  "username": "alice",
  "itemId": 1,
  "quantity": 2
}
```

#### Remove Items from Cart

```http
POST /api/cart/removeFromCart
Authorization: Bearer <jwt>
Content-Type: application/json
```

Request body:

```json
{
  "username": "alice",
  "itemId": 1,
  "quantity": 1
}
```

### Orders

#### Submit Order

```http
POST /api/order/submit/{username}
Authorization: Bearer <jwt>
```

Creates an order from the user's current cart.

#### Get Order History

```http
GET /api/order/history/{username}
Authorization: Bearer <jwt>
```

Returns orders belonging to the authenticated user.

## Getting Started

### Prerequisites

- Java 21+
- Maven is optional because the repository includes `mvnw` and `mvnw.cmd`

### Run the Application

```bash
./mvnw spring-boot:run
```

The API starts on:

```text
http://localhost:8080
```

### H2 Console

The H2 console is enabled for local development:

```text
http://localhost:8080/h2
```

Default connection values:

| Setting | Value |
| --- | --- |
| JDBC URL | `jdbc:h2:mem:bootapp;NON_KEYWORDS=user` |
| Username | `sa` |
| Password | empty |

## Example API Flow

Create a user:

```bash
curl -i -X POST http://localhost:8080/api/user/create \
  -H 'Content-Type: application/json' \
  -d '{"username":"alice","password":"password1","confirmPassword":"password1"}'
```

Log in and copy the returned `Authorization` header:

```bash
curl -i -X POST http://localhost:8080/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"alice","password":"password1"}'
```

List products:

```bash
curl -H "Authorization: Bearer <jwt>" http://localhost:8080/api/item
```

Add two round widgets to the cart:

```bash
curl -X POST http://localhost:8080/api/cart/addToCart \
  -H "Authorization: Bearer <jwt>" \
  -H 'Content-Type: application/json' \
  -d '{"username":"alice","itemId":1,"quantity":2}'
```

Submit an order:

```bash
curl -X POST http://localhost:8080/api/order/submit/alice \
  -H "Authorization: Bearer <jwt>"
```

Read order history:

```bash
curl -H "Authorization: Bearer <jwt>" http://localhost:8080/api/order/history/alice
```

## Testing

Run the full test suite:

```bash
./mvnw test
```

Generate the JaCoCo coverage report:

```bash
./mvnw test jacoco:report
```

After running tests, the HTML coverage report is available at:

```text
target/site/jacoco/index.html
```

## Configuration

The default application configuration uses an in-memory H2 database and recreates the schema at startup.

Key settings:

```properties
spring.datasource.url=jdbc:h2:mem:bootapp;NON_KEYWORDS=user
spring.jpa.hibernate.ddl-auto=create
spring.jpa.defer-datasource-initialization=true
server.port=8080
spring.h2.console.enabled=true
spring.h2.console.path=/h2
```

## Project Structure

```text
src/main/java/com/example/demo
├── SareetaApplication.java
├── controllers
│   ├── CartController.java
│   ├── ItemController.java
│   ├── OrderController.java
│   └── UserController.java
├── model
│   ├── persistence
│   │   ├── Cart.java
│   │   ├── Item.java
│   │   ├── User.java
│   │   ├── UserOrder.java
│   │   └── repositories
│   └── requests
│       ├── CreateUserRequest.java
│       └── ModifyCartRequest.java
└── security
    ├── JWTAuthenticationFilter.java
    ├── JWTAuthorizationFilter.java
    ├── SecurityConfig.java
    ├── SecurityConstants.java
    └── UserDetailsServiceImpl.java
```

## Improvement Roadmap

Here are the most valuable improvements to make next.

### 1. Externalize Secrets and Security Settings

Move `SecurityConstants.SECRET` out of source code and load it from environment variables or a secrets manager. Also make token expiration configurable by environment.

Recommended approach:

- Add strongly typed Spring `@ConfigurationProperties` for JWT settings.
- Require a long random secret in non-local profiles.
- Keep demo defaults only in a local/dev profile.

### 2. Add DTOs for API Responses

Controllers currently return JPA entities directly. A more professional API should use response DTOs to avoid leaking persistence details, circular references, or internal fields.

Suggested DTOs:

- `UserResponse`
- `ItemResponse`
- `CartResponse`
- `OrderResponse`
- `ErrorResponse`

### 3. Strengthen Request Validation

Add Jakarta Bean Validation annotations and controller-level validation.

Examples:

- `@NotBlank` for usernames.
- `@Size(min = 8)` or stronger for passwords.
- `@Positive` for `itemId` and `quantity`.
- A custom validator to ensure password and confirmation match.

### 4. Prevent Negative Cart Totals

`Cart.removeItem` currently subtracts an item's price even if the item was not present in the cart. This can create negative totals. Update the method to subtract only when an item is actually removed, and reject invalid removal quantities at the API boundary.

### 5. Clear or Snapshot Carts After Order Submission

Order submission currently creates an order from the cart but does not clear the cart afterward. Decide on the desired business behavior:

- Clear cart after successful checkout, or
- Keep cart as-is and introduce explicit checkout state.

For realistic e-commerce behavior, clearing the cart after order creation is usually expected.

### 6. Add Service Layer

Business rules currently live inside controllers and entities. Introduce services to make the app easier to test and maintain:

- `UserService`
- `CartService`
- `ItemService`
- `OrderService`
- `JwtService`

### 7. Improve Persistence Modeling

Consider replacing `Cart.items` and `UserOrder.items` many-to-many lists with line-item entities that store quantity and historical price.

Suggested entities:

- `CartItem`
- `OrderItem`

This would support:

- Quantity tracking without duplicate item rows in lists.
- Order price snapshots even when product prices change later.
- Better reporting and inventory workflows.

### 8. Add OpenAPI Documentation

Add `springdoc-openapi` to generate interactive Swagger UI documentation for all endpoints.

### 9. Add Role-Based Authorization

The current app has customer-only behavior. If an admin catalog or admin order view is added, introduce roles such as:

- `ROLE_CUSTOMER`
- `ROLE_ADMIN`

### 10. Production Database Profiles

Add profiles for local H2 and production PostgreSQL/MySQL. Use migration tooling such as Flyway or Liquibase instead of `ddl-auto=create` in production.

### 11. Improve Error Responses

Return structured error bodies instead of empty responses for validation and authorization failures.

Example:

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Quantity must be greater than zero",
  "path": "/api/cart/addToCart"
}
```

### 12. Add CI/CD

Add a GitHub Actions workflow that runs:

- Maven test suite.
- JaCoCo coverage generation.
- Dependency vulnerability scanning.
- Static analysis.

## Production Readiness Checklist

Before deploying this application, complete the following:

- [ ] Move JWT secret and expiration settings to environment configuration.
- [ ] Disable H2 console outside local development.
- [ ] Replace in-memory H2 with a persistent database.
- [ ] Add Flyway or Liquibase migrations.
- [ ] Add DTOs and stop returning JPA entities directly.
- [ ] Add bean validation for all request payloads.
- [ ] Prevent negative cart totals and invalid quantities.
- [ ] Add structured error handling with `@ControllerAdvice`.
- [ ] Add refresh-token or token-rotation strategy if required.
- [ ] Add API documentation with OpenAPI.
- [ ] Add CI checks and dependency scanning.
- [ ] Add observability: logs, metrics, traces, and health checks.

## Final Notes

This project has a strong foundation: modern Spring Boot, stateless JWT authentication, BCrypt password storage, ownership-based authorization checks, and meaningful integration tests. With DTOs, validation, externalized secrets, improved cart/order modeling, and production profiles, it can evolve from a secure learning project into a polished e-commerce backend.
