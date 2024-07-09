package com.bite.springdataredis;

import com.bite.springdataredis.pojo.User;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Iterator;
import java.util.Map;

@SpringBootTest
class SpringDataRedisStringTest {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;  //使用 Spring 提供的 StringRedisTemplate

    private static final ObjectMapper mapper = new ObjectMapper();
    @Test
    void testSave() throws JsonProcessingException {
        //创建对象
        User user = new User("虎哥",21);
        //存入之前进行手动序列化  其实就是把 Java 对象转成 Json 字符串的形式3
        String json = mapper.writeValueAsString(user);
        //正式写入数据
        stringRedisTemplate.opsForValue().set("user:400",json);
        //获取数据
        String jsonUser = stringRedisTemplate.opsForValue().get("user:400");
        //读取的时候进行手动反序列化
        User user1 = mapper.readValue(jsonUser, User.class);

        System.out.println("user1 = " + user1);
    }

    @Test
    void testString() {
        //写入
        stringRedisTemplate.opsForValue().set("name","虎哥");

        stringRedisTemplate.opsForValue().set("age","18");

        //读取
        Object name = stringRedisTemplate.opsForValue().get("name");
        Object age = stringRedisTemplate.opsForValue().get("age");
        System.out.println("name = " + name);
        System.out.println("age = " + age);
    }

    @Test
    void testHash() {
        //写入
        stringRedisTemplate.opsForHash().put("user:500","name","小曹");
        stringRedisTemplate.opsForHash().put("user:500","age","18");
        Map<Object, Object> entries = stringRedisTemplate.opsForHash().entries("user:500");
        Iterator<Map.Entry<Object, Object>> mapIterator = entries.entrySet().iterator();
        while (mapIterator.hasNext()) {
            Map.Entry<Object, Object> strMap =mapIterator.next();
            Object key = strMap.getKey();
            Object value = strMap.getValue();
            System.out.println("key = " + key + "value = " + value);
        }
    }

}
