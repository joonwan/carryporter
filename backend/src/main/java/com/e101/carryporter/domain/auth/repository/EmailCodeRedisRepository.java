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
public class EmailCodeRedisRepository {

    private static final String EMAIL_CODE_PREFIX = "auth:email:code:";
    private static final long TTL_MINUTES = 5;

    private final RedisTemplate<String, Object> redisTemplate;

    // email 인증 번호 저장 메서드 (TTL: 5분)
    public void save(String email, Integer code) {
        try {
            redisTemplate.opsForValue().set(getEmailCodeKey(email), code.toString(), TTL_MINUTES, TimeUnit.MINUTES);
            log.debug("이메일 인증번호 저장 email = {}", email);
        } catch (Exception e) {
            log.error("인증번호 저장 실패 : email = {}", email, e);
            throw e;
        }
    }

    // email 인증번호 조회 메서드
    public Optional<Integer> get(String email) {
        try {
            Object value = redisTemplate.opsForValue().get(getEmailCodeKey(email));

            if (value == null) {
                return Optional.empty();
            }

            return Optional.of(Integer.valueOf(value.toString()));
        } catch (Exception e) {
            log.error("인증번호 조회 실패 : email = {}", email, e);
            throw e;
        }
    }

    // email 인증번호 삭제 메서드
    public void delete(String email) {
        try {
            redisTemplate.delete(getEmailCodeKey(email));
            log.debug("이메일 인증번호 삭제 email = {}", email);
        } catch (Exception e) {
            log.error("인증번호 삭제 실패 : email = {}", email, e);
        }
    }

    private String getEmailCodeKey(String email) {
        return EMAIL_CODE_PREFIX + email;
    }
    /**
     * [추가] 외부(Service)에서 만료 시간을 초 단위로 조회할 수 있게 해주는 메서드
     * 5분 -> 300초 변환해서 반환
     */
    public long getExpireSeconds() {
        return TimeUnit.MINUTES.toSeconds(TTL_MINUTES);
    }
}
