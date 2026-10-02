package com.eventbooking.model;

public class Booking {

    private long id;
    private long userId;
    private long eventId;
    private int seatNumber;
    private String status;
    private String idempotencyKey;

    public Booking() {
    }

    public Booking(
            long id,
            long userId,
            long eventId,
            int seatNumber,
            String status,
            String idempotencyKey
    ) {
        this.id = id;
        this.userId = userId;
        this.eventId = eventId;
        this.seatNumber = seatNumber;
        this.status = status;
        this.idempotencyKey = idempotencyKey;
    }

    public long getId() {
        return id;
    }

    public long getUserId() {
        return userId;
    }

    public long getEventId() {
        return eventId;
    }

    public int getSeatNumber() {
        return seatNumber;
    }

    public String getStatus() {
        return status;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }
}