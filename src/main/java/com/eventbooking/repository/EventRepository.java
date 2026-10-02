package com.eventbooking.repository;

import com.eventbooking.database.DatabaseConnection;
import com.eventbooking.model.Event;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class EventRepository {

    public List<Event> findAll() {

        List<Event> events = new ArrayList<>();

        String sql = """
                SELECT id,
                       organizer_id,
                       title,
                       description,
                       venue,
                       start_time,
                       end_time,
                       total_seats,
                       available_seats,
                       status
                FROM events
                ORDER BY start_time
                """;

        try (
                Connection connection =
                        DatabaseConnection.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql);

                ResultSet resultSet =
                        statement.executeQuery()
        ) {

            while (resultSet.next()) {

                Event event = new Event(
                        resultSet.getLong("id"),
                        resultSet.getLong("organizer_id"),
                        resultSet.getString("title"),
                        resultSet.getString("description"),
                        resultSet.getString("venue"),
                        resultSet.getString("start_time"),
                        resultSet.getString("end_time"),
                        resultSet.getInt("total_seats"),
                        resultSet.getInt("available_seats"),
                        resultSet.getString("status")
                );

                events.add(event);
            }

        } catch (Exception e) {
            System.out.println("DATABASE ERROR:");
            e.printStackTrace();

            throw new RuntimeException(
                    "Failed to fetch events",
                    e
            );
        }

        

        return events;
    }
}