# Amma Pickles Backend 🍯

[![Java](https://img.shields.io/badge/Java-17-blue)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-green)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/License-MIT-yellow)](LICENSE)

Amma Pickles is a Spring Boot backend application for managing products, categories, users, cart, Addresses and orders for an e-commerce system.
It supports basic authentication, role-based access, and CRUD operations.

---

## Features 🚀

* JWT-based Authentication & Authorization  
* User registration & login  
* Password reset via email  
* Admin & Customer roles  
* CRUD for Products & Categories  
* Cart management (add/update/remove/clear)  
* Order placement & tracking  
* Address management  
* BCrypt password encryption  

---

## Tech Stack 🛠

* Java 17  
* Spring Boot 3.x  
* Spring Security (JWT)  
* Spring Data JPA  
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
jwt.secret=your_secret_key

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

# 📘 Amma Pickles REST API Endpoints


---

## 🔒 SECURITY CONFIGURATION
### Spring Security (JWT Based)

| Access            | Endpoint | Description |
| ----------------- | ----------------------------------------------------- | ------------------------------------------------- |
| **Public**        | `/api/auth/**`                                        | All authentication endpoints                      |
| **Public**        | `/api/users/email/**`                                 | Fetch user by email (used for verification/reset) |
| **Public**        | `GET /api/products/**`                                | Product and category browsing                     |
| **Customer Only** | `/api/cart/**`, `/api/orders/**`, `/api/addresses/**` | Requires role `CUSTOMER`                          |
| **Admin Only**    | `/api/products/**`, `/api/categories/**`              | Requires role `ADMIN`                             |
| **All Others**    | Require valid JWT token                               |                                                   |

---



## 🔐 AUTH CONTROLLER (`/api/auth`)

Handles registration, login, password reset, and email verification.

| Method | Endpoint                           | Description                              |
| ------ | ---------------------------------- | ---------------------------------------- |
| POST   | `/api/auth/register`               | Register a new user                      |
| POST   | `/api/auth/login`                  | Login user and get JWT token             |
| PUT    | `/api/auth/reset-password/{email}` | Reset password by email                  |
| GET    | `/api/auth/verify/{token}`         | Verify user email/token (if implemented) |

---

## 👤 USER CONTROLLER (`/api/users`)
Manages user profile details.


| Method | Endpoint                   | Description          |
| ------ | -------------------------- | -------------------- |
| GET    | `/api/users/{id}`          | Get user by ID       |
| PUT    | `/api/users/{id}`          | Update user details  |
| GET    | `/api/users/email/{email}` | Get user by email ID |


---

## 🏠 ADDRESS CONTROLLER (`/api/addresses`)
Handles user addresses.

| Method | Endpoint                       | Description                  |
| ------ | ------------------------------ | ---------------------------- |
| GET    | `/api/addresses/user/{userId}` | Get all addresses for a user |
| GET    | `/api/addresses/{id}`          | Get address by ID            |
| POST   | `/api/addresses/user/{userId}` | Add new address for user     |
| PUT    | `/api/addresses/{id}`          | Update address               |
| DELETE | `/api/addresses/{id}`          | Delete address               |


---

## 🏷 CATEGORY CONTROLLER (`/api/categories`)
Manages product categories.

| Method | Endpoint | Description |
| ------- | -------- | ------------ |
| **GET** | `/api/categories` | Get all categories |
| **GET** | `/api/categories/{id}` | Get category by ID |
| **POST** | `/api/categories` | Create new category |
| **PUT** | `/api/categories/{id}` | Update category |
| **DELETE** | `/api/categories/{id}` | Delete category by ID |

---

## 📦 PRODUCT CONTROLLER (`/api/products`)
Handles product operations and filters.

| Method | Endpoint                              | Description              |
| ------ | ------------------------------------- | ------------------------ |
| GET    | `/api/products`                       | Get all products         |
| GET    | `/api/products/{id}`                  | Get product by ID        |
| POST   | `/api/products`                       | Add new product (Admin)  |
| PUT    | `/api/products/{id}`                  | Update product (Admin)   |
| DELETE | `/api/products/{id}`                  | Delete product (Admin)   |
| GET    | `/api/products/category/{categoryId}` | Get products by category |
| GET    | `/api/products/search?name={name}`    | Search products by name  |


---

## 🛒 CART CONTROLLER (`/api/cart`)
Handles shopping cart logic.

| Method | Endpoint                                                     | Description                 |
| ------ | ------------------------------------------------------------ | --------------------------- |
| GET    | `/api/cart/user/{userId}`                                    | Get all cart items for user |
| POST   | `/api/cart/user/{userId}/product/{productId}?quantity={qty}` | Add product to cart         |
| PUT    | `/api/cart/item/{cartItemId}`                                | Update cart item quantity   |
| DELETE | `/api/cart/item/{cartItemId}`                                | Remove cart item            |
| DELETE | `/api/cart/user/{userId}/clear`                              | Clear entire cart           |


---

## 🧾 ORDER CONTROLLER (`/api/orders`)
Handles order placement, tracking, and cancellation.

| Method | Endpoint                    | Description               |
| ------ | --------------------------- | ------------------------- |
| GET    | `/api/orders/user/{userId}` | Get all orders for a user |
| GET    | `/api/orders/{id}`          | Get order by ID           |
| POST   | `/api/orders/user/{userId}` | Place a new order         |
| DELETE | `/api/orders/{id}`          | Cancel an order           |

---


## 🧠 TOKEN FORMAT

Each protected endpoint requires a JWT token in the header:


---

## ✅ TESTING IN POSTMAN

1. **Register** via `/api/auth/register`
2. **Login** to get JWT token
3. Use the token in the `Authorization` header
4. Access other endpoints (User, Product, Cart, Order, etc.)

---


## Error Handling ⚠️

* `ResourceNotFoundException` – thrown when a resource (User, Product, Category, etc.) is not found
* `RuntimeException` – generic runtime errors
* Global exception handling can be extended with `@ControllerAdvice`


---

## 👨‍💻 Developer
**Ravi (Raju)**  
Java Developer | Spring Boot | REST APIs  


---

## License 📄

MIT License – for learning/demo purposes.
