package com.eventbooking.repository;

import com.eventbooking.database.DatabaseConnection;
import com.eventbooking.model.User;

import java.sql.*;

public class UserRepository {

    public User findByEmail(String email)
            throws SQLException {

        String sql = """
                SELECT id,
                       name,
                       email,
                       password_hash,
                       role
                FROM users
                WHERE email = ?
                """;

        try (
                Connection connection =
                        DatabaseConnection.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            statement.setString(1, email);

            try (
                    ResultSet resultSet =
                            statement.executeQuery()
            ) {

                if (resultSet.next()) {

                    return new User(
                            resultSet.getLong("id"),
                            resultSet.getString("name"),
                            resultSet.getString("email"),
                            resultSet.getString("password_hash"),
                            resultSet.getString("role")
                    );
                }
            }
        }

        return null;
    }

    public User create(
            String name,
            String email,
            String passwordHash,
            String role
    ) throws SQLException {

        String sql = """
                INSERT INTO users
                (name, email, password_hash, role)
                VALUES (?, ?, ?, ?)
                """;

        try (
                Connection connection =
                        DatabaseConnection.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(
                                sql,
                                Statement.RETURN_GENERATED_KEYS
                        )
        ) {

            statement.setString(1, name);
            statement.setString(2, email);
            statement.setString(3, passwordHash);
            statement.setString(4, role);

            statement.executeUpdate();

            try (
                    ResultSet keys =
                            statement.getGeneratedKeys()
            ) {

                keys.next();

                long id = keys.getLong(1);

                return new User(
                        id,
                        name,
                        email,
                        passwordHash,
                        role
                );
            }
        }
    }
}