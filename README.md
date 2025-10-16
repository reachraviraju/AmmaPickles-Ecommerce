# Amma Pickles Backend 🍯

[![Java](https://img.shields.io/badge/Java-17-blue)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-green)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/License-MIT-yellow)](LICENSE)

Amma Pickles is a Spring Boot backend application for managing products, categories, users, cart, Addresses and orders for an e-commerce system.
It supports basic authentication, role-based access, and CRUD operations.

---

## Features 🚀

* User registration & login
* Admin & Customer roles
* CRUD for Products & Categories
* Cart management (add/update/remove/clear)
* Order placement & tracking
* Address management
* Password encryption (BCrypt) & Basic authentication

---

## Tech Stack 🛠

* Java 17
* Spring Boot 3.x
* Spring Data JPA
* Spring Security
* ModelMapper
* MySQL
* Maven

---

## Prerequisites ⚙️

* Java 17+
* Maven 3+
* MySQL database

---

## Setup & Run 💻

1. Clone the repo:

```bash
git clone <repo-url>
cd amma-pickles-backend
```

2. Configure `application.properties`:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/amma_pickles
spring.datasource.username=root
spring.datasource.password=your_password
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
```

3. Build & run:

```bash
mvn clean install
mvn spring-boot:run
```

App runs at `http://localhost:8080`.

---

## Database Tables 🗄

* `users`, `roles`, `products`, `categories`
* `cart`, `cart_items`
* `orders`, `order_items`
* `addresses`

Seed `roles` table with: `ADMIN`, `CUSTOMER`.

---

## API Endpoints 📡

### Users

| Method | Endpoint              | Description         |
| ------ | --------------------- | ------------------- |
| POST   | `/api/users/register` | Register a new user |
| POST   | `/api/users/login`    | Login user          |
| GET    | `/api/users/{id}`     | Get user by ID      |
| PUT    | `/api/users/{id}`     | Update user         |

**Sample Request: Register**

```json
POST /api/users/register
{
  "username": "john",
  "password": "password123",
  "email": "john@example.com",
  "address": "123 Street",
  "phoneNumber": "9876543210"
}
```

---

### Addresses

| Method | Endpoint                       | Description                  |
| ------ | ------------------------------ | ---------------------------- |
| GET    | `/api/addresses/user/{userId}` | Get all addresses for a user |
| GET    | `/api/addresses/{id}`          | Get address by ID            |
| POST   | `/api/addresses/user/{userId}` | Add new address for user     |
| PUT    | `/api/addresses/{id}`          | Update address               |
| DELETE | `/api/addresses/{id}`          | Delete address               |

---

### Products

| Method | Endpoint                              | Description              |
| ------ | ------------------------------------- | ------------------------ |
| GET    | `/api/products`                       | Get all products         |
| GET    | `/api/products/{id}`                  | Get product by ID        |
| POST   | `/api/products`                       | Add new product          |
| PUT    | `/api/products/{id}`                  | Update product           |
| DELETE | `/api/products/{id}`                  | Delete product           |
| GET    | `/api/products/category/{categoryId}` | Get products by category |
| GET    | `/api/products/search?name={name}`    | Search products by name  |

---

### Categories

| Method | Endpoint               | Description        |
| ------ | ---------------------- | ------------------ |
| GET    | `/api/categories`      | Get all categories |
| GET    | `/api/categories/{id}` | Get category by ID |
| POST   | `/api/categories`      | Add new category   |
| PUT    | `/api/categories/{id}` | Update category    |
| DELETE | `/api/categories/{id}` | Delete category    |

---

### Cart

| Method | Endpoint                                                     | Description                 |
| ------ | ------------------------------------------------------------ | --------------------------- |
| GET    | `/api/cart/user/{userId}`                                    | Get all cart items for user |
| POST   | `/api/cart/user/{userId}/product/{productId}?quantity={qty}` | Add product to cart         |
| PUT    | `/api/cart/item/{cartItemId}`                                | Update cart item quantity   |
| DELETE | `/api/cart/item/{cartItemId}`                                | Remove cart item            |
| DELETE | `/api/cart/user/{userId}/clear`                              | Clear entire cart           |

---

### Orders

| Method | Endpoint                    | Description               |
| ------ | --------------------------- | ------------------------- |
| GET    | `/api/orders/user/{userId}` | Get all orders for a user |
| GET    | `/api/orders/{id}`          | Get order by ID           |
| POST   | `/api/orders/user/{userId}` | Place a new order         |
| DELETE | `/api/orders/{id}`          | Cancel an order           |

---

## Authentication 🔑

* BCrypt password encryption
* Role-based access (`ADMIN` & `CUSTOMER`)
* Basic HTTP authentication
* Public endpoints: `/api/users/register`, `/api/users/login`

---

## Error Handling ⚠️

* `ResourceNotFoundException` – thrown when a resource (User, Product, Category, etc.) is not found
* `RuntimeException` – generic runtime errors
* Global exception handling can be extended with `@ControllerAdvice`

---

## License 📄

MIT License – for learning/demo purposes.
