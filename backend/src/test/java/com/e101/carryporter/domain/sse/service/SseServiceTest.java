package com.e101.carryporter.domain.sse.service;

import com.e101.carryporter.domain.mission.entity.MissionStatus; // 미션 상태 Enum 임포트
import com.e101.carryporter.domain.sse.repository.SseEmitterRepository;
import com.e101.carryporter.domain.user.entity.Role;
import com.e101.carryporter.support.IntegrationTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import static org.assertj.core.api.Assertions.assertThat;

class SseServiceTest extends IntegrationTestSupport {

    @Autowired
    private SseService sseService;

    @Autowired
    private SseEmitterRepository repository;

    @Test
    @DisplayName("미션 상태 변경 알림 전송 테스트: 실제 MissionStatus를 사용한다")
    void sendMissionStatusNotification() {
        // given
        Long userId = 1L;
        sseService.subscribe(userId, "BASIC");

        // 실제 DB에 들어갈 미션 상태 Enum 사용
        MissionStatus status = MissionStatus.ASSIGNED;
        String data = "로봇 배정이 완료되었습니다.";

        // when & then
        // status.name()은 "ASSIGNED" 문자열을 반환하여 sse의 eventName이 됩니다.
        sseService.sendToUser(userId, status.name(), data);

        assertThat(repository.findUser(userId)).isNotNull();
    }

    @Test
    @DisplayName("관리자 브로드캐스트: 새로운 미션 요청 알림")
    void broadcastNewMissionToAdmins() {
        // given
        Long adminId = 99L;

        sseService.subscribe(adminId, Role.ADMIN.name());

        // when
        // MissionStatus.REQUESTED.name() -> "REQUESTED"
        sseService.broadcastToAdmins(MissionStatus.REQUESTED.name(), "새로운 미션 요청!");

        // then
        // 이제 실제 관리자 Map에 저장되었으므로 key를 찾을 수 있습니다.
        assertThat(repository.findAllAdmins()).containsKey(adminId);
    }

    @Test
    @DisplayName("유저 구독: Role.BASIC으로 구독 시 유저 저장소에 저장되어야 한다")
    void subscribeUser() {
        // given
        Long userId = 1L;
        String role = Role.BASIC.name(); // "BASIC"

        // when
        sseService.subscribe(userId, role);

        // then
        // 관리자 저장소가 아닌 유저 저장소에서 조회되는지 확인
        assertThat(repository.findUser(userId)).isNotNull();
        assertThat(repository.findAllAdmins()).doesNotContainKey(userId);
    }

    @Test
    @DisplayName("유저 알림: 특정 유저에게 MissionStatus 기반 이벤트를 전송한다")
    void sendNotificationToUser() {
        // given
        Long userId = 2L;
        sseService.subscribe(userId, Role.BASIC.name());

        // 실제 DB의 MissionStatus Enum 사용
        MissionStatus status = MissionStatus.ARRIVED;
        String message = "로봇이 도착했습니다!";

        // when & then
        // 예외 없이 로직이 수행되는지 확인
        sseService.sendToUser(userId, status.name(), message);

        assertThat(repository.findUser(userId)).isNotNull();
    }

    @Test
    @DisplayName("역할 분리 테스트: 동일 ID라도 역할에 따라 저장소가 달라야 한다")
    void subscribeDifferentRoles() {
        // given
        Long commonId = 100L;

        // when
        sseService.subscribe(commonId, Role.BASIC.name());
        sseService.subscribe(commonId, Role.ADMIN.name());

        // then
        // 각 저장소에 해당 ID가 키로 존재하는지 확인
        assertThat(repository.findUser(commonId)).isNotNull();
        assertThat(repository.findAllAdmins()).containsKey(commonId);
    }
}