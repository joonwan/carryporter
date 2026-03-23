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
public class UserPasswordRedisRepository {

    private static final String USER_PWD_PREFIX = "user:password:";
    private static final long TTL_HOURS = 24;

    private final RedisTemplate<String, Object> redisTemplate;

    // 사용자 4자리 비밀번호 저장 (TTL: 24 시간)
    public void save(Long userId, Integer password) {
        try {
            redisTemplate.opsForValue().set(getUserPasswordKey(userId), password.toString(), TTL_HOURS, TimeUnit.HOURS);
            log.debug("사용자 4자리 비밀번호 저장 userId = {}", userId);
        } catch (Exception e) {
            log.error("사용자 4자리 비밀번호 저장 실패 userId = {}", userId, e);
            throw e;
        }
    }

    // 사용자 4자리 비밀번호 조회
    public Optional<Integer> get(Long userId) {
        try {
            Object value = redisTemplate.opsForValue().get(getUserPasswordKey(userId));

            if (value == null) {
                return Optional.empty();
            }

            return Optional.of(Integer.valueOf(value.toString()));
        } catch (Exception e) {
            log.error("4자리 비밀번호 조회 실패 : userId = {}", userId, e);
            throw e;
        }
    }

    // 사용자 4자리 비밀번호 삭제
    public void delete(Long userId) {
        try {
            redisTemplate.delete(getUserPasswordKey(userId));
            log.debug("4자리 비밀번호 삭제 userId = {}", userId);
        } catch (Exception e) {
            log.error("비밀번호 삭제 실패 : userId = {}", userId, e);
        }
    }

    private String getUserPasswordKey(Long userId) {
        return USER_PWD_PREFIX + userId;
    }
}
