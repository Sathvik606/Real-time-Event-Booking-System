package com.eventbooking.service;

import com.eventbooking.model.User;
import com.eventbooking.repository.UserRepository;
import com.eventbooking.security.JwtUtil;
import com.eventbooking.security.PasswordUtil;

public class AuthService {

    private final UserRepository userRepository;

    public AuthService() {
        this.userRepository =
                new UserRepository();
    }

    public User register(
            String name,
            String email,
            String password
    ) throws Exception {

        User existingUser =
                userRepository.findByEmail(email);

        if (existingUser != null) {
            throw new RuntimeException(
                    "Email already registered"
            );
        }

        String passwordHash =
                PasswordUtil.hashPassword(password);

        return userRepository.create(
                name,
                email,
                passwordHash,
                "USER"
        );
    }

    public String login(
            String email,
            String password
    ) throws Exception {

        User user =
                userRepository.findByEmail(email);

        if (user == null) {
            throw new RuntimeException(
                    "Invalid email or password"
            );
        }

        boolean valid =
                PasswordUtil.verifyPassword(
                        password,
                        user.getPasswordHash()
                );

        if (!valid) {
            throw new RuntimeException(
                    "Invalid email or password"
            );
        }

        return JwtUtil.generateToken(
                user.getId(),
                user.getRole()
        );
    }
}