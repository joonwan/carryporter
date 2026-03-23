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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class LoginFailCountRedisRepositoryTest extends IntegrationTestSupport {

    @Autowired
    LoginFailCountRedisRepository loginFailCountRedisRepository;

    @Autowired
    RedisTemplate<String, Object> redisTemplate;

    @AfterEach
    void tearDown() {
        Optional.ofNullable(redisTemplate.getConnectionFactory())
                .map(RedisConnectionFactory::getConnection)
                .ifPresent(conn -> conn.serverCommands().flushDb());
    }

    @Nested
    @DisplayName("로그인 실패 횟수 증가")
    class IncrementTest {

        @DisplayName("로그인 실패 시 카운트가 증가한다")
        @Test
        void increment() {
            // given
            Long userId = 1L;

            // when
            loginFailCountRedisRepository.increment(userId);
            Optional<Integer> result = loginFailCountRedisRepository.get(userId);

            // then
            assertThat(result).isPresent();
            assertThat(result.get()).isEqualTo(1);
        }

        @DisplayName("로그인 실패를 여러 번 하면 카운트가 누적된다")
        @Test
        void incrementMultipleTimes() {
            // given
            Long userId = 1L;

            // when
            loginFailCountRedisRepository.increment(userId);
            loginFailCountRedisRepository.increment(userId);
            loginFailCountRedisRepository.increment(userId);
            Optional<Integer> result = loginFailCountRedisRepository.get(userId);

            // then
            assertThat(result).isPresent();
            assertThat(result.get()).isEqualTo(3);
        }

        @DisplayName("여러 사용자의 실패 횟수를 독립적으로 관리한다")
        @Test
        void multipleUsers() {
            // given
            Long user1 = 1L;
            Long user2 = 2L;
            Long user3 = 3L;

            // when
            loginFailCountRedisRepository.increment(user1);
            loginFailCountRedisRepository.increment(user1);

            loginFailCountRedisRepository.increment(user2);
            loginFailCountRedisRepository.increment(user2);
            loginFailCountRedisRepository.increment(user2);
            loginFailCountRedisRepository.increment(user2);

            loginFailCountRedisRepository.increment(user3);

            // then
            assertThat(loginFailCountRedisRepository.get(user1)).contains(2);
            assertThat(loginFailCountRedisRepository.get(user2)).contains(4);
            assertThat(loginFailCountRedisRepository.get(user3)).contains(1);
        }
    }

    @Nested
    @DisplayName("로그인 실패 횟수 조회")
    class GetTest {

        @DisplayName("로그인 실패 횟수를 조회할 수 있다")
        @Test
        void get() {
            // given
            Long userId = 1L;
            loginFailCountRedisRepository.increment(userId);
            loginFailCountRedisRepository.increment(userId);

            // when
            Optional<Integer> result = loginFailCountRedisRepository.get(userId);

            // then
            assertThat(result).isPresent();
            assertThat(result.get()).isEqualTo(2);
        }

        @DisplayName("실패 이력이 없으면 빈 Optional을 반환한다")
        @Test
        void getNotFound() {
            // given
            Long userId = 999L;

            // when
            Optional<Integer> result = loginFailCountRedisRepository.get(userId);

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("로그인 실패 횟수 초기화")
    class ResetTest {

        @DisplayName("로그인 실패 횟수를 초기화할 수 있다")
        @Test
        void reset() {
            // given
            Long userId = 1L;
            loginFailCountRedisRepository.increment(userId);
            loginFailCountRedisRepository.increment(userId);
            loginFailCountRedisRepository.increment(userId);

            // when
            loginFailCountRedisRepository.reset(userId);
            Optional<Integer> result = loginFailCountRedisRepository.get(userId);

            // then
            assertThat(result).isEmpty();
        }

        @DisplayName("초기화 후 다시 실패 횟수를 카운트할 수 있다")
        @Test
        void resetAndIncrement() {
            // given
            Long userId = 1L;
            loginFailCountRedisRepository.increment(userId);
            loginFailCountRedisRepository.increment(userId);
            loginFailCountRedisRepository.reset(userId);

            // when
            loginFailCountRedisRepository.increment(userId);
            Optional<Integer> result = loginFailCountRedisRepository.get(userId);

            // then
            assertThat(result).contains(1);
        }

        @DisplayName("존재하지 않는 실패 횟수를 초기화해도 예외가 발생하지 않는다")
        @Test
        void resetNotFound() {
            // given
            Long notExistUserId = 999L;

            // when & then (예외 없이 정상 실행)
            loginFailCountRedisRepository.reset(notExistUserId);
        }
    }

    @Nested
    @DisplayName("TTL 검증")
    class TtlTest {

        @DisplayName("첫 실패 시 1시간 TTL이 설정된다")
        @Test
        void ttlSetOnFirstFailure() {
            // given
            Long userId = 1L;

            // when
            loginFailCountRedisRepository.increment(userId);
            Long ttl = redisTemplate.getExpire("auth:fail:count:" + userId, TimeUnit.MINUTES);

            // then
            assertThat(ttl).isNotNull();
            assertThat(ttl).isGreaterThan(55L); // 1시간(60분)에 가까운 값
            assertThat(ttl).isLessThanOrEqualTo(60L);
        }

        @DisplayName("두 번째 이후 실패 시에는 TTL이 재설정되지 않는다")
        @Test
        void ttlNotResetAfterFirstFailure() throws InterruptedException {
            // given
            Long userId = 1L;

            // when - 첫 번째 실패
            loginFailCountRedisRepository.increment(userId);
            Long firstTtl = redisTemplate.getExpire("auth:fail:count:" + userId, TimeUnit.SECONDS);

            Thread.sleep(2000); // 2초 대기

            // when - 두 번째 실패
            loginFailCountRedisRepository.increment(userId);
            Long secondTtl = redisTemplate.getExpire("auth:fail:count:" + userId, TimeUnit.SECONDS);

            // then - TTL이 재설정되지 않았으므로 감소했을 것
            assertThat(secondTtl).isLessThan(firstTtl);
        }

        @DisplayName("TTL이 만료되면 실패 횟수가 자동으로 삭제된다")
        @Test
        void ttlExpiration() throws InterruptedException {
            // given
            Long userId = 1L;

            // 테스트를 위해 짧은 TTL로 직접 설정
            redisTemplate.opsForValue().set("auth:fail:count:" + userId, "3", 1, TimeUnit.SECONDS);

            // when
            Thread.sleep(1100); // 1.1초 대기
            Optional<Integer> result = loginFailCountRedisRepository.get(userId);

            // then
            assertThat(result).isEmpty();
        }
    }
}
