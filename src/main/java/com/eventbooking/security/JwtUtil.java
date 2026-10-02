package com.eventbooking.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

public class JwtUtil {

    private static final String SECRET =
            System.getenv("JWT_SECRET");

    private static final long EXPIRATION =
            1000 * 60 * 60; // 1 hour

    private static final SecretKey KEY =
            Keys.hmacShaKeyFor(
                    SECRET.getBytes(StandardCharsets.UTF_8)
            );

    public static String generateToken(
            long userId,
            String role
    ) {

        Date now = new Date();

        Date expiry =
                new Date(
                        now.getTime() + EXPIRATION
                );

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("role", role)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(KEY)
                .compact();
    }

    public static long getUserId(
            String token
    ) {

        String userId =
                Jwts.parser()
                        .verifyWith(KEY)
                        .build()
                        .parseSignedClaims(token)
                        .getPayload()
                        .getSubject();

        return Long.parseLong(userId);
    }

    public static String getRole(
            String token
    ) {

        return (String)
                Jwts.parser()
                        .verifyWith(KEY)
                        .build()
                        .parseSignedClaims(token)
                        .getPayload()
                        .get("role");
    }
}