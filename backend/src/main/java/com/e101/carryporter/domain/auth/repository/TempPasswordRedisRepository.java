package com.e101.carryporter.domain.auth.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Repository
@Slf4j // 1. 로깅을 위한 어노테이션 추가
@RequiredArgsConstructor
public class TempPasswordRedisRepository {

    // 팀원 스타일: 상수 정의
    private static final String TEMP_PWD_PREFIX = "auth:temp:pwd:";
    private static final long TTL_MINUTES = 5;

    private final RedisTemplate<String, Object> redisTemplate;

    // 임시 비밀번호 저장 (TTL: 5분)
    public void save(String email, Integer password) {
        try {
            // 2. 키 생성 메서드 활용 & try-catch로 감싸기
            redisTemplate.opsForValue().set(getTempPasswordKey(email), password.toString(), TTL_MINUTES, TimeUnit.MINUTES);
            log.debug("임시 비밀번호 저장 email = {}", email);
        } catch (Exception e) {
            log.error("임시 비밀번호 저장 실패 : email = {}", email, e);
            throw e; // 에러를 상위로 던져서 서비스가 알게 함
        }
    }

    // 임시 비밀번호 조회
    public Optional<Integer> get(String email) {
        try {
            Object value = redisTemplate.opsForValue().get(getTempPasswordKey(email));

            // 3. Null 체크 및 명시적 형변환 (팀원 코드 핵심 로직)
            if (value == null) {
                return Optional.empty();
            }

            // Redis에서 가져온 Object를 String으로 바꾼 뒤 Integer로 파싱 (안전함)
            return Optional.of(Integer.valueOf(value.toString()));
        } catch (Exception e) {
            log.error("임시 비밀번호 조회 실패 : email = {}", email, e);
            throw e;
        }
    }

    // 임시 비밀번호 삭제
    public void delete(String email) {
        try {
            redisTemplate.delete(getTempPasswordKey(email));
            log.debug("임시 비밀번호 삭제 email = {}", email);
        } catch (Exception e) {
            // 삭제 실패는 비즈니스 로직에 치명적이지 않으므로 로그만 남김 (팀원 스타일)
            log.error("임시 비밀번호 삭제 실패 : email = {}", email, e);
        }
    }

    // 4. 키 생성 로직 분리 (Private Helper Method)
    private String getTempPasswordKey(String email) {
        return TEMP_PWD_PREFIX + email;
    }
}