package com.eventbooking.model;

public class User {

    private long id;
    private String name;
    private String email;
    private String passwordHash;
    private String role;

    public User() {
    }

    public User(
            long id,
            String name,
            String email,
            String passwordHash,
            String role
    ) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getRole() {
        return role;
    }
}