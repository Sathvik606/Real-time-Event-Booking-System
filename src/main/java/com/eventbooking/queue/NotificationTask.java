package com.eventbooking.queue;

public class NotificationTask {

    private final long bookingId;
    private final long userId;
    private final String message;

    public NotificationTask(
            long bookingId,
            long userId,
            String message) {

        this.bookingId = bookingId;
        this.userId = userId;
        this.message = message;
    }

    public long getBookingId() {
        return bookingId;
    }

    public long getUserId() {
        return userId;
    }

    public String getMessage() {
        return message;
    }
}