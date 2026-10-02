package com.eventbooking.handler;

import com.eventbooking.exception.BookingConflictException;
import com.eventbooking.model.Booking;
import com.eventbooking.service.BookingService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import com.eventbooking.security.AuthUtil;

public class BookingHandler {

    private final BookingService bookingService;
    private final ObjectMapper objectMapper;

    public BookingHandler() {
        this.bookingService = new BookingService();

        this.objectMapper = new ObjectMapper();
    }

    public void createBooking(
            HttpExchange exchange) throws IOException {

        try {

            // Get authenticated user from JWT
            long userId = AuthUtil.getUserId(exchange);

            String body = new String(
                    exchange.getRequestBody().readAllBytes(),
                    StandardCharsets.UTF_8);

            System.out.println("RECEIVED BODY = [" + body + "]");

            JsonNode request = objectMapper.readTree(body);

            System.out.println("eventId node = " + request.get("eventId"));
            System.out.println("seatNumber node = " + request.get("seatNumber"));
            System.out.println("idempotencyKey node = " + request.get("idempotencyKey"));

            long eventId = request.get("eventId").asLong();
            int seatNumber = request.get("seatNumber").asInt();
            String idempotencyKey = request.get("idempotencyKey").asText();

            Booking booking = bookingService.createBooking(
                    userId,
                    eventId,
                    seatNumber,
                    idempotencyKey);

            String response = objectMapper.writeValueAsString(
                    booking);

            byte[] responseBytes = response.getBytes(
                    StandardCharsets.UTF_8);

            exchange.getResponseHeaders()
                    .set(
                            "Content-Type",
                            "application/json");

            exchange.sendResponseHeaders(
                    201,
                    responseBytes.length);

            exchange.getResponseBody()
                    .write(responseBytes);

        } catch (BookingConflictException e) {

            sendResponse(
                    exchange,
                    409,
                    "{\"error\":\"" + e.getMessage() + "\"}");

        } catch (Exception e) {

            e.printStackTrace();

            String error = "{\"error\":\"" +
                    e.getMessage() +
                    "\"}";

            byte[] responseBytes = error.getBytes(
                    StandardCharsets.UTF_8);

            exchange.getResponseHeaders()
                    .set(
                            "Content-Type",
                            "application/json");

            exchange.sendResponseHeaders(
                    400,
                    responseBytes.length);

            exchange.getResponseBody()
                    .write(responseBytes);

        } finally {

            exchange.close();
        }
    }

    public void handle(HttpExchange exchange) throws IOException {

        try {

            String method = exchange.getRequestMethod();

            if ("POST".equalsIgnoreCase(method)) {
                createBooking(exchange);

            } else if ("DELETE".equalsIgnoreCase(method)) {
                cancelBooking(exchange);

            } else {
                sendResponse(
                        exchange,
                        405,
                        "{\"error\":\"Method not allowed\"}");
            }

        } catch (RuntimeException e) {

            sendResponse(
                    exchange,
                    401,
                    "{\"error\":\"Invalid or expired authentication token\"}");

        } finally {
            exchange.close();
        }
    }

    private void cancelBooking(HttpExchange exchange) throws IOException {

        try {
            long userId = AuthUtil.getUserId(exchange);

            String path = exchange.getRequestURI().getPath();

            // /api/bookings/5
            String[] parts = path.split("/");
            long bookingId = Long.parseLong(parts[3]);

            boolean cancelled = bookingService.cancelBooking(bookingId, userId);

            if (!cancelled) {
                sendResponse(
                        exchange,
                        404,
                        "{\"error\":\"Booking not found or already cancelled\"}");
                return;
            }

            sendResponse(
                    exchange,
                    200,
                    "{\"message\":\"Booking cancelled successfully\"}");

        } catch (Exception e) {

            e.printStackTrace();

            sendResponse(
                    exchange,
                    500,
                    "{\"error\":\"Internal server error\"}");
        }
    }

    private void sendResponse(
            HttpExchange exchange,
            int statusCode,
            String response) throws IOException {

        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders()
                .set("Content-Type", "application/json");

        exchange.sendResponseHeaders(statusCode, bytes.length);

        exchange.getResponseBody().write(bytes);
    }
}