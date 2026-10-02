package com.eventbooking.handler;

import com.eventbooking.model.Event;
import com.eventbooking.service.EventService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class EventHandler {

    private final EventService eventService;
    private final ObjectMapper objectMapper;

    public EventHandler() {
        this.eventService = new EventService();
        this.objectMapper = new ObjectMapper();
    }

    public void getAllEvents(HttpExchange exchange)
            throws IOException {

        try {

            List<Event> events =
                    eventService.getAllEvents();

            String json =
                    objectMapper.writeValueAsString(events);

            byte[] response =
                    json.getBytes(StandardCharsets.UTF_8);

            exchange.getResponseHeaders()
                    .set(
                            "Content-Type",
                            "application/json"
                    );

            exchange.sendResponseHeaders(
                    200,
                    response.length
            );

            exchange.getResponseBody()
                    .write(response);

        } catch (Exception e) {

            e.printStackTrace();

            String error =
                    "{\"error\":\"Internal server error\"}";

            byte[] response =
                    error.getBytes(StandardCharsets.UTF_8);

            exchange.getResponseHeaders()
                    .set(
                            "Content-Type",
                            "application/json"
                    );

            exchange.sendResponseHeaders(
                    500,
                    response.length
            );

            exchange.getResponseBody()
                    .write(response);

        } finally {
            exchange.close();
        }
    }
}