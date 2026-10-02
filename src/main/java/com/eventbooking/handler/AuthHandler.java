package com.eventbooking.handler;

import com.eventbooking.model.User;
import com.eventbooking.service.AuthService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class AuthHandler {

    private final AuthService authService;
    private final ObjectMapper objectMapper;

    public AuthHandler() {
        this.authService = new AuthService();
        this.objectMapper = new ObjectMapper();
    }

    public void register(
            HttpExchange exchange
    ) throws IOException {

        try {

            String body =
                    new String(
                            exchange.getRequestBody().readAllBytes(),
                            StandardCharsets.UTF_8
                    );

            JsonNode request =
                    objectMapper.readTree(body);

            String name =
                    request.get("name").asText();

            String email =
                    request.get("email").asText();

            String password =
                    request.get("password").asText();

            User user =
                    authService.register(
                            name,
                            email,
                            password
                    );

            String response =
                    objectMapper.writeValueAsString(
                            Map.of(
                                    "id", user.getId(),
                                    "name", user.getName(),
                                    "email", user.getEmail(),
                                    "role", user.getRole()
                            )
                    );

            sendResponse(
                    exchange,
                    201,
                    response
            );

        } catch (Exception e) {

            e.printStackTrace();

            sendResponse(
                    exchange,
                    400,
                    "{\"error\":\"" +
                    e.getMessage() +
                    "\"}"
            );

        } finally {
            exchange.close();
        }
    }

    public void login(
            HttpExchange exchange
    ) throws IOException {

        try {

            String body =
                    new String(
                            exchange.getRequestBody().readAllBytes(),
                            StandardCharsets.UTF_8
                    );

            JsonNode request =
                    objectMapper.readTree(body);

            String email =
                    request.get("email").asText();

            String password =
                    request.get("password").asText();

            String token =
                    authService.login(
                            email,
                            password
                    );

            String response =
                    objectMapper.writeValueAsString(
                            Map.of(
                                    "token", token
                            )
                    );

            sendResponse(
                    exchange,
                    200,
                    response
            );

        } catch (Exception e) {

            e.printStackTrace();

            sendResponse(
                    exchange,
                    401,
                    "{\"error\":\"Invalid email or password\"}"
            );

        } finally {
            exchange.close();
        }
    }

    private void sendResponse(
            HttpExchange exchange,
            int statusCode,
            String response
    ) throws IOException {

        byte[] bytes =
                response.getBytes(
                        StandardCharsets.UTF_8
                );

        exchange.getResponseHeaders()
                .set(
                        "Content-Type",
                        "application/json"
                );

        exchange.sendResponseHeaders(
                statusCode,
                bytes.length
        );

        exchange.getResponseBody()
                .write(bytes);
    }
}