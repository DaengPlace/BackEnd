package com.mycom.backenddaengplace.auth.email;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class RedisEmailAuthentication {

    private final StringRedisTemplate redisTemplate;

    public String checkEmailAuthentication(String key) {
        HashOperations<String, String, String> hashOperations = redisTemplate.opsForHash();
        return hashOperations.get(key, "auth");
    }

    public String getEmailAuthenticationCode(String key) {
        HashOperations<String, String, String> hashOperations = redisTemplate.opsForHash();
        return hashOperations.get(key, "code");
    }

    public void setEmailAuthenticationExpire(String email, String code, long duration) {
        HashOperations<String, String, String> hashOperations = redisTemplate.opsForHash();
        hashOperations.put(email, "code", code);
        hashOperations.put(email, "auth", "N");
        redisTemplate.expire(email, Duration.ofMinutes(duration));
    }

    public void setEmailAuthenticationComplete(String email) {
        HashOperations<String, String, String> hashOperations = redisTemplate.opsForHash();
        hashOperations.put(email, "auth", "Y");
    }

    public void deleteEmailAuthenticationHistory(String key) {
        HashOperations<String, String, String> hashOperations = redisTemplate.opsForHash();
        hashOperations.delete(key, "code");
        hashOperations.delete(key, "auth");
    }
}
