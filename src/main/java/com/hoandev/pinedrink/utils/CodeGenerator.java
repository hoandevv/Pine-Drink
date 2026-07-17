package com.hoandev.pinedrink.utils;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Component
@RequiredArgsConstructor
public class CodeGenerator {

    private final StringRedisTemplate stringRedisTemplate;

    private static final String KEY_PATTERN = "code:seq:%s:%s";
    private static final String KEY_PATTERN_SCOPE = "code:seq:%s:%s:%s";
    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyyMMdd");

    public String generate(String prefix) {
        String date = LocalDate.now().format(DTF);
        String key = String.format(KEY_PATTERN, prefix, date);
        long seq = incrementAndExpire(key);
        return String.format("%s-%s-%04d", prefix, date, seq);
    }

    public String generate(String prefix, String scopeId) {
        String date = LocalDate.now().format(DTF);
        String key = String.format(KEY_PATTERN_SCOPE, prefix, scopeId, date);
        long seq = incrementAndExpire(key);
        return String.format("%s-%s-%04d", prefix, date, seq);
    }

    private long incrementAndExpire(String key) {
        Long seq = stringRedisTemplate.opsForValue().increment(key);
        stringRedisTemplate.expire(key, Duration.ofDays(3));
        return seq;
    }
}
