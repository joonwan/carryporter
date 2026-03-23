package com.e101.carryporter.domain.robot.repository;

import com.e101.carryporter.domain.robot.entity.RobotRealTimeInfo;
import com.e101.carryporter.domain.robot.entity.RobotStatus;
import com.e101.carryporter.support.IntegrationTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class RobotRealTimeInfoRepositoryTest extends IntegrationTestSupport {

    @Autowired
    RobotRealTimeRepository robotRealTimeRepository;

    @Autowired
    RedisTemplate<String, Object> redisTemplate;

    private static final String ROBOT_STATUS_PREFIX = "robot:status:";
    private static final String AVAILABLE_ROBOTS_KEY = "robot:available";

    @AfterEach
    void tearDown() {
        Optional.ofNullable(redisTemplate.getConnectionFactory())
                .map(RedisConnectionFactory::getConnection)
                .ifPresent(conn -> conn.serverCommands().flushDb());
    }

    private void initRobotHash(Long robotId, String macAddress, RobotStatus status, int battery) {
        String key = ROBOT_STATUS_PREFIX + robotId;

        Map<String, String> data = Map.of(
                "macAddress", macAddress,
                "status", status.name(),
                "battery", String.valueOf(battery),
                // 실제 로직과 동일하게 날짜 정보도 넣어주는 것이 테스트 정합성에 좋습니다.
                "updatedAt", LocalDateTime.now().toString()
        );

        redisTemplate.opsForHash().putAll(key, data);
    }

    @Nested
    @DisplayName("상태 업데이트 (updateRobotInfoScript)")
    class UpdateStatusOnlyTest {

        @DisplayName("로봇 상태를 IDLE로 변경하면 Lua 스크립트가 available queue에 추가한다")
        @Test
        void updateStatusToIdleAddsToQueue() {
            // given
            Long robotId = 1L;
            initRobotHash(robotId, "AA:BB:CC:DD", RobotStatus.BUSY, 100);

            // when - Lua 스크립트 실행 (LREM -> status가 IDLE이면 RPUSH)
            robotRealTimeRepository.updateStatusOnly(robotId, RobotStatus.IDLE);

            // then
            Optional<RobotRealTimeInfo> result = robotRealTimeRepository.findById(robotId);
            assertThat(result).isPresent();
            assertThat(result.get().getStatus()).isEqualTo(RobotStatus.IDLE);

            // Lua 스크립트가 큐에 추가했는지 확인
            Long queueSize = redisTemplate.opsForList().size(AVAILABLE_ROBOTS_KEY);
            assertThat(queueSize).isEqualTo(1);

            Object robotIdInQueue = redisTemplate.opsForList().index(AVAILABLE_ROBOTS_KEY, 0);
            assertThat(robotIdInQueue.toString()).isEqualTo(robotId.toString());
        }

        @DisplayName("로봇 상태를 BUSY로 변경하면 Lua 스크립트가 available queue에서 제거한다")
        @Test
        void updateStatusToBusyRemovesFromQueue() {
            // given
            Long robotId = 1L;
            initRobotHash(robotId, "AA:BB:CC:DD", RobotStatus.BUSY, 100);

            // IDLE로 변경하면 자동으로 큐에 추가됨
            robotRealTimeRepository.updateStatusOnly(robotId, RobotStatus.IDLE);

            // 큐에 추가되었는지 확인
            Long initialQueueSize = redisTemplate.opsForList().size(AVAILABLE_ROBOTS_KEY);
            assertThat(initialQueueSize).isEqualTo(1);

            // when - Lua 스크립트 실행 (LREM으로 제거, IDLE이 아니므로 RPUSH 안 함)
            robotRealTimeRepository.updateStatusOnly(robotId, RobotStatus.BUSY);

            // then
            Optional<RobotRealTimeInfo> result = robotRealTimeRepository.findById(robotId);
            assertThat(result).isPresent();
            assertThat(result.get().getStatus()).isEqualTo(RobotStatus.BUSY);

            // Lua 스크립트가 큐에서 제거했는지 확인
            Long queueSize = redisTemplate.opsForList().size(AVAILABLE_ROBOTS_KEY);
            assertThat(queueSize).isEqualTo(0);
        }

        @DisplayName("로봇 상태를 OFFLINE으로 변경하면 Lua 스크립트가 available queue에서 제거한다")
        @Test
        void updateStatusToOfflineRemovesFromQueue() {
            // given
            Long robotId = 1L;
            initRobotHash(robotId, "AA:BB:CC:DD", RobotStatus.BUSY, 100);

            // IDLE로 변경하면 자동으로 큐에 추가됨
            robotRealTimeRepository.updateStatusOnly(robotId, RobotStatus.IDLE);

            // when - Lua 스크립트 실행 (LREM으로 제거, IDLE이 아니므로 RPUSH 안 함)
            robotRealTimeRepository.updateStatusOnly(robotId, RobotStatus.OFFLINE);

            // then
            Optional<RobotRealTimeInfo> result = robotRealTimeRepository.findById(robotId);
            assertThat(result).isPresent();
            assertThat(result.get().getStatus()).isEqualTo(RobotStatus.OFFLINE);

            // Lua 스크립트가 큐에서 제거했는지 확인
            Long queueSize = redisTemplate.opsForList().size(AVAILABLE_ROBOTS_KEY);
            assertThat(queueSize).isEqualTo(0);
        }

        @DisplayName("Lua 스크립트가 큐 중복을 방지한다 - LREM으로 먼저 제거 후 IDLE이면 추가")
        @Test
        void luaScriptPreventsDuplicateInQueue() {
            // given
            Long robotId = 1L;
            initRobotHash(robotId, "AA:BB:CC:DD", RobotStatus.BUSY, 100);

            // when - IDLE로 여러 번 변경 (Lua 스크립트가 매번 LREM -> RPUSH)
            robotRealTimeRepository.updateStatusOnly(robotId, RobotStatus.IDLE);
            robotRealTimeRepository.updateStatusOnly(robotId, RobotStatus.IDLE);
            robotRealTimeRepository.updateStatusOnly(robotId, RobotStatus.IDLE);

            // then - Lua 스크립트의 LREM 덕분에 큐에 한 번만 존재
            Long queueSize = redisTemplate.opsForList().size(AVAILABLE_ROBOTS_KEY);
            assertThat(queueSize).isEqualTo(1);
        }

        @DisplayName("존재하지 않는 로봇의 상태를 업데이트하면 Lua 스크립트가 새로 생성한다")
        @Test
        void updateStatusOnlyForNonExistentRobot() {
            // given
            Long robotId = 999L;

            // when - Lua 스크립트가 HSET으로 생성
            robotRealTimeRepository.updateStatusOnly(robotId, RobotStatus.IDLE);

            // then
            Optional<RobotRealTimeInfo> result = robotRealTimeRepository.findById(robotId);
            assertThat(result).isPresent();
            assertThat(result.get().getStatus()).isEqualTo(RobotStatus.IDLE);
            assertThat(result.get().getUpdatedAt()).isNotNull();

            // IDLE이므로 큐에 추가되어야 함
            Long queueSize = redisTemplate.opsForList().size(AVAILABLE_ROBOTS_KEY);
            assertThat(queueSize).isEqualTo(1);
        }

        @DisplayName("Lua 스크립트는 battery가 빈 문자열이면 업데이트하지 않는다")
        @Test
        void luaScriptDoesNotUpdateBatteryWhenEmpty() {
            // given
            Long robotId = 1L;
            initRobotHash(robotId, "AA:BB:CC:DD", RobotStatus.BUSY, 100);

            // when - updateStatusOnly는 battery를 빈 문자열로 전달
            robotRealTimeRepository.updateStatusOnly(robotId, RobotStatus.IDLE);

            // then - battery는 유지되어야 함
            Optional<RobotRealTimeInfo> result = robotRealTimeRepository.findById(robotId);
            assertThat(result).isPresent();
            assertThat(result.get().getBattery()).isEqualTo(100); // 변경 안 됨
            assertThat(result.get().getStatus()).isEqualTo(RobotStatus.IDLE); // status만 변경됨
        }
    }

    @Nested
    @DisplayName("배터리 업데이트 (직접 HSET)")
    class UpdateBatteryOnlyTest {

        @DisplayName("배터리만 업데이트할 수 있다")
        @Test
        void updateBatteryOnly() {
            // given
            Long robotId = 1L;
            initRobotHash(robotId, "AA:BB:CC:DD", RobotStatus.IDLE, 100);

            // when - 직접 HSET 사용 (Lua 스크립트 안 씀)
            robotRealTimeRepository.updateBatteryOnly(robotId, 75);

            // then
            Optional<RobotRealTimeInfo> result = robotRealTimeRepository.findById(robotId);
            assertThat(result).isPresent();
            assertThat(result.get().getBattery()).isEqualTo(75);
            assertThat(result.get().getStatus()).isEqualTo(RobotStatus.IDLE); // 상태는 변경되지 않음
            assertThat(result.get().getMacAddress()).isEqualTo("AA:BB:CC:DD");
        }

        @DisplayName("배터리 업데이트는 큐에 영향을 주지 않는다 (Lua 스크립트를 사용하지 않음)")
        @Test
        void updateBatteryDoesNotAffectQueue() {
            // given
            Long robotId = 1L;
            initRobotHash(robotId, "AA:BB:CC:DD", RobotStatus.BUSY, 100);

            // IDLE로 변경하여 큐에 추가
            robotRealTimeRepository.updateStatusOnly(robotId, RobotStatus.IDLE);

            Long queueSizeBefore = redisTemplate.opsForList().size(AVAILABLE_ROBOTS_KEY);

            // when - 직접 HSET이므로 큐에 영향 없음
            robotRealTimeRepository.updateBatteryOnly(robotId, 50);

            // then
            Long queueSizeAfter = redisTemplate.opsForList().size(AVAILABLE_ROBOTS_KEY);
            assertThat(queueSizeAfter).isEqualTo(queueSizeBefore);
        }

        @DisplayName("존재하지 않는 로봇의 배터리를 업데이트해도 예외가 발생하지 않는다")
        @Test
        void updateBatteryOnlyForNonExistentRobot() {
            // given
            Long robotId = 999L;

            // when & then - 예외가 발생하지 않아야 함 (로그만 warning)
            robotRealTimeRepository.updateBatteryOnly(robotId, 80);
        }
    }

    @Nested
    @DisplayName("신규 로봇 등록 (registerRobotScript)")
    class RegisterRobotTest {

        @DisplayName("IDLE 상태로 신규 로봇을 등록하면 Redis Hash가 생성되고 가용 큐에 추가된다")
        @Test
        void registerRobotWithIdleStatus() {
            // given
            Long robotId = 1L;
            RobotRealTimeInfo info = RobotRealTimeInfo.of("AA:BB:CC:DD", RobotStatus.IDLE, 100);

            // when
            robotRealTimeRepository.registerRobotStatus(robotId, info);

            // then - Redis Hash 생성 확인
            Optional<RobotRealTimeInfo> result = robotRealTimeRepository.findById(robotId);
            assertThat(result).isPresent();
            assertThat(result.get().getMacAddress()).isEqualTo("AA:BB:CC:DD");
            assertThat(result.get().getStatus()).isEqualTo(RobotStatus.IDLE);
            assertThat(result.get().getBattery()).isEqualTo(100);
            assertThat(result.get().getUpdatedAt()).isNotNull();

            // then - 가용 큐에 추가 확인
            Long queueSize = redisTemplate.opsForList().size(AVAILABLE_ROBOTS_KEY);
            assertThat(queueSize).isEqualTo(1);

            Object robotIdInQueue = redisTemplate.opsForList().index(AVAILABLE_ROBOTS_KEY, 0);
            assertThat(robotIdInQueue.toString()).isEqualTo(robotId.toString());
        }

        @DisplayName("BUSY 상태로 신규 로봇을 등록하면 Redis Hash만 생성되고 가용 큐에는 추가되지 않는다")
        @Test
        void registerRobotWithBusyStatus() {
            // given
            Long robotId = 2L;
            RobotRealTimeInfo info = RobotRealTimeInfo.of("EE:FF:GG:HH", RobotStatus.BUSY, 85);

            // when
            robotRealTimeRepository.registerRobotStatus(robotId, info);

            // then - Redis Hash 생성 확인
            Optional<RobotRealTimeInfo> result = robotRealTimeRepository.findById(robotId);
            assertThat(result).isPresent();
            assertThat(result.get().getMacAddress()).isEqualTo("EE:FF:GG:HH");
            assertThat(result.get().getStatus()).isEqualTo(RobotStatus.BUSY);
            assertThat(result.get().getBattery()).isEqualTo(85);

            // then - 가용 큐에는 추가되지 않음
            Long queueSize = redisTemplate.opsForList().size(AVAILABLE_ROBOTS_KEY);
            assertThat(queueSize).isEqualTo(0);
        }

        @DisplayName("OFFLINE 상태로 신규 로봇을 등록하면 Redis Hash만 생성되고 가용 큐에는 추가되지 않는다")
        @Test
        void registerRobotWithOfflineStatus() {
            // given
            Long robotId = 3L;
            RobotRealTimeInfo info = RobotRealTimeInfo.of("II:JJ:KK:LL", RobotStatus.OFFLINE, 0);

            // when
            robotRealTimeRepository.registerRobotStatus(robotId, info);

            // then - Redis Hash 생성 확인
            Optional<RobotRealTimeInfo> result = robotRealTimeRepository.findById(robotId);
            assertThat(result).isPresent();
            assertThat(result.get().getStatus()).isEqualTo(RobotStatus.OFFLINE);

            // then - 가용 큐에는 추가되지 않음
            Long queueSize = redisTemplate.opsForList().size(AVAILABLE_ROBOTS_KEY);
            assertThat(queueSize).isEqualTo(0);
        }

        @DisplayName("같은 robotId로 여러 번 등록하면 Lua Script가 중복을 방지한다 (LREM 후 RPUSH)")
        @Test
        void registerRobotPreventsDuplicateInQueue() {
            // given
            Long robotId = 1L;
            RobotRealTimeInfo info = RobotRealTimeInfo.of("AA:BB:CC:DD", RobotStatus.IDLE, 100);

            // when - 같은 로봇을 여러 번 등록 (실수로 중복 호출)
            robotRealTimeRepository.registerRobotStatus(robotId, info);
            robotRealTimeRepository.registerRobotStatus(robotId, info);
            robotRealTimeRepository.registerRobotStatus(robotId, info);

            // then - Lua Script의 LREM 덕분에 큐에 한 번만 존재
            Long queueSize = redisTemplate.opsForList().size(AVAILABLE_ROBOTS_KEY);
            assertThat(queueSize).isEqualTo(1);
        }

        @DisplayName("여러 로봇을 등록하면 FIFO 순서로 큐에 추가된다")
        @Test
        void registerMultipleRobotsInFifoOrder() {
            // given
            Long robotId1 = 1L;
            Long robotId2 = 2L;
            Long robotId3 = 3L;

            RobotRealTimeInfo info1 = RobotRealTimeInfo.of("AA:BB:CC:DD", RobotStatus.IDLE, 100);
            RobotRealTimeInfo info2 = RobotRealTimeInfo.of("EE:FF:GG:HH", RobotStatus.IDLE, 90);
            RobotRealTimeInfo info3 = RobotRealTimeInfo.of("II:JJ:KK:LL", RobotStatus.BUSY, 80);

            // when
            robotRealTimeRepository.registerRobotStatus(robotId1, info1);
            robotRealTimeRepository.registerRobotStatus(robotId2, info2);
            robotRealTimeRepository.registerRobotStatus(robotId3, info3); // BUSY는 큐에 추가 안됨

            // then - IDLE 상태인 로봇만 큐에 추가됨
            Long queueSize = redisTemplate.opsForList().size(AVAILABLE_ROBOTS_KEY);
            assertThat(queueSize).isEqualTo(2);

            // FIFO 순서 확인
            Object first = redisTemplate.opsForList().index(AVAILABLE_ROBOTS_KEY, 0);
            Object second = redisTemplate.opsForList().index(AVAILABLE_ROBOTS_KEY, 1);
            assertThat(first.toString()).isEqualTo(robotId1.toString());
            assertThat(second.toString()).isEqualTo(robotId2.toString());
        }

        @DisplayName("신규 등록 후 상태 업데이트가 정상 작동한다 (등록 -> BUSY 변경)")
        @Test
        void registerThenUpdateStatus() {
            // given - IDLE 상태로 등록
            Long robotId = 1L;
            RobotRealTimeInfo info = RobotRealTimeInfo.of("AA:BB:CC:DD", RobotStatus.IDLE, 100);
            robotRealTimeRepository.registerRobotStatus(robotId, info);

            // 큐에 추가되었는지 확인
            Long queueSizeBefore = redisTemplate.opsForList().size(AVAILABLE_ROBOTS_KEY);
            assertThat(queueSizeBefore).isEqualTo(1);

            // when - BUSY로 상태 변경 (Lua Script가 큐에서 제거)
            robotRealTimeRepository.updateStatusOnly(robotId, RobotStatus.BUSY);

            // then
            Optional<RobotRealTimeInfo> result = robotRealTimeRepository.findById(robotId);
            assertThat(result).isPresent();
            assertThat(result.get().getStatus()).isEqualTo(RobotStatus.BUSY);
            assertThat(result.get().getBattery()).isEqualTo(100); // 배터리는 유지됨

            // 큐에서 제거되었는지 확인
            Long queueSizeAfter = redisTemplate.opsForList().size(AVAILABLE_ROBOTS_KEY);
            assertThat(queueSizeAfter).isEqualTo(0);
        }

        @DisplayName("신규 등록 시 macAddress가 올바르게 저장된다")
        @Test
        void registerRobotSavesMacAddressCorrectly() {
            // given
            Long robotId = 1L;
            String expectedMacAddress = "12:34:56:78:90:AB";
            RobotRealTimeInfo info = RobotRealTimeInfo.of(expectedMacAddress, RobotStatus.IDLE, 100);

            // when
            robotRealTimeRepository.registerRobotStatus(robotId, info);

            // then - macAddress가 Redis Hash에 올바르게 저장되었는지 확인
            String key = ROBOT_STATUS_PREFIX + robotId;
            Object savedMacAddress = redisTemplate.opsForHash().get(key, "macAddress");
            assertThat(savedMacAddress).isEqualTo(expectedMacAddress);

            // findById로도 확인
            Optional<RobotRealTimeInfo> result = robotRealTimeRepository.findById(robotId);
            assertThat(result).isPresent();
            assertThat(result.get().getMacAddress()).isEqualTo(expectedMacAddress);
        }
    }

    @Nested
    @DisplayName("상태 조회")
    class FindByIdTest {

        @DisplayName("로봇 실시간 상태를 조회할 수 있다")
        @Test
        void findById() {
            // given
            Long robotId = 1L;
            initRobotHash(robotId, "AA:BB:CC:DD", RobotStatus.IDLE, 100);

            // when
            Optional<RobotRealTimeInfo> result = robotRealTimeRepository.findById(robotId);

            // then
            assertThat(result).isPresent();
            RobotRealTimeInfo info = result.get();
            assertThat(info.getMacAddress()).isEqualTo("AA:BB:CC:DD");
            assertThat(info.getStatus()).isEqualTo(RobotStatus.IDLE);
            assertThat(info.getBattery()).isEqualTo(100);
        }

        @DisplayName("존재하지 않는 Robot ID로 상태 조회 시 빈 Optional을 반환한다")
        @Test
        void findByIdNotFound() {
            // given
            Long notExistRobotId = 999L;

            // when
            Optional<RobotRealTimeInfo> result = robotRealTimeRepository.findById(notExistRobotId);

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("통합 시나리오")
    class IntegrationScenarioTest {

        @DisplayName("로봇 상태 전환 시나리오: BUSY -> IDLE -> BUSY (Lua 스크립트가 큐 관리)")
        @Test
        void statusTransitionScenario() {
            // given
            Long robotId = 1L;
            initRobotHash(robotId, "AA:BB:CC:DD", RobotStatus.BUSY, 100);

            // when & then - BUSY 상태 (큐에 없음)
            Optional<RobotRealTimeInfo> busyState = robotRealTimeRepository.findById(robotId);
            assertThat(busyState.get().getStatus()).isEqualTo(RobotStatus.BUSY);
            assertThat(redisTemplate.opsForList().size(AVAILABLE_ROBOTS_KEY)).isEqualTo(0);

            // IDLE로 변경 (Lua 스크립트가 LREM -> RPUSH로 큐에 추가)
            robotRealTimeRepository.updateStatusOnly(robotId, RobotStatus.IDLE);
            Optional<RobotRealTimeInfo> idleState = robotRealTimeRepository.findById(robotId);
            assertThat(idleState.get().getStatus()).isEqualTo(RobotStatus.IDLE);
            assertThat(redisTemplate.opsForList().size(AVAILABLE_ROBOTS_KEY)).isEqualTo(1);

            // 다시 BUSY로 변경 (Lua 스크립트가 LREM으로 큐에서 제거)
            robotRealTimeRepository.updateStatusOnly(robotId, RobotStatus.BUSY);
            Optional<RobotRealTimeInfo> busyAgain = robotRealTimeRepository.findById(robotId);
            assertThat(busyAgain.get().getStatus()).isEqualTo(RobotStatus.BUSY);
            assertThat(redisTemplate.opsForList().size(AVAILABLE_ROBOTS_KEY)).isEqualTo(0);
        }

        @DisplayName("배터리와 상태를 독립적으로 업데이트할 수 있다")
        @Test
        void independentUpdates() {
            // given
            Long robotId = 1L;
            initRobotHash(robotId, "AA:BB:CC:DD", RobotStatus.BUSY, 100);

            // IDLE로 변경
            robotRealTimeRepository.updateStatusOnly(robotId, RobotStatus.IDLE);

            // when - 배터리만 업데이트 (직접 HSET)
            robotRealTimeRepository.updateBatteryOnly(robotId, 90);

            Optional<RobotRealTimeInfo> afterBatteryUpdate = robotRealTimeRepository.findById(robotId);
            assertThat(afterBatteryUpdate.get().getBattery()).isEqualTo(90);
            assertThat(afterBatteryUpdate.get().getStatus()).isEqualTo(RobotStatus.IDLE);

            // 상태만 업데이트 (Lua 스크립트로 큐 관리)
            robotRealTimeRepository.updateStatusOnly(robotId, RobotStatus.BUSY);

            Optional<RobotRealTimeInfo> afterStatusUpdate = robotRealTimeRepository.findById(robotId);
            assertThat(afterStatusUpdate.get().getStatus()).isEqualTo(RobotStatus.BUSY);
            assertThat(afterStatusUpdate.get().getBattery()).isEqualTo(90); // 배터리는 유지됨

            // 다시 배터리만 업데이트
            robotRealTimeRepository.updateBatteryOnly(robotId, 80);

            Optional<RobotRealTimeInfo> finalState = robotRealTimeRepository.findById(robotId);
            assertThat(finalState.get().getBattery()).isEqualTo(80);
            assertThat(finalState.get().getStatus()).isEqualTo(RobotStatus.BUSY); // 상태는 유지됨
        }

        @DisplayName("여러 로봇의 상태를 관리할 수 있다 (Lua 스크립트가 각각 처리)")
        @Test
        void multipleRobotManagement() {
            // given
            Long robotId1 = 1L;
            Long robotId2 = 2L;
            Long robotId3 = 3L;

            initRobotHash(robotId1, "AA:BB:CC:DD", RobotStatus.BUSY, 100);
            initRobotHash(robotId2, "EE:FF:GG:HH", RobotStatus.BUSY, 90);
            initRobotHash(robotId3, "II:JJ:KK:LL", RobotStatus.OFFLINE, 80);

            // when - 각각 IDLE로 변경
            robotRealTimeRepository.updateStatusOnly(robotId1, RobotStatus.IDLE);
            robotRealTimeRepository.updateStatusOnly(robotId2, RobotStatus.IDLE);
            // robotId3는 OFFLINE 유지

            // then - Lua 스크립트가 IDLE인 것만 큐에 추가
            Long queueSize = redisTemplate.opsForList().size(AVAILABLE_ROBOTS_KEY);
            assertThat(queueSize).isEqualTo(2);

            // FIFO 순서 확인
            Object first = redisTemplate.opsForList().index(AVAILABLE_ROBOTS_KEY, 0);
            Object second = redisTemplate.opsForList().index(AVAILABLE_ROBOTS_KEY, 1);
            assertThat(first.toString()).isEqualTo(robotId1.toString());
            assertThat(second.toString()).isEqualTo(robotId2.toString());
        }
    }
}
