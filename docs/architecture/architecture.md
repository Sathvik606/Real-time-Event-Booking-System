# System Architecture

![System Architecture](../../images/architecture.png)

## 1. Overview

The Real-Time Event Booking & Seat Management System follows a layered backend architecture.

The application is built using Java 21's built-in HTTP server and communicates with MySQL, Redis, and an asynchronous notification queue.

```mermaid
flowchart TB
    Client["Client<br/>Postman / Frontend"]

    API["Java REST API<br/>HttpServer"]

    Handler["HTTP Handlers<br/>Auth / Event / Booking"]

    Service["Service Layer<br/>Business Logic"]

    Repository["Repository Layer<br/>JDBC / SQL"]

    MySQL[("MySQL<br/>Source of Truth")]

    Redis[("Redis<br/>Cache")]

    Queue["Notification Queue<br/>BlockingQueue"]

    Worker["Notification Worker<br/>Background Thread"]

    Client -->|HTTP / REST| API
    API --> Handler
    Handler --> Service
    Service --> Repository
    Repository --> MySQL

    Service -->|Cache Read / Write| Redis
    Service -->|Publish Notification| Queue
    Queue --> Worker
```

---

## 2. Application Layers

### HTTP Layer

The HTTP handlers are responsible for:

* Reading HTTP requests
* Parsing JSON
* Extracting JWT authentication information
* Calling the appropriate service
* Returning HTTP responses

Main handlers:

```text
AuthHandler
EventHandler
BookingHandler
```

---

### Service Layer

The service layer contains business logic.

```text
AuthService
EventService
BookingService
```

Examples:

* Validate registration/login
* Generate JWT
* Check idempotency
* Book seats
* Cancel bookings
* Publish notification tasks
* Read events through Redis cache

---

### Repository Layer

Repositories are responsible for database operations using JDBC.

```text
UserRepository
EventRepository
BookingRepository
```

The repository layer contains SQL queries and transaction handling.

---

## 3. Booking Request Flow

```mermaid
sequenceDiagram
    participant C as Client
    participant H as BookingHandler
    participant S as BookingService
    participant R as BookingRepository
    participant DB as MySQL
    participant Q as Notification Queue
    participant W as Notification Worker

    C->>H: POST /api/bookings
    H->>H: Validate JWT
    H->>S: createBooking(...)
    S->>R: Check idempotency key
    R->>DB: SELECT existing booking

    alt Existing booking
        DB-->>R: Existing booking
        R-->>S: Existing booking
        S-->>H: Existing booking
    else New booking
        S->>R: Create booking
        R->>DB: BEGIN TRANSACTION
        R->>DB: Atomic seat UPDATE
        R->>DB: INSERT booking
        R->>DB: COMMIT
        DB-->>R: Booking created
        R-->>S: Booking
        S->>Q: Publish notification
        Q->>W: Consume task
        W->>W: Process notification
        S-->>H: Booking response
    end

    H-->>C: HTTP response
```

---

## 4. Redis Cache Strategy

The event listing endpoint uses a cache-aside strategy.

```mermaid
flowchart LR
    Request["GET /api/events"]
    Redis[("Redis")]
    MySQL[("MySQL")]

    Request --> Redis

    Redis -->|Cache Hit| Response["Return events"]

    Redis -->|Cache Miss| MySQL
    MySQL --> Data["Event data"]
    Data --> Redis
    Redis --> Response
```

The current implementation caches the event list with a TTL.

MySQL remains the source of truth.

Redis is not used to decide whether a booking is valid.

---

## 5. Notification Architecture

Notifications are processed asynchronously.

```mermaid
flowchart LR
    Booking["Successful Booking"]
    Queue["BlockingQueue"]
    Worker["Notification Worker"]
    Processing["Process Notification"]

    Booking --> Queue
    Queue --> Worker
    Worker --> Processing
```

This prevents notification processing from becoming part of the critical booking path.

The current worker simulates notification delivery.

A production implementation could replace the in-memory queue with Kafka, RabbitMQ, or Amazon SQS.

---

## 6. Scalability

The current implementation runs as a single API instance.

A production deployment could use multiple stateless API instances:

```mermaid
flowchart TB
    Client["Clients"]
    LB["Load Balancer"]

    API1["API Instance 1"]
    API2["API Instance 2"]
    API3["API Instance 3"]

    Redis[("Redis")]
    DB[("MySQL")]
    MQ["Durable Message Broker"]
    Workers["Notification Workers"]

    Client --> LB

    LB --> API1
    LB --> API2
    LB --> API3

    API1 --> Redis
    API2 --> Redis
    API3 --> Redis

    API1 --> DB
    API2 --> DB
    API3 --> DB

    API1 --> MQ
    API2 --> MQ
    API3 --> MQ

    MQ --> Workers
```

This is a production-oriented extension and is **not claimed as part of the current local implementation**.
