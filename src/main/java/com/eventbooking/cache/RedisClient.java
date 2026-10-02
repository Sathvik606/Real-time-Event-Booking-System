package com.eventbooking.cache;

import redis.clients.jedis.Jedis;

public class RedisClient {

    private static final String HOST = "172.19.96.219";
    private static final int PORT = 6379;
    private static final String PASSWORD = System.getenv("REDIS_PASSWORD");

    public static Jedis getConnection() {

        Jedis jedis = new Jedis(HOST, PORT);

        jedis.auth(PASSWORD);

        return jedis;
    }
}