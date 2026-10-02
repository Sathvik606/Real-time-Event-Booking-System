package com.eventbooking.service;

import com.eventbooking.cache.RedisClient;
import com.eventbooking.model.Event;
import com.eventbooking.repository.EventRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import redis.clients.jedis.Jedis;

import java.util.List;

public class EventService {

    private final EventRepository eventRepository;
    private final ObjectMapper objectMapper;

    public EventService() {
        this.eventRepository = new EventRepository();
        this.objectMapper = new ObjectMapper();
    }

    public List<Event> getAllEvents() throws Exception {

        try (Jedis redis = RedisClient.getConnection()) {

            // 1. Check Redis first
            String cachedEvents = redis.get("events:all");

            if (cachedEvents != null) {

                System.out.println("REDIS CACHE HIT");

                return objectMapper.readValue(
                        cachedEvents,
                        new TypeReference<List<Event>>() {
                        });
            }

            // 2. Cache miss → MySQL
            System.out.println("REDIS CACHE MISS");

            List<Event> events = eventRepository.findAll();

            // 3. Store result in Redis
            String json = objectMapper.writeValueAsString(events);

            redis.setex(
                    "events:all",
                    60,
                    json);

            return events;
        }
    }
}