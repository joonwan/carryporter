package com.e101.carryporter.global.listener;

import com.e101.carryporter.domain.admin.event.AdminLockRequestEvent;
import com.e101.carryporter.domain.admin.event.AdminUnlockRequestEvent;
import com.e101.carryporter.domain.mission.event.*;
import com.e101.carryporter.domain.user.event.UserAuthSuccessEvent;
import com.e101.carryporter.global.service.mqtt.MqttPublisherService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("MqttCommandHandler 단위 테스트")
class MqttCommandHandlerTest {

    @Mock
    private MqttPublisherService mqttPublisherService;

    @InjectMocks
    private MqttCommandHandler mqttCommandHandler;

    private static final String TEST_MAC = "AA:BB:CC:DD:EE:FF";
    private static final Long TEST_MISSION_ID = 1L;
    private static final Long TEST_USER_ID = 100L;
    private static final String TEST_ROBOT_CODE = "robot-code";
    @Nested
    @DisplayName("관리자 요청 이벤트")
    class AdminRequestEvents {

        @Test
        @DisplayName("AdminUnlockRequestEvent 발생 시 로봇에게 unlock 명령 전송")
        void handleAdminUnlockRequest() {
            // given
            AdminUnlockRequestEvent event = new AdminUnlockRequestEvent(TEST_MISSION_ID, TEST_MAC);

            // when
            mqttCommandHandler.handleAdminUnlockRequest(event);

            // then
            verify(mqttPublisherService).sendCommand(TEST_MAC, "unlock", "{}");
        }

        @Test
        @DisplayName("AdminLockRequestEvent 발생 시 로봇에게 lock 명령 전송")
        void handleAdminLockRequest() {
            // given
            AdminLockRequestEvent event = new AdminLockRequestEvent(TEST_MISSION_ID, TEST_MAC);

            // when
            mqttCommandHandler.handleAdminLockRequest(event);

            // then
            verify(mqttPublisherService).sendCommand(TEST_MAC, "lock", "{}");
        }
    }

    @Nested
    @DisplayName("미션 이벤트")
    class MissionEvents {

        @Test
        @DisplayName("MissionStartedEvent 발생 시 로봇에게 이동 명령 전송")
        void handleMissionStarted() {
            // given
            String destination = "Gate A12";
            MissionStartedEvent event = new MissionStartedEvent(TEST_USER_ID, TEST_MISSION_ID, TEST_ROBOT_CODE, TEST_MAC, destination);

            // when
            mqttCommandHandler.handleMissionStarted(event);

            // then
            verify(mqttPublisherService).sendDispatchCommand(TEST_MAC, destination);
        }

        @Test
        @DisplayName("MissionAbortedEvent 발생 시 로봇에게 복귀 명령 전송")
        void handleMissionAborted() {
            // given
            MissionAbortedEvent event = new MissionAbortedEvent(
                    TEST_MISSION_ID, TEST_USER_ID, TEST_MAC, "비밀번호 3회 오류"
            );

            // when
            mqttCommandHandler.handleMissionAborted(event);

            // then
            verify(mqttPublisherService).sendReturnCommand(TEST_MAC);
        }

        @Test
        @DisplayName("MissionLockedEvent 발생 시 로봇에게 lock 명령 전송")
        void handleMissionLocked() {
            // given
            MissionLockRequestEvent event = new MissionLockRequestEvent(TEST_MISSION_ID, TEST_USER_ID, TEST_MAC);

            // when
            mqttCommandHandler.handleMissionLocked(event);

            // then
            verify(mqttPublisherService).sendCommand(TEST_MAC, "lock", "{}");
        }

        @Test
        @DisplayName("ReturnStartedEvent 발생 시 로봇에게 복귀 명령 전송")
        void handleReturnStarted() {
            // given
            Double homeX = 0.0;
            Double homeY = 0.0;
            ReturnStartedEvent event = new ReturnStartedEvent(TEST_MISSION_ID, TEST_MAC, homeX, homeY, "abc", LocalDateTime.now());

            // when
            mqttCommandHandler.handleReturnStarted(event);

            // then
            verify(mqttPublisherService).sendReturnCommand(TEST_MAC);
        }
    }

    @Nested
    @DisplayName("사용자 인증 이벤트")
    class UserAuthEvents {

        @Test
        @DisplayName("UserAuthSuccessEvent 발생 시 로봇에게 unlock 명령 전송")
        void handleUserAuthSuccess() {
            // given
            UserAuthSuccessEvent event = new UserAuthSuccessEvent(TEST_MISSION_ID, TEST_USER_ID, TEST_MAC);

            // when
            mqttCommandHandler.handleUserAuthSuccess(event);

            // then
            verify(mqttPublisherService).sendCommand(TEST_MAC, "unlock", "{}");
        }
    }
}
