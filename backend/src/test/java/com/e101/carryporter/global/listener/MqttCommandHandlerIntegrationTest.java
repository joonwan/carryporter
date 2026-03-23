package com.e101.carryporter.global.listener;

import com.e101.carryporter.domain.admin.event.AdminLockRequestEvent;
import com.e101.carryporter.domain.admin.event.AdminUnlockRequestEvent;
import com.e101.carryporter.domain.mission.event.MissionAbortedEvent;
import com.e101.carryporter.domain.mission.event.MissionLockRequestEvent;
import com.e101.carryporter.domain.mission.event.MissionStartedEvent;
import com.e101.carryporter.domain.mission.event.ReturnStartedEvent;
import com.e101.carryporter.domain.user.event.UserAuthSuccessEvent;
import com.e101.carryporter.support.IntegrationTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.integration.mqtt.outbound.MqttPahoMessageHandler;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.messaging.Message;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@DisplayName("MqttCommandHandler 통합 테스트")
// MqttHandler는 COMMIT 발생 or 트랜잭션이 없는 경우에만 동작하도록 설계 했기 때문
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class MqttCommandHandlerIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @MockitoBean
    private MqttPahoMessageHandler mqttOutbound;

    private static final String TEST_MAC = "AA:BB:CC:DD:EE:FF";
    private static final Long TEST_MISSION_ID = 1L;
    private static final Long TEST_USER_ID = 100L;
    private static final int ASYNC_TIMEOUT_MS = 5000;
    private static final String TEST_ROBOT_CODE = "robot-code";

    @Nested
    @DisplayName("관리자 요청 이벤트 통합 테스트")
    class AdminRequestEventsIntegration {

        @Test
        @DisplayName("AdminUnlockRequestEvent 발행 시 MQTT unlock 명령 전송")
        void publishAdminUnlockRequest() {
            // given
            AdminUnlockRequestEvent event = new AdminUnlockRequestEvent(TEST_MISSION_ID, TEST_MAC);

            // when
            eventPublisher.publishEvent(event);

            // then
            verifyMqttCommand("unlock", TEST_MAC);
        }

        @Test
        @DisplayName("AdminLockRequestEvent 발행 시 MQTT lock 명령 전송")
        void publishAdminLockRequest() {
            // given
            AdminLockRequestEvent event = new AdminLockRequestEvent(TEST_MISSION_ID, TEST_MAC);

            // when
            eventPublisher.publishEvent(event);

            // then
            verifyMqttCommand("lock", TEST_MAC);
        }
    }

    @Nested
    @DisplayName("미션 이벤트 통합 테스트")
    class MissionEventsIntegration {

        @Test
        @DisplayName("MissionStartedEvent 발행 시 MQTT move 명령 전송")
        void publishMissionStarted() {
            // given
            String destination = "Gate A12";
            MissionStartedEvent event = new MissionStartedEvent(TEST_USER_ID, TEST_MISSION_ID, TEST_ROBOT_CODE, TEST_MAC, destination);

            // when
            eventPublisher.publishEvent(event);

            // then
            verifyMqttMoveCommand(TEST_MAC, destination);
        }

        @Test
        @DisplayName("MissionAbortedEvent 발행 시 MQTT return 명령 전송")
        void publishMissionAborted() {
            // given
            MissionAbortedEvent event = new MissionAbortedEvent(
                    TEST_MISSION_ID, TEST_USER_ID, TEST_MAC, "비밀번호 3회 오류"
            );

            // when
            eventPublisher.publishEvent(event);

            // then
            verifyMqttCommand("return", TEST_MAC);
        }

        @Test
        @DisplayName("MissionLockRequestEvent 발행 시 MQTT lock 명령 전송")
        void publishMissionLockRequest() {
            // given
            MissionLockRequestEvent event = new MissionLockRequestEvent(TEST_MISSION_ID, TEST_USER_ID, TEST_MAC);

            // when
            eventPublisher.publishEvent(event);

            // then
            verifyMqttCommand("lock", TEST_MAC);
        }

        @Test
        @DisplayName("ReturnStartedEvent 발행 시 MQTT return 명령 전송")
        void publishReturnStarted() {
            // given
            Double homeX = 0.0;
            Double homeY = 0.0;
            ReturnStartedEvent event = new ReturnStartedEvent(TEST_MISSION_ID, TEST_MAC, homeX, homeY, "abc", LocalDateTime.now());

            // when
            eventPublisher.publishEvent(event);

            // then
            verifyMqttCommand("return", TEST_MAC);
        }
    }

    @Nested
    @DisplayName("사용자 인증 이벤트 통합 테스트")
    class UserAuthEventsIntegration {

        @Test
        @DisplayName("UserAuthSuccessEvent 발행 시 MQTT unlock 명령 전송")
        void publishUserAuthSuccess() {
            // given
            UserAuthSuccessEvent event = new UserAuthSuccessEvent(TEST_MISSION_ID, TEST_USER_ID, TEST_MAC);

            // when
            eventPublisher.publishEvent(event);

            // then
            verifyMqttCommand("unlock", TEST_MAC);
        }
    }

    private void verifyMqttCommand(String action, String mac) {
        ArgumentCaptor<Message<String>> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(mqttOutbound, timeout(ASYNC_TIMEOUT_MS)).handleMessage(messageCaptor.capture());

        Message<String> capturedMessage = messageCaptor.getValue();
        String actualTopic = (String) capturedMessage.getHeaders().get(MqttHeaders.TOPIC);

        String expectedTopic = String.format("robot/%s/command/%s", mac, action);
        assertThat(actualTopic).isEqualTo(expectedTopic);

        printResult(action, actualTopic, capturedMessage.getPayload());
    }

    private void verifyMqttMoveCommand(String mac, String destination) {
        ArgumentCaptor<Message<String>> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(mqttOutbound, timeout(ASYNC_TIMEOUT_MS)).handleMessage(messageCaptor.capture());

        Message<String> capturedMessage = messageCaptor.getValue();
        String actualTopic = (String) capturedMessage.getHeaders().get(MqttHeaders.TOPIC);
        String actualPayload = capturedMessage.getPayload();

        String expectedTopic = String.format("robot/%s/command/dispatch", mac);
        String expectedPayload = String.format("{\"destination\":\"%s\"}", destination);

        assertThat(actualTopic).isEqualTo(expectedTopic);
        assertThat(actualPayload).isEqualTo(expectedPayload);

        printResult("dispatch", actualTopic, actualPayload);
    }

    private void printResult(String action, String topic, String payload) {
        System.out.println("\n==================================================");
        System.out.println("   MQTT Command Handler Integration Test: " + action.toUpperCase());
        System.out.println("==================================================");
        System.out.println("TOPIC   : " + topic);
        System.out.println("PAYLOAD : " + payload);
        System.out.println("==================================================\n");
    }
}
