package com.bite.Jedis.util;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class JedisConnectionFactory {
    private static final JedisPool jedisPool;  //在类加载时已经被默认初始化过了 值为 null

    static {
        //配置连接池
        JedisPoolConfig config = new JedisPoolConfig();
        config.setMaxIdle(8);
        config.setMaxTotal(8);
        config.setMaxIdle(0);
        config.setMaxWaitMillis(1000);

        jedisPool = new JedisPool(config, "127.0.0.1", 6379,1000,"tenny");
    }

    public static Jedis getJedis() {
        return jedisPool.getResource();
    }
}
