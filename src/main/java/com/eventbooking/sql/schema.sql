CREATE DATABASE IF NOT EXISTS event_booking;

USE event_booking;

CREATE TABLE users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'USER',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE events (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    organizer_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    venue VARCHAR(255),
    start_time DATETIME,
    end_time DATETIME,
    total_seats INT NOT NULL,
    available_seats INT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_event_organizer
        FOREIGN KEY (organizer_id)
        REFERENCES users(id),

    CONSTRAINT chk_total_seats
        CHECK (total_seats > 0),

    CONSTRAINT chk_available_seats
        CHECK (available_seats >= 0)
);

CREATE TABLE bookings (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    event_id BIGINT NOT NULL,
    seat_number INT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'CONFIRMED',
    idempotency_key VARCHAR(100) NOT NULL UNIQUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    cancelled_at TIMESTAMP NULL,

    CONSTRAINT fk_booking_user
        FOREIGN KEY (user_id)
        REFERENCES users(id),

    CONSTRAINT fk_booking_event
        FOREIGN KEY (event_id)
        REFERENCES events(id),

    CONSTRAINT uk_event_seat
        UNIQUE (event_id, seat_number)
);

CREATE TABLE notifications (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    booking_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    type VARCHAR(30) NOT NULL,
    message TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    sent_at TIMESTAMP NULL,

    CONSTRAINT fk_notification_booking
        FOREIGN KEY (booking_id)
        REFERENCES bookings(id),

    CONSTRAINT fk_notification_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
);

CREATE INDEX idx_events_organizer
    ON events(organizer_id);

CREATE INDEX idx_events_start_time
    ON events(start_time);

CREATE INDEX idx_bookings_event
    ON bookings(event_id);

CREATE INDEX idx_bookings_user
    ON bookings(user_id);