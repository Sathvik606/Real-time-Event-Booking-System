package com.eventbooking;

import com.eventbooking.handler.AuthHandler;
import com.eventbooking.handler.BookingHandler;
import com.eventbooking.handler.EventHandler;
import com.eventbooking.worker.NotificationWorker;
import com.sun.net.httpserver.HttpServer;

import java.net.InetSocketAddress;
import java.util.concurrent.CountDownLatch;

public class Application {

    public static void main(String[] args)
            throws Exception {

        HttpServer server = HttpServer.create(
                new InetSocketAddress("localhost", 8080),
                0);

        AuthHandler authHandler = new AuthHandler();

        EventHandler eventHandler = new EventHandler();

        BookingHandler bookingHandler = new BookingHandler();

        server.createContext(
                "/api/auth/register",
                authHandler::register);

        server.createContext(
                "/api/auth/login",
                authHandler::login);

        server.createContext(
                "/api/events",
                eventHandler::getAllEvents);

        server.createContext(
                "/api/bookings",
                bookingHandler::handle);

        Thread notificationWorker = new Thread(new NotificationWorker());

        notificationWorker.setName("notification-worker");
        notificationWorker.start();

        server.start();

        System.out.println(
                "======================================");

        System.out.println(
                "Server running at http://localhost:8080");

        System.out.println(
                "======================================");

        // Keep the application alive.
        CountDownLatch latch = new CountDownLatch(1);

        latch.await();
    }
}