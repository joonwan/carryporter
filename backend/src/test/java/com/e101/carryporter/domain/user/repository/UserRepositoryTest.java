package com.e101.carryporter.domain.user.repository;

import com.e101.carryporter.domain.user.entity.User;
import com.e101.carryporter.support.IntegrationTestSupport;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class UserRepositoryTest extends IntegrationTestSupport {

    @Autowired
    UserRepository userRepository;

    @Autowired
    EntityManager em;

    @DisplayName("일반 사용자를 저장할 수 있다.")
    @Test
    void saveBasicUser() {
        // given
        User user = User.createUser("test@mm.com");

        // when
        Long savedId = userRepository.save(user);
        flushAndClear();

        User findUser = userRepository.findById(savedId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        // then
        assertThat(findUser.getMmEmail()).isEqualTo(user.getMmEmail());
        assertThat(findUser.getAdminCredential()).isNull();
        assertThat(findUser.isAdmin()).isFalse();
    }

    @DisplayName("관리자를 저장할 수 있다.")
    @Test
    void saveAdminUser() {
        // given
        User user = User.createAdminUser("test@mm.com", "test admin user", "hashed password");

        // when
        Long savedId = userRepository.save(user);
        flushAndClear();

        User findUser = userRepository.findById(savedId)
                .orElseThrow();

        // then
        assertThat(findUser.getMmEmail()).isEqualTo(user.getMmEmail());
        assertThat(findUser.isAdmin()).isTrue();
        assertThat(findUser.getAdminCredential().getName()).isEqualTo("test admin user");
        assertThat(findUser.getAdminCredential().getPassword()).isEqualTo("hashed password");

    }

    @DisplayName("존재하지 않는 사용자의 pk 로 조회시 빈 옵셔널이 반환된다")
    @Test
    void findByNotExistId() {
        // given
        Long notExistUserId = 99999L;

        //  when
        Optional<User> userOpt = userRepository.findById(notExistUserId);

        // then
        assertThat(userOpt).isEmpty();
    }

    @DisplayName("이메일로 사용자를 조회할 수 있다.")
    @Test
    void findByMmEmail() {
        // given
        String email = "find@carryporter.com";
        User user = User.createUser(email); // 기존 팩토리 메서드 활용
        userRepository.save(user);

        flushAndClear(); // ★ 기존 스타일 유지: DB에 강제 반영하고 1차 캐시 비우기

        // when
        Optional<User> foundUser = userRepository.findByMmEmail(email);

        // then
        assertThat(foundUser).isPresent(); // 데이터가 있어야 함
        assertThat(foundUser.get().getMmEmail()).isEqualTo(email); // 이메일 일치 확인
    }

    @DisplayName("존재하지 않는 이메일로 조회시 빈 값이 반환된다.")
    @Test
    void findByNotExistEmail() {
        // given
        String notExistEmail = "unknown@carryporter.com";

        // when
        Optional<User> result = userRepository.findByMmEmail(notExistEmail);

        // then
        assertThat(result).isEmpty(); // 없어야 정상
    }

    private void flushAndClear() {
        em.flush();
        em.clear();
    }

}