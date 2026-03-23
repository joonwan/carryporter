package com.e101.carryporter.domain.auth.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Repository
@Slf4j
@RequiredArgsConstructor
public class RefreshTokenRedisRepository {

    private static final String REFRESH_TOKEN_PREFIX = "refresh:";
    private static final long TTL_DAYS = 7;

    private final RedisTemplate<String, Object> redisTemplate;

    // refresh token 저장 메서드 (TTL : 7일)
    public void save(Long userId, String refreshToken) {
        try {
            redisTemplate.opsForValue().set(getRefreshTokenKey(userId), refreshToken, TTL_DAYS, TimeUnit.DAYS);
            log.debug("Refresh Token 저장 userId = {}", userId);
        } catch (Exception e) {
            log.error("refresh token 저장 실패 : userId = {}", userId, e);
            throw e;
        }
    }

    // refresh token 조회 메서드
    public Optional<String> get(Long userId) {
        try {
            Object value = redisTemplate.opsForValue().get(getRefreshTokenKey(userId));

            if (value == null) {
                return Optional.empty();
            }

            return Optional.of(value.toString());
        } catch (Exception e) {
            log.error("refresh token 조회 실패 : userId = {}", userId, e);
            throw e;
        }
    }

    // refresh token 삭제 메서드
    public void delete(Long userId) {
        try {
            redisTemplate.delete(getRefreshTokenKey(userId));
            log.debug("Refresh Token 삭제 userId = {}", userId);
        } catch (Exception e) {
            log.error("refresh token 삭제 실패 : userId = {}", userId, e);
        }
    }

    private String getRefreshTokenKey(Long userId) {
        return REFRESH_TOKEN_PREFIX + userId;
    }
}
