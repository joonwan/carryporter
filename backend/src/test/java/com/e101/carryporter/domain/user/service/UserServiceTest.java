package com.e101.carryporter.domain.user.service;

import com.e101.carryporter.domain.user.entity.User;
import com.e101.carryporter.domain.user.repository.UserRepository;
import com.e101.carryporter.global.exception.BusinessException;
import com.e101.carryporter.support.IntegrationTestSupport;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.*;

class UserServiceTest extends IntegrationTestSupport {

    @Autowired
    UserService userService;

    @Autowired
    UserRepository userRepository;

    @Autowired
    EntityManager em;

    @DisplayName("사용자를 pk 기반으로 조회할 수 있다.")
    @Test
    void findById() {
        // given
        User user = User.createUser("test@mm.com");
        userRepository.save(user);

        flushAndClear();

        // when
        User findUser = userService.findById(user.getId());

        // then
        assertThat(findUser.getRole()).isEqualTo(user.getRole());
        assertThat(findUser.getMmEmail()).isEqualTo(user.getMmEmail());
    }
    @DisplayName("사용자가 없을 경우 예외가 발생한다.")
    @Test
    void findByNotExistsUserId() {
        // given
        Long notExistsUserId = 9999L;

        // when then
        assertThatThrownBy(() -> userService.findById(notExistsUserId))
                .isInstanceOf(BusinessException.class)
                .hasMessage("해당 사용자를 찾을 수 없습니다.");

    }

    private void flushAndClear() {
        em.flush();
        em.clear();
    }
}