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
public class LoginFailCountRedisRepository {

    private static final String LOGIN_FAIL_PREFIX = "auth:fail:count:";
    private static final long TTL_HOURS = 1;

    private final RedisTemplate<String, Object> redisTemplate;

    // 로그인 실패 횟수 증가 메서드 (TTL 1시간)
    public Long increment(Long userId) {
        String key = getLoginFailKey(userId);

        try {
            Long count = redisTemplate.opsForValue().increment(key);

            // 처음 실패(1)일 때만 만료 시간 설정
            if (count != null && count == 1) {
                redisTemplate.expire(key, TTL_HOURS, TimeUnit.HOURS);
            }

            log.debug("로그인 실패 횟수 증가 userId = {}, count = {}", userId, count);

            return count;

        } catch (Exception e) {
            log.error("사용자 인증 실패 카운트 증가 실패: userId = {}", userId, e);
            // Redis 죽었을 때 로직을 멈출 순 없으니, 0을 리턴해서 "실패 횟수 없음" 취급하고 통과시킴
            return 0L;
        }
    }

    // 로그인 실패 횟수 조회 메서드
    public Optional<Integer> get(Long userId) {
        try {
            Object value = redisTemplate.opsForValue().get(getLoginFailKey(userId));

            if (value == null) {
                return Optional.empty();
            }

            return Optional.of(Integer.valueOf(value.toString()));
        } catch (Exception e) {
            log.error("실패 횟수 파싱 에러 : userId = {}", userId, e);
            return Optional.empty();
        }
    }

    // 로그인 실패 횟수 초기화 메서드
    public void reset(Long userId) {
        try {
            redisTemplate.delete(getLoginFailKey(userId));
            log.debug("로그인 실패 횟수 초기화 userId = {}", userId);
        } catch (Exception e) {
            log.error("로그인 실패 횟수 초기화 실패 : userId = {}", userId, e);
        }
    }

    private String getLoginFailKey(Long userId) {
        return LOGIN_FAIL_PREFIX + userId;
    }
}
