package com.blog.service.auth;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class TokenBlacklistService {
    private final StringRedisTemplate redis;

    public TokenBlacklistService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    private String key(Long uid, String jti) {
        return "TOKEN_BLACK_LIST_%d_%s".formatted(uid, jti);
    }

    public void blacklist(Long uid, String jti, Duration ttl) {
        String k = key(uid, jti);
        if (ttl != null && !ttl.isNegative() && !ttl.isZero()) {
            redis.opsForValue().set(k, "1", ttl);
        } else {
            redis.opsForValue().set(k, "1");
        }
    }

    public boolean isBlacklisted(Long uid, String jti) {
        String v = redis.opsForValue().get(key(uid, jti));
        return v != null;
    }
}
