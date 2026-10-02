# Booking Flow & Concurrency Control

![Booking Flow](../../images/booking-flow.png)

## 1. Normal Booking Flow

A user sends a booking request containing:

```json
{
  "eventId": 1,
  "seatNumber": 25,
  "idempotencyKey": "booking-123"
}
```

The user identity is obtained from the JWT rather than accepting a `userId` from the request body.

---

## 2. Complete Booking Sequence

```mermaid
sequenceDiagram

    actor User
    participant Handler as BookingHandler
    participant Service as BookingService
    participant Repo as BookingRepository
    participant DB as MySQL
    participant Queue as NotificationQueue
    participant Worker as NotificationWorker

    User->>Handler: POST /api/bookings
    Handler->>Handler: Extract JWT
    Handler->>Service: createBooking(userId, eventId, seat, key)

    Service->>Repo: findByIdempotencyKey(key)
    Repo->>DB: SELECT booking by key
    DB-->>Repo: Result

    alt Booking already exists
        Repo-->>Service: Existing booking
        Service-->>Handler: Existing booking
        Handler-->>User: 200 OK
    else New booking
        Service->>Repo: createBooking(...)
        Repo->>DB: BEGIN TRANSACTION

        Repo->>DB: UPDATE events<br/>SET available_seats = available_seats - 1<br/>WHERE available_seats > 0

        alt Seat available
            DB-->>Repo: 1 row updated

            Repo->>DB: INSERT booking
            DB-->>Repo: Booking inserted

            Repo->>DB: COMMIT
            Repo-->>Service: Booking created

            Service->>Queue: Publish notification
            Queue->>Worker: Consume task
            Worker->>Worker: Process notification

            Service-->>Handler: Booking
            Handler-->>User: 201 Created

        else No seat available
            DB-->>Repo: 0 rows updated
            Repo->>DB: ROLLBACK
            Repo-->>Service: Booking conflict
            Service-->>Handler: Conflict
            Handler-->>User: 409 Conflict
        end
    end
```

---

# 3. Concurrent Booking Scenario

Consider an event with only one remaining seat.

```text
Available seats = 1
```

Two users send booking requests at approximately the same time.

```text
User A ───────────────┐
                     │
                     ▼
                Booking API
                     ▲
                     │
User B ──────────────┘
```

Both requests eventually reach the database.

The important operation is:

```sql
UPDATE events
SET available_seats = available_seats - 1
WHERE id = ?
AND available_seats > 0;
```

---

## 4. What Happens Internally?

Suppose User A's transaction reaches the database first.

```text
Initial:
available_seats = 1

User A:
UPDATE ... WHERE available_seats > 0

Result:
1 row affected

available_seats = 0
```

User A can then insert the booking and commit.

When User B's transaction attempts the same operation:

```text
Current:
available_seats = 0

User B:
UPDATE ... WHERE available_seats > 0

Result:
0 rows affected
```

The application interprets this as a booking conflict.

```text
User A → 201 Created
User B → 409 Conflict
```

Therefore:

```text
Only one booking
        +
No negative seat count
        +
No overbooking
```

---

# 5. Why Not Use `synchronized`?

A Java `synchronized` block could protect access inside one JVM:

```java
synchronized (lock) {
    // booking logic
}
```

However, this does not provide sufficient protection when the application runs on multiple servers.

For example:

```text
             Load Balancer
                 │
          ┌──────┴──────┐
          ▼             ▼
       Server A       Server B
          │             │
      JVM Lock A     JVM Lock B
```

The locks belong to different JVMs.

The database, however, is shared:

```text
Server A ───────┐
                ▼
             MySQL
                ▲
Server B ───────┘
```

Therefore, the database-level atomic operation provides concurrency protection across application instances.

---

# 6. Why a Transaction Is Still Required

The atomic update solves the seat race condition, but booking still consists of multiple operations.

```text
1. Decrease available seats
2. Insert booking
```

Without a transaction:

```text
Seat decreased
      ↓
Application crashes
      ↓
Booking never inserted
```

The event would incorrectly lose a seat.

With a transaction:

```text
BEGIN
  ↓
Seat update
  ↓
Booking insert
  ↓
COMMIT
```

If the booking insertion fails:

```text
ROLLBACK
```

The seat update is also undone.

---

# 7. Idempotency and Concurrency Solve Different Problems

These two concepts are easy to confuse.

### Concurrency control

Protects against:

```text
Multiple users
      ↓
Same seat
      ↓
At the same time
```

### Idempotency

Protects against:

```text
Same client
      ↓
Same request
      ↓
Retry due to timeout/network issue
```

Example:

```text
Request:
idempotencyKey = "booking-123"

First request:
Booking created

Retry:
Existing booking returned
```

Therefore the system uses both:

```text
Concurrency control
        +
Transaction
        +
Unique constraint
        +
Idempotency
```

to make booking safer.

---

# 8. Cancellation Flow

Cancellation is implemented as a state transition rather than deleting the booking.

```mermaid
sequenceDiagram

    actor User
    participant Handler as BookingHandler
    participant Service as BookingService
    participant Repo as BookingRepository
    participant DB as MySQL

    User->>Handler: DELETE /api/bookings/{id}
    Handler->>Handler: Extract JWT
    Handler->>Service: cancelBooking(bookingId, userId)
    Service->>Repo: cancelBooking(...)
    Repo->>DB: BEGIN TRANSACTION

    Repo->>DB: Mark booking CANCELLED
    Repo->>DB: Increase available_seats

    Repo->>DB: COMMIT

    Repo-->>Service: Success
    Service-->>Handler: Success
    Handler-->>User: 200 OK
```

The booking record remains in the database with:

```text
status = CANCELLED
```

This preserves booking history.

---

# 9. Key Interview Explanation

If asked:

> "How did you prevent overbooking?"

A concise answer is:

> "I avoided doing a separate availability check followed by an update because that creates a race condition. Instead, I use an atomic conditional UPDATE that decrements the seat only when `available_seats > 0`. If the update affects zero rows, the seat is unavailable and I return 409 Conflict. The seat update and booking insertion are inside the same MySQL transaction, and I also have a unique constraint on `(event_id, seat_number)` as a final database-level safeguard."

That is the core concurrency design of this project.
