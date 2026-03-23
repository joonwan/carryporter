package com.e101.carryporter.global.service.mqtt;

import com.e101.carryporter.domain.location.entity.Location;
import com.e101.carryporter.domain.location.repository.LocationRepository;
import com.e101.carryporter.domain.mission.entity.Mission;
import com.e101.carryporter.domain.mission.entity.MissionStatus;
import com.e101.carryporter.domain.mission.event.MissionUnlockedEvent;
import com.e101.carryporter.domain.mission.repository.MissionRepository;
import com.e101.carryporter.domain.robot.entity.Robot;
import com.e101.carryporter.domain.robot.event.RobotArrivalEvent;
import com.e101.carryporter.domain.robot.event.RobotReturnedEvent;
import com.e101.carryporter.domain.robot.repository.RobotRepository;
import com.e101.carryporter.domain.user.entity.User;
import com.e101.carryporter.domain.user.repository.UserRepository;
import com.e101.carryporter.support.IntegrationTestSupport;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.mqtt.outbound.MqttPahoMessageHandler;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class MqttSubscriberServiceTest extends IntegrationTestSupport {

    @Autowired
    private MqttSubscriberService mqttSubscriberService;

    @Autowired
    private RobotRepository robotRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MissionRepository missionRepository;

    @Autowired
    private LocationRepository locationRepository;

    @Autowired
    private EntityManager em;

    @MockitoBean
    private MqttPahoMessageHandler mqttOutbound;

    @Test
    @DisplayName("로봇 등록 메시지 수신 처리 테스트")
    void handleRegister() {
        // given
        String mac = "00:11:22:33:44:55";
        String topic = "robot/" + mac + "/register";
        String payload = "{\"mac\":\"" + mac + "\"}";

        // when & then
        assertThatCode(() -> mqttSubscriberService.handleMqttMessage(topic, payload))
            .doesNotThrowAnyException();
        printReceivedMessage("로봇 등록", topic, payload);

    }

    @Test
    @DisplayName("로봇 상태 보고 메시지 수신 처리 테스트")
    void handleStatus() {
        // given
        String mac = "00:11:22:33:44:55";
        String topic = "robot/" + mac + "/status";
        String payload = "{\"bat\":80,\"x\":10.5,\"y\":20.3}";

        // when & then
        assertThatCode(() -> mqttSubscriberService.handleMqttMessage(topic, payload))
            .doesNotThrowAnyException();

        printReceivedMessage("상태 보고", topic, payload);
    }

    @Test
    @DisplayName("로봇 도착 알림 메시지 수신 시 RobotArrivalEvent가 발행된다")
    void handleArrived() {
        String mac = "AA:BB:CC:DD:EE:FF";

        // given
        // 1. 기초 데이터 세팅 (User, Robot, Location)
        User user = User.createUser("test@mm.com");
        userRepository.save(user);

        Robot robot = Robot.createRobot("test code", mac);
        robotRepository.save(robot);

        Location callLocation = Location.createLocation("Gate A12", "탑승구 A12");
        locationRepository.save(callLocation);

        // 2. 미션 생성
        Mission mission = Mission.createMission(user, callLocation);
        mission.assignRobot(robot);
        mission.dispatch(); // MOVING 상태로 변경
        missionRepository.save(mission);

        // 영속성 컨텍스트 비우기 (DB에 반영하여 실제 조회 환경과 맞춤)
        flushAndClear();

        // 3. MQTT 메시지 생성 (DB에 저장된 missionId 사용)
        String topic = "robot/" + mac + "/arrived";
        String payload = "{\"missionId\":" + mission.getId() + "}";

        // when
        assertThatCode(() -> mqttSubscriberService.handleMqttMessage(topic, payload))
            .doesNotThrowAnyException();
        // then
        // 1. 이벤트가 1개 발생했는지 확인
        long publishedCount = events.stream(RobotArrivalEvent.class).count();
        assertThat(publishedCount).isEqualTo(1);

        // 2. 발생한 이벤트 내용 검증
        RobotArrivalEvent publishedEvent = events.stream(RobotArrivalEvent.class)
            .findFirst()
            .orElseThrow();

        assertThat(publishedEvent.missionId()).isEqualTo(mission.getId());
        assertThat(publishedEvent.userId()).isEqualTo(user.getId());

        printReceivedMessage("도착 알림 (Event 발행 성공)", topic, payload);
    }

    @Test
    @DisplayName("배송 완료 메시지 수신 처리 테스트")
    void handleDelivered() {
        // given
        String mac = "AA:BB:CC:DD:EE:FF";
        String topic = "robot/" + mac + "/delivered";
        String payload = "{\"missionId\":101}";

        // when & then
        assertThatCode(() -> mqttSubscriberService.handleMqttMessage(topic, payload))
            .doesNotThrowAnyException();

        printReceivedMessage("배송 완료", topic, payload);
    }

    @Test
    @DisplayName("에러 발생 메시지 수신 처리 테스트")
    void handleError() {
        // given
        String mac = "12:34:56:78:90:AB";
        String topic = "robot/" + mac + "/error";
        String payload = "{\"code\":\"ERR_01\",\"msg\":\"Battery low\"}";

        // when & then
        assertThatCode(() -> mqttSubscriberService.handleMqttMessage(topic, payload))
            .doesNotThrowAnyException();

        printReceivedMessage("에러 발생", topic, payload);
    }

    @Test
    @DisplayName("잘못된 토픽 형식 메시지 처리 테스트")
    void handleInvalidTopic() {
        // given
        String topic = "invalid/topic";
        String payload = "{}";

        // when & then
        assertThatCode(() -> mqttSubscriberService.handleMqttMessage(topic, payload))
            .doesNotThrowAnyException();

        printReceivedMessage("잘못된 토픽", topic, payload);
    }

    @Test
    @DisplayName("알 수 없는 액션 메시지 처리 테스트")
    void handleUnknownAction() {
        // given
        String mac = "00:11:22:33:44:55";
        String topic = "robot/" + mac + "/unknown";
        String payload = "{}";

        // when & then
        assertThatCode(() -> mqttSubscriberService.handleMqttMessage(topic, payload))
            .doesNotThrowAnyException();

        printReceivedMessage("알 수 없는 액션", topic, payload);
    }

    @Test
    @DisplayName("로봇 관리소 복귀 메시지 수신 시 RobotReturnedEvent가 발행된다")
    void handleIDLE() {
        // given
        String mac = "AA:BB:CC:DD:EE:FF";
        User user = User.createUser("return-test@mm.com");
        userRepository.save(user);

        Robot robot = Robot.createRobot("R-001", mac);
        robotRepository.save(robot);

        Location location = Location.createLocation("Station", "관리소");
        locationRepository.save(location);

        Mission mission = Mission.createMission(user, location);
        mission.assignRobot(robot);
        missionRepository.save(mission);

        // 테스트를 위해 미션 상태를 RETURNING으로 강제 변경
        em.createQuery("UPDATE Mission m SET m.missionStatus = :status WHERE m.id = :id")
            .setParameter("status", MissionStatus.RETURNING)
            .setParameter("id", mission.getId())
            .executeUpdate();

        flushAndClear();

        String topic = "robot/" + mac + "/IDLE";
        String payload = "{\"missionId\":" + mission.getId() + "}";

        // when
        assertThatCode(() -> mqttSubscriberService.handleMqttMessage(topic, payload))
            .doesNotThrowAnyException();

        // then
        long publishedCount = events.stream(RobotReturnedEvent.class).count();
        assertThat(publishedCount).isEqualTo(1);

        RobotReturnedEvent publishedEvent = events.stream(RobotReturnedEvent.class)
            .findFirst()
            .orElseThrow();

        assertThat(publishedEvent.missionId()).isEqualTo(mission.getId());
        assertThat(publishedEvent.robotId()).isEqualTo(robot.getId());
        assertThat(publishedEvent.robotMacAddress()).isEqualTo(mac);

        printReceivedMessage("관리소 복귀", topic, payload);
    }

    @Test
    @DisplayName("존재하지 않는 로봇의 관리소 복귀 메시지 수신 시 이벤트가 발행되지 않는다")
    void handleIDLEWithNotExistRobot() {
        // given
        String mac = "00:00:00:00:00:00";
        String topic = "robot/" + mac + "/IDLE";
        String payload = "{\"missionId\":101}";

        // when
        assertThatCode(() -> mqttSubscriberService.handleMqttMessage(topic, payload))
            .doesNotThrowAnyException();

        // then
        long publishedCount = events.stream(RobotReturnedEvent.class).count();
        assertThat(publishedCount).isEqualTo(0);

        printReceivedMessage("존재하지 않는 로봇 복귀", topic, payload);
    }

    @Test
    @DisplayName("로봇 잠금 완료 메시지 수신 시 MissionLockedEvent가 발행된다")
    void handleLocked() {
        // given
        String mac = "AA:BB:CC:DD:EE:FF";

        // 1. 기초 데이터 세팅 (User, Robot, Location)
        User user = User.createUser("locker-test@mm.com");
        userRepository.save(user);

        Robot robot = Robot.createRobot("Locker-Robot", mac);
        robotRepository.save(robot);

        Location startLocation = Location.createLocation("Lobby", "로비");
        locationRepository.save(startLocation);

        // 2. 미션 생성 및 로봇 배정
        Mission mission = Mission.createMission(user, startLocation);
        mission.assignRobot(robot);
        missionRepository.save(mission);

        // 테스트를 위해 미션 상태를 UNLOCKED로 강제 변경
        em.createQuery("UPDATE Mission m SET m.missionStatus = :status WHERE m.id = :id")
            .setParameter("status", MissionStatus.UNLOCKED)
            .setParameter("id", mission.getId())
            .executeUpdate();

        flushAndClear();

        // 3. MQTT 메시지 생성 (HW가 보내는 locked 응답 시뮬레이션)
        String topic = "robot/" + mac + "/locked";
        String payload = "{\"missionId\":" + mission.getId() + ", \"status\":\"success\"}";

        // when
        assertThatCode(() -> mqttSubscriberService.handleMqttMessage(topic, payload))
            .doesNotThrowAnyException();

        // then
        // 1. MissionLockedEvent가 발행되었는지 확인
        long publishedCount = events.stream(com.e101.carryporter.domain.mission.event.MissionLockedEvent.class).count();
        assertThat(publishedCount).isEqualTo(1);

        // 2. 발행된 이벤트의 필드값 검증
        com.e101.carryporter.domain.mission.event.MissionLockedEvent publishedEvent = events.stream(com.e101.carryporter.domain.mission.event.MissionLockedEvent.class)
            .findFirst()
            .orElseThrow();

        assertThat(publishedEvent.userId()).isEqualTo(user.getId());
        assertThat(publishedEvent.missionId()).isEqualTo(mission.getId());
        assertThat(publishedEvent.robotMacAddress()).isEqualTo(mac);

        printReceivedMessage("로봇 잠금 완료 (Event 발행 성공)", topic, payload);
    }

    @Test
    @DisplayName("로봇 열림 완료 메시지 수신 시 MissionUnlockedEvent가 발행된다")
    void handleUnlocked() {
        // given
        String mac = "AA:BB:CC:DD:EE:FF";

        // 1. 기초 데이터 세팅 (User, Robot, Location)
        User user = User.createUser("locker-test@mm.com");
        userRepository.save(user);

        Robot robot = Robot.createRobot("Locker-Robot", mac);
        robotRepository.save(robot);

        Location startLocation = Location.createLocation("Lobby", "로비");
        locationRepository.save(startLocation);

        // 2. 미션 생성 및 로봇 배정
        Mission mission = Mission.createMission(user, startLocation);
        mission.assignRobot(robot);
        missionRepository.save(mission);

        // 테스트를 위해 미션 상태를 ARRIVED로 강제 변경
        em.createQuery("UPDATE Mission m SET m.missionStatus = :status WHERE m.id = :id")
            .setParameter("status", MissionStatus.ARRIVED)
            .setParameter("id", mission.getId())
            .executeUpdate();

        flushAndClear();


        String topic = "robot/" + mac + "/unlocked";
        String payload = "{\"missionId\":" + mission.getId() + ", \"status\":\"success\"}";

        // when
        assertThatCode(() -> mqttSubscriberService.handleMqttMessage(topic, payload))
            .doesNotThrowAnyException();

        // then
        long publishedCount = events.stream(MissionUnlockedEvent.class).count();
        assertThat(publishedCount).isEqualTo(1);

        MissionUnlockedEvent event = events.stream(MissionUnlockedEvent.class)
            .findFirst()
            .orElseThrow();

        assertThat(event.userId()).isEqualTo(user.getId());
        assertThat(event.missionId()).isEqualTo(mission.getId());

        printReceivedMessage("로봇 열림 완료 (Event 발행 성공)", topic, payload);
    }


    @Test
    @DisplayName("존재하지 않는 미션 ID로 잠금 완료 메시지 수신 시 예외를 던지지 않고 무시한다")
    void handleLockedWithInvalidMissionId() {
        // given
        String mac = "AA:BB:CC:DD:EE:FF";
        Robot robot = Robot.createRobot("Test-Robot", mac);
        robotRepository.save(robot);
        flushAndClear();

        String topic = "robot/" + mac + "/locked";
        String payload = "{\"missionId\":9999}"; // 존재하지 않는 ID

        // when & then (서비스 로직의 catch 블록 덕분에 예외가 전파되지 않아야 함)
        assertThatCode(() -> mqttSubscriberService.handleMqttMessage(topic, payload))
            .doesNotThrowAnyException();

        // 이벤트가 발행되지 않았는지 확인
        long publishedCount = events.stream(com.e101.carryporter.domain.mission.event.MissionLockedEvent.class).count();
        assertThat(publishedCount).isEqualTo(0);

        printReceivedMessage("존재하지 않는 미션 잠금 시도 (무시 처리)", topic, payload);
    }

    @Test
    @DisplayName("존재하지 않는 미션 ID로 열림 완료 메시지 수신 시 예외를 던지지 않고 무시한다")
    void handleUnlockedWithInvalidMissionId() {
        // given
        String mac = "AA:BB:CC:DD:EE:FF";
        Robot robot = Robot.createRobot("Test-Robot", mac);
        robotRepository.save(robot);
        flushAndClear();

        String topic = "robot/" + mac + "/unlocked";
        String payload = "{\"missionId\":9999}"; // 존재하지 않는 ID

        // when & then (서비스 로직의 catch 블록 덕분에 예외가 전파되지 않아야 함)
        assertThatCode(() -> mqttSubscriberService.handleMqttMessage(topic, payload))
            .doesNotThrowAnyException();

        // 이벤트가 발행되지 않았는지 확인
        long publishedCount = events.stream(MissionUnlockedEvent.class).count();
        assertThat(publishedCount).isEqualTo(0);

        printReceivedMessage("존재하지 않는 미션 열림 시도 (무시 처리)", topic, payload);
    }

    /**
     * 테스트용 Message 객체 생성 헬퍼 메서드
     */
    private Message<String> createMessage(String topic, String payload) {
        return MessageBuilder
            .withPayload(payload)
            .setHeader(MqttHeaders.RECEIVED_TOPIC, topic)
            .build();
    }

    /**
     * 수신 메시지 출력 헬퍼 메서드
     */
    private void printReceivedMessage(String testTitle, String topic, String payload) {
        System.out.println("\n==================================================");
        System.out.println("   TEST: " + testTitle);
        System.out.println("==================================================");
        System.out.println("TOPIC   : " + topic);
        System.out.println("PAYLOAD : " + payload);
        System.out.println("==================================================\n");
    }

    private void flushAndClear() {
        em.flush();
        em.clear();
    }
}
