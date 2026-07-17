package com.qm.common;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@Tag(name = "健康检查")
@RestController
public class HealthController {

    @Autowired(required = false)
    private JdbcTemplate jdbcTemplate;

    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    @Operation(summary = "健康检查")
    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> result = new HashMap<>();
        result.put("status", "UP");
        result.put("timestamp", System.currentTimeMillis());

        // DB 检查
        try {
            if (jdbcTemplate != null) {
                jdbcTemplate.queryForObject("SELECT 1", Integer.class);
                result.put("db", "UP");
            }
        } catch (Exception e) {
            result.put("db", "DOWN: " + e.getMessage());
        }

        // Redis 检查
        try {
            if (redisTemplate != null) {
                redisTemplate.opsForValue().set("health:check", "ok", 10);
                result.put("redis", "UP");
            }
        } catch (Exception e) {
            result.put("redis", "DOWN: " + e.getMessage());
        }

        return result;
    }
}
