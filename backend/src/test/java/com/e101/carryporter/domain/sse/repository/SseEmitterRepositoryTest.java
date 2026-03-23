package com.e101.carryporter.domain.sse.repository;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

class SseEmitterRepositoryTest {

    // 테스트할 대상 (스프링 없이 순수 자바 객체로 생성)
    SseEmitterRepository repository = new SseEmitterRepository();

    @Test
    @DisplayName("일반 유저 저장 및 조회 테스트")
    void saveAndFindUser() {
        // given
        Long userId = 1L;
        SseEmitter emitter = new SseEmitter();

        // when
        repository.saveUser(userId, emitter);
        SseEmitter result = repository.findUser(userId);

        // then
        Assertions.assertEquals(emitter, result);
    }

    @Test
    @DisplayName("일반 유저 삭제 테스트")
    void deleteUser() {
        // given
        Long userId = 1L;
        SseEmitter emitter = new SseEmitter();
        repository.saveUser(userId, emitter);

        // when
        repository.deleteUser(userId);

        // then
        Assertions.assertNull(repository.findUser(userId));
    }

    @Test
    @DisplayName("관리자 저장 및 전체 조회 테스트")
    void saveAndFindAdmin() {
        // given
        Long admin1 = 1L;
        Long admin2 = 2L;
        repository.saveAdmin(admin1, new SseEmitter());
        repository.saveAdmin(admin2, new SseEmitter());

        // when
        Map<Long, SseEmitter> admins = repository.findAllAdmins();

        // then
        Assertions.assertEquals(2, admins.size()); // 2명이 들어가 있어야 함
        Assertions.assertTrue(admins.containsKey(admin1));
        Assertions.assertTrue(admins.containsKey(admin2));
    }
}
