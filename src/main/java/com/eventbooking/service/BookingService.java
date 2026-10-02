package com.eventbooking.service;

import java.sql.SQLException;

import com.eventbooking.model.Booking;
import com.eventbooking.queue.NotificationQueue;
import com.eventbooking.queue.NotificationTask;
import com.eventbooking.repository.BookingRepository;

public class BookingService {

    private final BookingRepository bookingRepository;

    public BookingService() {
        this.bookingRepository = new BookingRepository();
    }

    public Booking createBooking(
            long userId,
            long eventId,
            int seatNumber,
            String idempotencyKey) throws Exception {

        if (seatNumber <= 0) {
            throw new IllegalArgumentException(
                    "Seat number must be greater than 0");
        }

        Booking existingBooking =
                bookingRepository.findByIdempotencyKey(idempotencyKey);

        if (existingBooking != null) {
            return existingBooking;
        }

        // Create booking in MySQL
        Booking booking = bookingRepository.createBooking(
                userId,
                eventId,
                seatNumber,
                idempotencyKey);

        // Publish notification only after successful booking
        NotificationQueue.publish(
                new NotificationTask(
                        booking.getId(),
                        userId,
                        "Your booking for event "
                                + eventId
                                + " and seat "
                                + seatNumber
                                + " is confirmed."
                )
        );

        return booking;
    }

    public boolean cancelBooking(
            long bookingId,
            long userId) throws SQLException {

        return bookingRepository.cancelBooking(
                bookingId,
                userId);
    }
}