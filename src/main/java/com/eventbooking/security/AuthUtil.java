package com.eventbooking.security;

import com.sun.net.httpserver.HttpExchange;

public class AuthUtil {

    public static String extractToken(HttpExchange exchange) {

        String header =
                exchange.getRequestHeaders().getFirst("Authorization");

        if (header == null || !header.startsWith("Bearer ")) {
            throw new RuntimeException("Missing or invalid Authorization header");
        }

        String token = header.substring(7).trim();

        if (token.isEmpty()) {
            throw new RuntimeException("Invalid JWT token");
        }

        return token;
    }

    public static long getUserId(HttpExchange exchange) {
        return JwtUtil.getUserId(extractToken(exchange));
    }

    public static String getRole(HttpExchange exchange) {
        return JwtUtil.getRole(extractToken(exchange));
    }
}