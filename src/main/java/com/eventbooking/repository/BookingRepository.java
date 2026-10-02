package com.eventbooking.repository;

import com.eventbooking.database.DatabaseConnection;
import com.eventbooking.exception.BookingConflictException;
import com.eventbooking.model.Booking;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class BookingRepository {

    public Booking createBooking(
            long userId,
            long eventId,
            int seatNumber,
            String idempotencyKey) throws Exception {

        Connection connection = null;

        try {
            connection = DatabaseConnection.getConnection();

            // Start transaction
            connection.setAutoCommit(false);

            // 1. Atomically reserve a seat
            String updateEventSql = """
                    UPDATE events
                    SET available_seats = available_seats - 1
                    WHERE id = ?
                    AND available_seats > 0
                    """;

            try (PreparedStatement statement = connection.prepareStatement(
                    updateEventSql)) {

                statement.setLong(1, eventId);

                int rowsUpdated = statement.executeUpdate();

                if (rowsUpdated == 0) {
                    connection.rollback();
                    throw new BookingConflictException(
                            "No seats available for this event");
                }
            }

            // 2. Create booking
            String insertBookingSql = """
                    INSERT INTO bookings
                    (
                        user_id,
                        event_id,
                        seat_number,
                        status,
                        idempotency_key
                    )
                    VALUES (?, ?, ?, 'CONFIRMED', ?)
                    """;

            long bookingId;

            try (PreparedStatement statement = connection.prepareStatement(
                    insertBookingSql,
                    Statement.RETURN_GENERATED_KEYS)) {

                statement.setLong(1, userId);
                statement.setLong(2, eventId);
                statement.setInt(3, seatNumber);
                statement.setString(4, idempotencyKey);

                statement.executeUpdate();

                try (ResultSet keys = statement.getGeneratedKeys()) {

                    keys.next();
                    bookingId = keys.getLong(1);
                }
            }

            // 3. Commit everything
            connection.commit();

            return new Booking(
                    bookingId,
                    userId,
                    eventId,
                    seatNumber,
                    "CONFIRMED",
                    idempotencyKey);

        } catch (Exception e) {

            if (connection != null) {
                connection.rollback();
            }

            throw e;

        } finally {

            if (connection != null) {
                connection.setAutoCommit(true);
                connection.close();
            }
        }
    }

    public Booking findByIdempotencyKey(
            String idempotencyKey) throws Exception {

        String sql = """
                SELECT
                    id,
                    user_id,
                    event_id,
                    seat_number,
                    status,
                    idempotency_key
                FROM bookings
                WHERE idempotency_key = ?
                """;

        try (
                Connection connection = DatabaseConnection.getConnection();

                PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, idempotencyKey);

            try (ResultSet resultSet = statement.executeQuery()) {

                if (resultSet.next()) {

                    return new Booking(
                            resultSet.getLong("id"),
                            resultSet.getLong("user_id"),
                            resultSet.getLong("event_id"),
                            resultSet.getInt("seat_number"),
                            resultSet.getString("status"),
                            resultSet.getString(
                                    "idempotency_key"));
                }
            }
        }

        return null;
    }

    public boolean cancelBooking(long bookingId, long userId) throws SQLException {

        Connection connection = null;

        try {
            connection = DatabaseConnection.getConnection();
            connection.setAutoCommit(false);

            // 1. Mark booking as cancelled
            String updateBooking = "UPDATE bookings " +
                    "SET status = 'CANCELLED', cancelled_at = CURRENT_TIMESTAMP " +
                    "WHERE id = ? " +
                    "AND user_id = ? " +
                    "AND status = 'CONFIRMED'";

            int bookingUpdated;

            try (PreparedStatement statement = connection.prepareStatement(updateBooking)) {

                statement.setLong(1, bookingId);
                statement.setLong(2, userId);

                bookingUpdated = statement.executeUpdate();
            }

            if (bookingUpdated == 0) {
                connection.rollback();
                return false;
            }

            // 2. Increase available seats
            String updateEvent = "UPDATE events " +
                    "SET available_seats = available_seats + 1 " +
                    "WHERE id = (" +
                    "    SELECT event_id FROM bookings WHERE id = ?" +
                    ")";

            try (PreparedStatement statement = connection.prepareStatement(updateEvent)) {

                statement.setLong(1, bookingId);
                statement.executeUpdate();
            }

            // 3. Commit both operations
            connection.commit();

            return true;

        } catch (SQLException e) {

            if (connection != null) {
                connection.rollback();
            }

            throw e;

        } finally {

            if (connection != null) {
                connection.setAutoCommit(true);
                connection.close();
            }
        }
    }
}
