package com.e101.carryporter.domain.auth.repository;

import com.e101.carryporter.support.IntegrationTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class RefreshTokenRedisRepositoryTest extends IntegrationTestSupport {

    @Autowired
    RefreshTokenRedisRepository refreshTokenRedisRepository;

    @Autowired
    RedisTemplate<String, Object> redisTemplate;

    @AfterEach
    void tearDown() {
        Optional.ofNullable(redisTemplate.getConnectionFactory())
                .map(RedisConnectionFactory::getConnection)
                .ifPresent(conn -> conn.serverCommands().flushDb());
    }

    @Nested
    @DisplayName("Refresh Token 저장 및 조회")
    class SaveAndGetTest {

        @DisplayName("Refresh Token을 저장하고 조회할 수 있다")
        @Test
        void saveAndGet() {
            // given
            Long userId = 1L;
            String refreshToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.test";

            // when
            refreshTokenRedisRepository.save(userId, refreshToken);
            Optional<String> result = refreshTokenRedisRepository.get(userId);

            // then
            assertThat(result).isPresent();
            assertThat(result.get()).isEqualTo(refreshToken);
        }

        @DisplayName("같은 사용자 ID에 Refresh Token을 여러 번 저장하면 마지막 값으로 덮어쓴다")
        @Test
        void saveOverwrite() {
            // given
            Long userId = 1L;
            String firstToken = "first.refresh.token";
            String secondToken = "second.refresh.token";

            // when
            refreshTokenRedisRepository.save(userId, firstToken);
            refreshTokenRedisRepository.save(userId, secondToken);
            Optional<String> result = refreshTokenRedisRepository.get(userId);

            // then
            assertThat(result).isPresent();
            assertThat(result.get()).isEqualTo(secondToken);
        }

        @DisplayName("존재하지 않는 사용자 ID로 조회하면 빈 Optional을 반환한다")
        @Test
        void getNotFound() {
            // given
            Long notExistUserId = 999L;

            // when
            Optional<String> result = refreshTokenRedisRepository.get(notExistUserId);

            // then
            assertThat(result).isEmpty();
        }

        @DisplayName("여러 사용자의 Refresh Token을 독립적으로 저장하고 조회할 수 있다")
        @Test
        void multipleUsers() {
            // given
            Long userId1 = 1L;
            Long userId2 = 2L;
            Long userId3 = 3L;
            String token1 = "token.for.user1";
            String token2 = "token.for.user2";
            String token3 = "token.for.user3";

            // when
            refreshTokenRedisRepository.save(userId1, token1);
            refreshTokenRedisRepository.save(userId2, token2);
            refreshTokenRedisRepository.save(userId3, token3);

            // then
            assertThat(refreshTokenRedisRepository.get(userId1)).contains(token1);
            assertThat(refreshTokenRedisRepository.get(userId2)).contains(token2);
            assertThat(refreshTokenRedisRepository.get(userId3)).contains(token3);
        }

    }

    @Nested
    @DisplayName("Refresh Token 삭제")
    class DeleteTest {

        @DisplayName("저장된 Refresh Token을 삭제할 수 있다")
        @Test
        void delete() {
            // given
            Long userId = 1L;
            String refreshToken = "test.refresh.token";
            refreshTokenRedisRepository.save(userId, refreshToken);

            // when
            refreshTokenRedisRepository.delete(userId);
            Optional<String> result = refreshTokenRedisRepository.get(userId);

            // then
            assertThat(result).isEmpty();
        }

        @DisplayName("존재하지 않는 Refresh Token을 삭제해도 예외가 발생하지 않는다")
        @Test
        void deleteNotFound() {
            // given
            Long notExistUserId = 999L;

            // when & then (예외 없이 정상 실행)
            refreshTokenRedisRepository.delete(notExistUserId);
        }

        @DisplayName("삭제 후 다시 저장할 수 있다")
        @Test
        void deleteAndSaveAgain() {
            // given
            Long userId = 1L;
            String firstToken = "first.token";
            String secondToken = "second.token";

            refreshTokenRedisRepository.save(userId, firstToken);
            refreshTokenRedisRepository.delete(userId);

            // when
            refreshTokenRedisRepository.save(userId, secondToken);
            Optional<String> result = refreshTokenRedisRepository.get(userId);

            // then
            assertThat(result).isPresent();
            assertThat(result.get()).isEqualTo(secondToken);
        }
    }

    @Nested
    @DisplayName("TTL 검증")
    class TtlTest {

        @DisplayName("저장된 Refresh Token은 7일 TTL을 가진다")
        @Test
        void ttlCheck() {
            // given
            Long userId = 1L;
            String refreshToken = "test.refresh.token";

            // when
            refreshTokenRedisRepository.save(userId, refreshToken);
            Long ttl = redisTemplate.getExpire("refresh:" + userId, TimeUnit.DAYS);

            // then
            assertThat(ttl).isNotNull();
            assertThat(ttl).isGreaterThanOrEqualTo(6L); // 7일에 가까운 값
            assertThat(ttl).isLessThanOrEqualTo(7L);
        }

        @DisplayName("TTL이 만료되면 Refresh Token이 자동으로 삭제된다")
        @Test
        void ttlExpiration() throws InterruptedException {
            // given
            Long userId = 1L;
            String refreshToken = "test.refresh.token";

            // 테스트를 위해 짧은 TTL로 직접 설정
            redisTemplate.opsForValue().set("refresh:" + userId, refreshToken, 1, TimeUnit.SECONDS);

            // when
            Thread.sleep(1100); // 1.1초 대기
            Optional<String> result = refreshTokenRedisRepository.get(userId);

            // then
            assertThat(result).isEmpty();
        }

        @DisplayName("Refresh Token을 재발급하면 TTL이 갱신된다")
        @Test
        void ttlRenewal() throws InterruptedException {
            // given
            Long userId = 1L;
            String firstToken = "first.token";
            String secondToken = "second.token";

            // 짧은 TTL로 첫 번째 토큰 저장
            redisTemplate.opsForValue().set("refresh:" + userId, firstToken, 2, TimeUnit.SECONDS);
            Thread.sleep(1000); // 1초 대기

            // when - 새 토큰으로 재발급
            refreshTokenRedisRepository.save(userId, secondToken);
            Long ttl = redisTemplate.getExpire("refresh:" + userId, TimeUnit.DAYS);

            // then - TTL이 새로 설정됨 (7일)
            assertThat(ttl).isNotNull();
            assertThat(ttl).isGreaterThanOrEqualTo(6L);
        }
    }
}
