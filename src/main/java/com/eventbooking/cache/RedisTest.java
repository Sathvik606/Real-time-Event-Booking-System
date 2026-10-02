package com.eventbooking.cache;

import redis.clients.jedis.Jedis;

public class RedisTest {

    public static void main(String[] args) {

        try (Jedis redis = RedisClient.getConnection()) {

            System.out.println("Redis response: " + redis.ping());

            redis.set("test:key", "Hello Redis");

            System.out.println(
                    "Stored value: " + redis.get("test:key")
            );
        }
    }
}