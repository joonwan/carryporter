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

class EmailCodeRedisRepositoryTest extends IntegrationTestSupport {

    @Autowired
    EmailCodeRedisRepository emailCodeRedisRepository;

    @Autowired
    RedisTemplate<String, Object> redisTemplate;

    @AfterEach
    void tearDown() {
        Optional.ofNullable(redisTemplate.getConnectionFactory())
                .map(RedisConnectionFactory::getConnection)
                .ifPresent(conn -> conn.serverCommands().flushDb());
    }

    @Nested
    @DisplayName("이메일 인증번호 저장 및 조회")
    class SaveAndGetTest {

        @DisplayName("이메일 인증번호를 저장하고 조회할 수 있다")
        @Test
        void saveAndGet() {
            // given
            String email = "test@example.com";
            Integer code = 123456;

            // when
            emailCodeRedisRepository.save(email, code);
            Optional<Integer> result = emailCodeRedisRepository.get(email);

            // then
            assertThat(result).isPresent();
            assertThat(result.get()).isEqualTo(code);
        }

        @DisplayName("같은 이메일에 인증번호를 여러 번 저장하면 마지막 값으로 덮어쓴다")
        @Test
        void saveOverwrite() {
            // given
            String email = "test@example.com";
            Integer firstCode = 111111;
            Integer secondCode = 999999;

            // when
            emailCodeRedisRepository.save(email, firstCode);
            emailCodeRedisRepository.save(email, secondCode);
            Optional<Integer> result = emailCodeRedisRepository.get(email);

            // then
            assertThat(result).isPresent();
            assertThat(result.get()).isEqualTo(secondCode);
        }

        @DisplayName("존재하지 않는 이메일로 조회하면 빈 Optional을 반환한다")
        @Test
        void getNotFound() {
            // given
            String notExistEmail = "notexist@example.com";

            // when
            Optional<Integer> result = emailCodeRedisRepository.get(notExistEmail);

            // then
            assertThat(result).isEmpty();
        }

        @DisplayName("여러 이메일의 인증번호를 독립적으로 저장하고 조회할 수 있다")
        @Test
        void multipleEmails() {
            // given
            String email1 = "user1@example.com";
            String email2 = "user2@example.com";
            String email3 = "user3@example.com";
            Integer code1 = 111111;
            Integer code2 = 222222;
            Integer code3 = 333333;

            // when
            emailCodeRedisRepository.save(email1, code1);
            emailCodeRedisRepository.save(email2, code2);
            emailCodeRedisRepository.save(email3, code3);

            // then
            assertThat(emailCodeRedisRepository.get(email1)).contains(code1);
            assertThat(emailCodeRedisRepository.get(email2)).contains(code2);
            assertThat(emailCodeRedisRepository.get(email3)).contains(code3);
        }
    }

    @Nested
    @DisplayName("이메일 인증번호 삭제")
    class DeleteTest {

        @DisplayName("저장된 인증번호를 삭제할 수 있다")
        @Test
        void delete() {
            // given
            String email = "test@example.com";
            Integer code = 123456;
            emailCodeRedisRepository.save(email, code);

            // when
            emailCodeRedisRepository.delete(email);
            Optional<Integer> result = emailCodeRedisRepository.get(email);

            // then
            assertThat(result).isEmpty();
        }

        @DisplayName("존재하지 않는 인증번호를 삭제해도 예외가 발생하지 않는다")
        @Test
        void deleteNotFound() {
            // given
            String notExistEmail = "notexist@example.com";

            // when & then
            emailCodeRedisRepository.delete(notExistEmail);
        }

        @DisplayName("삭제 후 다시 저장할 수 있다")
        @Test
        void deleteAndSaveAgain() {
            // given
            String email = "test@example.com";
            Integer firstCode = 111111;
            Integer secondCode = 999999;

            emailCodeRedisRepository.save(email, firstCode);
            emailCodeRedisRepository.delete(email);

            // when
            emailCodeRedisRepository.save(email, secondCode);
            Optional<Integer> result = emailCodeRedisRepository.get(email);

            // then
            assertThat(result).isPresent();
            assertThat(result.get()).isEqualTo(secondCode);
        }
    }

    @Nested
    @DisplayName("TTL 검증")
    class TtlTest {

        @DisplayName("저장된 인증번호는 5분 TTL을 가진다")
        @Test
        void ttlCheck() {
            // given
            String email = "test@example.com";
            Integer code = 123456;

            // when
            emailCodeRedisRepository.save(email, code);
            Long ttl = redisTemplate.getExpire("auth:email:code:" + email, TimeUnit.SECONDS);

            // then
            assertThat(ttl).isNotNull();
            assertThat(ttl).isGreaterThan(290L); // 5분(300초)에 가까운 값
            assertThat(ttl).isLessThanOrEqualTo(300L);
        }

        @DisplayName("TTL이 만료되면 인증번호가 자동으로 삭제된다")
        @Test
        void ttlExpiration() throws InterruptedException {
            // given
            String email = "test@example.com";
            Integer code = 123456;

            // 테스트를 위해 짧은 TTL로 직접 설정
            redisTemplate.opsForValue().set("auth:email:code:" + email, code.toString(), 1, TimeUnit.SECONDS);

            // when
            Thread.sleep(1100); // 1.1초 대기
            Optional<Integer> result = emailCodeRedisRepository.get(email);

            // then
            assertThat(result).isEmpty();
        }
    }
}
