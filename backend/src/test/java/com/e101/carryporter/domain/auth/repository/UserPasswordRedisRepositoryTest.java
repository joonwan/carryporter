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

class UserPasswordRedisRepositoryTest extends IntegrationTestSupport {

    @Autowired
    UserPasswordRedisRepository userPasswordRedisRepository;

    @Autowired
    RedisTemplate<String, Object> redisTemplate;

    @AfterEach
    void tearDown() {
        Optional.ofNullable(redisTemplate.getConnectionFactory())
                .map(RedisConnectionFactory::getConnection)
                .ifPresent(conn -> conn.serverCommands().flushDb());
    }

    @Nested
    @DisplayName("4자리 비밀번호 저장 및 조회")
    class SaveAndGetTest {

        @DisplayName("사용자 4자리 비밀번호를 저장하고 조회할 수 있다")
        @Test
        void saveAndGet() {
            // given
            Long userId = 1L;
            Integer password = 1234;

            // when
            userPasswordRedisRepository.save(userId, password);
            Optional<Integer> result = userPasswordRedisRepository.get(userId);

            // then
            assertThat(result).isPresent();
            assertThat(result.get()).isEqualTo(password);
        }

        @DisplayName("같은 사용자 ID에 비밀번호를 여러 번 저장하면 마지막 값으로 덮어쓴다")
        @Test
        void saveOverwrite() {
            // given
            Long userId = 1L;
            Integer firstPassword = 1111;
            Integer secondPassword = 9999;

            // when
            userPasswordRedisRepository.save(userId, firstPassword);
            userPasswordRedisRepository.save(userId, secondPassword);
            Optional<Integer> result = userPasswordRedisRepository.get(userId);

            // then
            assertThat(result).isPresent();
            assertThat(result.get()).isEqualTo(secondPassword);
        }

        @DisplayName("존재하지 않는 사용자 ID로 조회하면 빈 Optional을 반환한다")
        @Test
        void getNotFound() {
            // given
            Long notExistUserId = 999L;

            // when
            Optional<Integer> result = userPasswordRedisRepository.get(notExistUserId);

            // then
            assertThat(result).isEmpty();
        }

        @DisplayName("여러 사용자의 비밀번호를 독립적으로 저장하고 조회할 수 있다")
        @Test
        void multipleUsers() {
            // given
            Long userId1 = 1L;
            Long userId2 = 2L;
            Long userId3 = 3L;
            Integer password1 = 1234;
            Integer password2 = 5678;
            Integer password3 = 9012;

            // when
            userPasswordRedisRepository.save(userId1, password1);
            userPasswordRedisRepository.save(userId2, password2);
            userPasswordRedisRepository.save(userId3, password3);

            // then
            assertThat(userPasswordRedisRepository.get(userId1)).contains(password1);
            assertThat(userPasswordRedisRepository.get(userId2)).contains(password2);
            assertThat(userPasswordRedisRepository.get(userId3)).contains(password3);
        }
    }

    @Nested
    @DisplayName("4자리 비밀번호 삭제")
    class DeleteTest {

        @DisplayName("저장된 비밀번호를 삭제할 수 있다")
        @Test
        void delete() {
            // given
            Long userId = 1L;
            Integer password = 1234;
            userPasswordRedisRepository.save(userId, password);

            // when
            userPasswordRedisRepository.delete(userId);
            Optional<Integer> result = userPasswordRedisRepository.get(userId);

            // then
            assertThat(result).isEmpty();
        }

        @DisplayName("존재하지 않는 비밀번호를 삭제해도 예외가 발생하지 않는다")
        @Test
        void deleteNotFound() {
            // given
            Long notExistUserId = 999L;

            // when & then (예외 없이 정상 실행)
            userPasswordRedisRepository.delete(notExistUserId);
        }

        @DisplayName("삭제 후 다시 저장할 수 있다")
        @Test
        void deleteAndSaveAgain() {
            // given
            Long userId = 1L;
            Integer firstPassword = 1234;
            Integer secondPassword = 5678;

            userPasswordRedisRepository.save(userId, firstPassword);
            userPasswordRedisRepository.delete(userId);

            // when
            userPasswordRedisRepository.save(userId, secondPassword);
            Optional<Integer> result = userPasswordRedisRepository.get(userId);

            // then
            assertThat(result).isPresent();
            assertThat(result.get()).isEqualTo(secondPassword);
        }
    }

    @Nested
    @DisplayName("TTL 검증")
    class TtlTest {

        @DisplayName("저장된 비밀번호는 24시간 TTL을 가진다")
        @Test
        void ttlCheck() {
            // given
            Long userId = 1L;
            Integer password = 1234;

            // when
            userPasswordRedisRepository.save(userId, password);
            Long ttl = redisTemplate.getExpire("user:password:" + userId, TimeUnit.HOURS);

            System.out.println("ttl = " + ttl);

            // then
            assertThat(ttl).isNotNull();
            assertThat(ttl).isGreaterThanOrEqualTo(23L); // 24시간에 가까운 값
            assertThat(ttl).isLessThanOrEqualTo(24L);
        }

        @DisplayName("TTL이 만료되면 비밀번호가 자동으로 삭제된다")
        @Test
        void ttlExpiration() throws InterruptedException {
            // given
            Long userId = 1L;
            Integer password = 1234;

            // 테스트를 위해 짧은 TTL로 직접 설정
            redisTemplate.opsForValue().set("user:password:" + userId, password.toString(), 1, TimeUnit.SECONDS);

            // when
            Thread.sleep(1100); // 1.1초 대기
            Optional<Integer> result = userPasswordRedisRepository.get(userId);

            // then
            assertThat(result).isEmpty();
        }
    }
}
