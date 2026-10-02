# Real-Time Event Booking & Seat Management System

## Architecture Diagram

![Real-Time Event Booking System Architecture Diagram](./docs/images/event-booking-architecture.png)

A backend system for managing events, seat reservations, cancellations, and asynchronous booking notifications.

The primary focus of this project is **preventing seat overbooking under concurrent requests** while maintaining database consistency, fast event reads, and reliable booking operations.

---

## 🚀 Key Features

* User registration and login
* JWT-based authentication
* BCrypt password hashing
* Event listing and availability
* Seat booking
* Seat cancellation
* Concurrency-safe booking
* MySQL transactions
* Idempotent booking requests
* Database-level uniqueness constraints
* Redis caching using cache-aside strategy
* Asynchronous notification processing
* Producer-consumer pattern using `BlockingQueue`
* Layered backend architecture

---

## 🏗️ System Architecture

```text
                         ┌─────────────────────┐
                         │       Client        │
                         │  Postman / Frontend │
                         └──────────┬──────────┘
                                    │
                                    │ HTTP / REST
                                    ▼
                         ┌─────────────────────┐
                         │    Java REST API    │
                         │   Java HTTPServer   │
                         └──────────┬──────────┘
                                    │
                  ┌─────────────────┼─────────────────┐
                  │                 │                 │
                  ▼                 ▼                 ▼
           ┌────────────┐    ┌────────────┐    ┌──────────────┐
           │   Redis    │    │   MySQL    │    │ Notification │
           │   Cache    │    │  Database  │    │    Queue     │
           └────────────┘    └────────────┘    └──────┬───────┘
                                                        │
                                                        ▼
                                               ┌─────────────────┐
                                               │ Notification    │
                                               │     Worker      │
                                               └─────────────────┘
```

### Application Layers

```text
HTTP Request
     │
     ▼
┌──────────────┐
│   Handler    │  → HTTP request/response handling
└──────┬───────┘
       ▼
┌──────────────┐
│   Service    │  → Business logic
└──────┬───────┘
       ▼
┌──────────────┐
│ Repository   │  → SQL/database operations
└──────┬───────┘
       ▼
┌──────────────┐
│    MySQL     │
└──────────────┘
```

---

## 🛠️ Tech Stack

### Backend

* Java 21
* Java Built-in `HttpServer`
* JDBC
* Maven

### Database

* MySQL 8

### Caching

* Redis
* Jedis

### Security

* JWT
* BCrypt

### Asynchronous Processing

* Java `BlockingQueue`
* Background Worker Thread

### Serialization

* Jackson

---

# 🔥 Core System Design Concepts

## 1. Concurrency-Safe Seat Booking

The most important challenge in this system is preventing **overbooking when multiple users attempt to book the last available seat simultaneously**.

### ❌ Naive Approach

A problematic implementation would first check availability:

```sql
SELECT available_seats
FROM events
WHERE id = ?;
```

and then perform:

```sql
UPDATE events
SET available_seats = available_seats - 1
WHERE id = ?;
```

Two requests could read the same available seat count before either request performs the update.

This creates a **race condition**.

### ✅ Implemented Approach

The system performs an atomic conditional update:

```sql
UPDATE events
SET available_seats = available_seats - 1
WHERE id = ?
AND available_seats > 0;
```

The database evaluates the condition and update atomically.

If one request consumes the last available seat:

```text
Request A → UPDATE affects 1 row → Booking succeeds
Request B → UPDATE affects 0 rows → Booking rejected
```

The API returns:

```text
409 Conflict
```

when no seat is available.

### Additional Database Protection

The database also contains:

```sql
UNIQUE(event_id, seat_number)
```

This prevents the same seat from being booked twice for the same event even if an application-level mistake occurs.

---

# 2. Database Transactions

Seat reservation involves multiple operations:

```text
1. Decrease available seats
2. Insert booking
```

These operations must succeed or fail together.

Therefore, the booking operation uses a MySQL transaction:

```text
BEGIN TRANSACTION

    ↓
Decrease available seats

    ↓
Insert booking

    ↓
COMMIT
```

If an error occurs:

```text
ROLLBACK
```

This prevents situations such as:

```text
Seat count decreased
        +
Booking record NOT created
```

---

# 3. Idempotency

The booking API accepts an idempotency key.

Example:

```json
{
  "eventId": 1,
  "seatNumber": 25,
  "idempotencyKey": "booking-request-123"
}
```

Suppose the client sends the request and the booking succeeds, but the network response is lost.

The client may retry the same request.

Without idempotency:

```text
Request 1 → Booking created
Request 2 → Another booking created
```

With idempotency:

```text
Request 1 → Booking created
Request 2 → Existing booking returned
```

The database also enforces:

```sql
UNIQUE(idempotency_key)
```

This provides an additional layer of protection against duplicate requests.

---

# 4. Redis Caching

Event listing is a read-heavy operation, so Redis is used as a cache.

The project follows the **cache-aside pattern**.

### Cache Miss

```text
Client
  │
  ▼
API
  │
  ▼
Redis
  │
  └── Cache Miss
          │
          ▼
        MySQL
          │
          ▼
     Store in Redis
          │
          ▼
       Response
```

### Cache Hit

```text
Client
  │
  ▼
API
  │
  ▼
Redis
  │
  └── Cache Hit
          │
          ▼
       Response
```

The current event list is cached with a short TTL.

**MySQL remains the source of truth for bookings and seat availability.**

Redis is used for performance, not for maintaining booking consistency.

---

# 5. Asynchronous Notification Processing

Booking confirmation should not need to wait for notification processing.

After a successful booking, the service publishes a notification task to a `BlockingQueue`.

```text
                Booking Request
                       │
                       ▼
                Booking Service
                       │
                       ▼
                 MySQL Transaction
                       │
                       ▼
                Booking Successful
                       │
                       ▼
              Notification Queue
                       │
                       ▼
             Background Worker
                       │
                       ▼
             Process Notification
```

The current implementation simulates notification processing.

In a production system, the in-memory queue could be replaced with a durable messaging system such as:

* Apache Kafka
* RabbitMQ
* Amazon SQS

---

# 🔐 Authentication

The application uses JWT-based authentication.

### Login Flow

```text
User
 │
 │ email + password
 ▼
Auth API
 │
 ▼
Verify BCrypt Password
 │
 ▼
Generate JWT
 │
 ▼
Return Token
```

For protected APIs:

```text
Authorization: Bearer <JWT>
```

The JWT contains the authenticated user's ID and role.

The server extracts the user ID from the token instead of trusting a user ID supplied by the client.

---

# 🗄️ Database Design

The system uses four primary tables:

```text
┌──────────────┐
│    users     │
└──────┬───────┘
       │
       │ 1:N
       ▼
┌──────────────┐
│    events    │
└──────┬───────┘
       │
       │ 1:N
       ▼
┌──────────────┐
│   bookings   │
└──────┬───────┘
       │
       │ 1:N
       ▼
┌────────────────┐
│ notifications  │
└────────────────┘
```

### Relationships

```text
User 1 ───────── N Event
User 1 ───────── N Booking
Event 1 ──────── N Booking
Booking 1 ────── N Notification
```

### Main Constraints

```text
users.email
        ↓
     UNIQUE

bookings.idempotency_key
        ↓
     UNIQUE

(event_id, seat_number)
        ↓
     UNIQUE
```

The complete database schema is available at:

```text
sql/schema.sql
```

---

# 📡 REST API

## Authentication

| Method | Endpoint             | Description                       |
| ------ | -------------------- | --------------------------------- |
| POST   | `/api/auth/register` | Register a new user               |
| POST   | `/api/auth/login`    | Authenticate user and receive JWT |

## Events

| Method | Endpoint      | Description         |
| ------ | ------------- | ------------------- |
| GET    | `/api/events` | Retrieve all events |

## Bookings

| Method | Endpoint             | Description      |
| ------ | -------------------- | ---------------- |
| POST   | `/api/bookings`      | Book a seat      |
| DELETE | `/api/bookings/{id}` | Cancel a booking |

---

# 📋 HTTP Status Codes

| Status                      | Meaning                             |
| --------------------------- | ----------------------------------- |
| `200 OK`                    | Request completed successfully      |
| `201 Created`               | Resource successfully created       |
| `400 Bad Request`           | Invalid request                     |
| `401 Unauthorized`          | Missing or invalid authentication   |
| `404 Not Found`             | Resource not found                  |
| `409 Conflict`              | Booking conflict / seat unavailable |
| `500 Internal Server Error` | Unexpected server error             |

---

# 📁 Project Structure

```text
event-booking-system/
│
├── src/
│   └── main/
│       └── java/
│           └── com/
│               └── eventbooking/
│
│                   ├── cache/
│                   │   └── RedisClient.java
│                   │
│                   ├── database/
│                   │   └── DatabaseConnection.java
│                   │
│                   ├── handler/
│                   │   ├── AuthHandler.java
│                   │   ├── EventHandler.java
│                   │   └── BookingHandler.java
│                   │
│                   ├── model/
│                   │   ├── User.java
│                   │   ├── Event.java
│                   │   └── Booking.java
│                   │
│                   ├── queue/
│                   │   ├── NotificationQueue.java
│                   │   └── NotificationTask.java
│                   │
│                   ├── repository/
│                   │   ├── UserRepository.java
│                   │   ├── EventRepository.java
│                   │   └── BookingRepository.java
│                   │
│                   ├── security/
│                   │   ├── JwtUtil.java
│                   │   ├── AuthUtil.java
│                   │   └── PasswordUtil.java
│                   │
│                   ├── service/
│                   │   ├── AuthService.java
│                   │   ├── EventService.java
│                   │   └── BookingService.java
│                   │
│                   ├── worker/
│                   │   └── NotificationWorker.java
│                   │
│                   └── Application.java
│
├── sql/
│   └── schema.sql
│
├── .gitignore
├── pom.xml
└── README.md
```

---

# ⚙️ Setup & Installation

## Prerequisites

Install:

* JDK 21
* Maven
* MySQL 8
* Redis

Verify Java:

```bash
java -version
```

Verify Maven:

```bash
mvn -version
```

---

## 1. Clone the Repository

```bash
git clone <your-repository-url>
cd event-booking-system
```

## 2. Configure Environment Variables

The application expects:

```powershell
$env:DB_PASSWORD="your_mysql_password"
$env:REDIS_PASSWORD="your_redis_password"
$env:JWT_SECRET="your_long_random_secret"
```

Secrets should not be committed to Git.

## 3. Setup MySQL

Create the database and tables using:

```text
sql/schema.sql
```

## 4. Start Redis

Make sure Redis is running and accessible from the application.

## 5. Build the Project

```bash
mvn clean compile
```

## 6. Run the Application

```bash
mvn exec:java
```

The API starts at:

```text
http://localhost:8080
```

---

# 🧪 Concurrency Test

A one-seat event can be used to test concurrent booking requests.

```text
Event
Total Seats: 1
Available Seats: 1
```

Two clients attempt to book the same seat simultaneously:

```text
                ┌──────────────┐
Request A ─────►│              │
                │   Database   │──► Seat available
Request B ─────►│              │
                │              │
                └──────────────┘
```

Expected result:

```text
Request A → 201 Created
Request B → 409 Conflict
```

Only one booking should exist for the seat.

This demonstrates that the system prevents overbooking using database-level concurrency control.

---

# 📈 Scalability Considerations

The current implementation runs as a single Java application instance.

A production architecture could scale horizontally:

```text
                    Load Balancer
                         │
             ┌───────────┼───────────┐
             ▼           ▼           ▼
          API-1        API-2       API-3
             │           │           │
             └───────────┼───────────┘
                         │
                  ┌──────┴──────┐
                  │             │
                Redis         MySQL
                  │
                  ▼
              Message Broker
                  │
          ┌───────┴───────┐
          ▼               ▼
       Worker-1        Worker-2
```

Possible production improvements include:

* Multiple stateless API instances
* Load balancing
* Distributed caching
* Durable message queues
* Database connection pooling
* Rate limiting
* Refresh tokens
* Monitoring and centralized logging
* Containerized deployment
* Payment integration
* Real email/SMS notifications

---

# 🎯 Design Decisions

### Why MySQL?

Booking requires strong consistency, transactions, foreign keys, and uniqueness constraints.

### Why Redis?

Event listings are frequently read and can benefit from low-latency caching.

### Why BlockingQueue?

It provides a simple producer-consumer model for the current implementation without introducing an external messaging system.

### Why JWT?

It allows the API to authenticate requests without storing server-side session state.

### Why database-level concurrency control?

Application-level checks alone are vulnerable to race conditions when multiple requests access the same seat concurrently.

---

# 📚 What This Project Demonstrates

This project demonstrates practical backend and system-design concepts including:

* REST API design
* Layered architecture
* Authentication
* Authorization fundamentals
* SQL database design
* Transactions
* ACID consistency
* Concurrency control
* Race conditions
* Idempotency
* Database constraints
* Redis caching
* Cache-aside pattern
* Producer-consumer architecture
* Asynchronous processing
* Horizontal scaling concepts

---

## Future Improvements

* Complete organizer-specific authorization
* Event update/delete APIs
* Event booking statistics
* Redis cache invalidation for event mutations
* Durable message broker integration
* Connection pooling
* Rate limiting
* Payment processing
* Real notification delivery
* Docker deployment
* Automated tests
* CI/CD pipeline
* Observability and monitoring
