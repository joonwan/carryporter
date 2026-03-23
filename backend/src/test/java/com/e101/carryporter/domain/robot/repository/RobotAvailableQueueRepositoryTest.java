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

class RobotAvailableQueueRepositoryTest extends IntegrationTestSupport {

    @Autowired
    RobotAvailableQueueRepository robotAvailableQueueRepository;

    @Autowired
    RobotRealTimeRepository robotRealTimeRepository;

    @Autowired
    RedisTemplate<String, Object> redisTemplate;

    private static final String AVAILABLE_ROBOTS_KEY = "robot:available";
    private static final String ROBOT_STATUS_PREFIX = "robot:status:";

    @AfterEach
    void tearDown() {
        Optional.ofNullable(redisTemplate.getConnectionFactory())
                .map(RedisConnectionFactory::getConnection)
                .ifPresent(conn -> conn.serverCommands().flushDb());
    }

    private void initRobotHash(Long robotId, String macAddress, RobotStatus status, int battery) {
        String key = ROBOT_STATUS_PREFIX + robotId;

        // RedisTemplate을 사용해 Map으로 한 번에 저장 (Config에 설정된 StringSerializer가 자동 적용됨)
        Map<String, String> data = Map.of(
                "macAddress", macAddress,
                "status", status.name(),
                "battery", String.valueOf(battery),
                "updatedAt", LocalDateTime.now().toString() // 날짜도 넣어주면 더 완벽
        );

        redisTemplate.opsForHash().putAll(key, data);
    }

    @Nested
    @DisplayName("로봇 배정 (acquireRobotScript)")
    class AcquireRobotIdTest {

        @DisplayName("큐에 로봇이 있으면 Lua 스크립트가 LPOP으로 로봇 ID를 반환하고 상태를 BUSY로 변경한다")
        @Test
        void acquireRobotIdSuccess() {
            // given
            Long robotId = 1L;
            initRobotHash(robotId, "AA:BB:CC:DD", RobotStatus.BUSY, 100);

            // updateStatusOnly로 IDLE 상태로 변경 (자동으로 큐에 추가됨)
            robotRealTimeRepository.updateStatusOnly(robotId, RobotStatus.IDLE);

            // when - Lua 스크립트 실행 (LPOP -> HSET status BUSY)
            Optional<Long> result = robotAvailableQueueRepository.acquireRobotId();

            // then
            assertThat(result).isPresent();
            assertThat(result.get()).isEqualTo(robotId);

            // Lua 스크립트가 상태를 BUSY로 변경했는지 확인
            Optional<RobotRealTimeInfo> updatedState = robotRealTimeRepository.findById(robotId);
            assertThat(updatedState).isPresent();
            assertThat(updatedState.get().getStatus()).isEqualTo(RobotStatus.BUSY);

            // 큐에서 제거되었는지 확인
            Long queueSize = redisTemplate.opsForList().size(AVAILABLE_ROBOTS_KEY);
            assertThat(queueSize).isEqualTo(0);
        }

        @DisplayName("큐에 여러 로봇이 있으면 Lua 스크립트가 FIFO 순서로 반환한다")
        @Test
        void acquireRobotIdFifo() {
            // given
            Long robotId1 = 1L;
            Long robotId2 = 2L;
            Long robotId3 = 3L;

            initRobotHash(robotId1, "AA:BB:CC:DD", RobotStatus.BUSY, 100);
            initRobotHash(robotId2, "EE:FF:GG:HH", RobotStatus.BUSY, 90);
            initRobotHash(robotId3, "II:JJ:KK:LL", RobotStatus.BUSY, 80);

            // updateStatusOnly로 IDLE 상태로 변경 (순서대로 큐에 추가됨)
            robotRealTimeRepository.updateStatusOnly(robotId1, RobotStatus.IDLE);
            robotRealTimeRepository.updateStatusOnly(robotId2, RobotStatus.IDLE);
            robotRealTimeRepository.updateStatusOnly(robotId3, RobotStatus.IDLE);

            // when - Lua 스크립트가 LPOP으로 앞에서부터 꺼냄
            Optional<Long> first = robotAvailableQueueRepository.acquireRobotId();
            Optional<Long> second = robotAvailableQueueRepository.acquireRobotId();
            Optional<Long> third = robotAvailableQueueRepository.acquireRobotId();

            // then - FIFO 순서 확인
            assertThat(first).contains(robotId1);
            assertThat(second).contains(robotId2);
            assertThat(third).contains(robotId3);

            // 모든 로봇이 BUSY 상태로 변경되었는지 확인
            assertThat(robotRealTimeRepository.findById(robotId1).get().getStatus()).isEqualTo(RobotStatus.BUSY);
            assertThat(robotRealTimeRepository.findById(robotId2).get().getStatus()).isEqualTo(RobotStatus.BUSY);
            assertThat(robotRealTimeRepository.findById(robotId3).get().getStatus()).isEqualTo(RobotStatus.BUSY);

            // 큐가 비어있는지 확인
            Long queueSize = redisTemplate.opsForList().size(AVAILABLE_ROBOTS_KEY);
            assertThat(queueSize).isEqualTo(0);
        }

        @DisplayName("큐가 비어있으면 Lua 스크립트가 nil을 반환하고 Java에서는 빈 Optional로 변환된다")
        @Test
        void acquireRobotIdWhenQueueEmpty() {
            // given - 큐에 아무것도 없음

            // when - Lua 스크립트가 LPOP -> nil 반환
            Optional<Long> result = robotAvailableQueueRepository.acquireRobotId();

            // then
            assertThat(result).isEmpty();
        }

        @DisplayName("로봇을 배정받은 후 다시 반환하면 재배정할 수 있다 (IDLE로 변경)")
        @Test
        void acquireAndReturnCycle() {
            // given
            Long robotId = 1L;
            initRobotHash(robotId, "AA:BB:CC:DD", RobotStatus.BUSY, 100);

            // updateStatusOnly로 IDLE로 변경 (자동으로 큐에 추가됨)
            robotRealTimeRepository.updateStatusOnly(robotId, RobotStatus.IDLE);

            // when - 첫 번째 배정 (acquireRobotScript)
            Optional<Long> firstAcquire = robotAvailableQueueRepository.acquireRobotId();
            assertThat(firstAcquire).contains(robotId);
            assertThat(robotRealTimeRepository.findById(robotId).get().getStatus()).isEqualTo(RobotStatus.BUSY);

            // 로봇 반환 (updateRobotInfoScript로 IDLE로 변경 -> 큐에 추가)
            robotRealTimeRepository.updateStatusOnly(robotId, RobotStatus.IDLE);
            assertThat(robotRealTimeRepository.findById(robotId).get().getStatus()).isEqualTo(RobotStatus.IDLE);

            // then - 다시 배정 가능
            Optional<Long> secondAcquire = robotAvailableQueueRepository.acquireRobotId();
            assertThat(secondAcquire).contains(robotId);
            assertThat(robotRealTimeRepository.findById(robotId).get().getStatus()).isEqualTo(RobotStatus.BUSY);
        }
    }

    @Nested
    @DisplayName("가용 로봇 수 조회 (getAvailableRobotCount)")
    class GetAvailableRobotCountTest {

        @DisplayName("큐에 로봇이 여러 개 있을 때 정확한 개수를 반환한다")
        @Test
        void getCountWhenMultipleRobots() {
            // given
            redisTemplate.opsForList().rightPush(AVAILABLE_ROBOTS_KEY, "1");
            redisTemplate.opsForList().rightPush(AVAILABLE_ROBOTS_KEY, "2");
            redisTemplate.opsForList().rightPush(AVAILABLE_ROBOTS_KEY, "3");

            // when
            Long count = robotAvailableQueueRepository.getAvailableRobotCount();

            // then
            assertThat(count).isEqualTo(3);
        }

        @DisplayName("큐가 비어있을 때 0을 반환한다")
        @Test
        void getCountWhenQueueEmpty() {
            // given - 큐가 비어있음

            // when
            Long count = robotAvailableQueueRepository.getAvailableRobotCount();

            // then
            assertThat(count).isEqualTo(0);
        }

        @DisplayName("로봇이 추가되고 제거될 때 개수가 정확하게 반영된다")
        @Test
        void countChangesWithPushAndPop() {
            // given
            assertThat(robotAvailableQueueRepository.getAvailableRobotCount()).isEqualTo(0);

            // when - 로봇 2대 추가
            redisTemplate.opsForList().rightPush(AVAILABLE_ROBOTS_KEY, "1");
            redisTemplate.opsForList().rightPush(AVAILABLE_ROBOTS_KEY, "2");

            // then
            assertThat(robotAvailableQueueRepository.getAvailableRobotCount()).isEqualTo(2);

            // when - 로봇 1대 제거
            redisTemplate.opsForList().leftPop(AVAILABLE_ROBOTS_KEY);

            // then
            assertThat(robotAvailableQueueRepository.getAvailableRobotCount()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("통합 시나리오")
    class IntegrationScenarioTest {

        @DisplayName("여러 로봇을 동시에 배정하고 반환할 수 있다")
        @Test
        void multipleRobotsCycle() {
            // given
            Long robotId1 = 1L;
            Long robotId2 = 2L;

            initRobotHash(robotId1, "AA:BB:CC:DD", RobotStatus.BUSY, 100);
            initRobotHash(robotId2, "EE:FF:GG:HH", RobotStatus.BUSY, 90);

            // updateStatusOnly로 IDLE로 변경 (자동으로 큐에 추가됨)
            robotRealTimeRepository.updateStatusOnly(robotId1, RobotStatus.IDLE);
            robotRealTimeRepository.updateStatusOnly(robotId2, RobotStatus.IDLE);

            // when - 두 로봇 배정
            Optional<Long> robot1 = robotAvailableQueueRepository.acquireRobotId();
            Optional<Long> robot2 = robotAvailableQueueRepository.acquireRobotId();

            // then
            assertThat(robot1).contains(robotId1);
            assertThat(robot2).contains(robotId2);
            assertThat(robotRealTimeRepository.findById(robotId1).get().getStatus()).isEqualTo(RobotStatus.BUSY);
            assertThat(robotRealTimeRepository.findById(robotId2).get().getStatus()).isEqualTo(RobotStatus.BUSY);

            // 큐가 비어있어야 함
            Long queueSize = redisTemplate.opsForList().size(AVAILABLE_ROBOTS_KEY);
            assertThat(queueSize).isEqualTo(0);

            // 로봇들을 다시 IDLE로 반환
            robotRealTimeRepository.updateStatusOnly(robotId1, RobotStatus.IDLE);
            robotRealTimeRepository.updateStatusOnly(robotId2, RobotStatus.IDLE);

            // 큐에 다시 추가되었는지 확인
            Long queueSizeAfterReturn = redisTemplate.opsForList().size(AVAILABLE_ROBOTS_KEY);
            assertThat(queueSizeAfterReturn).isEqualTo(2);
        }

        @DisplayName("Lua 스크립트가 원자적으로 로봇을 배정한다 (LPOP + HSET)")
        @Test
        void atomicAcquire() {
            // given
            Long robotId = 1L;
            initRobotHash(robotId, "AA:BB:CC:DD", RobotStatus.BUSY, 100);

            // updateStatusOnly로 IDLE로 변경 (자동으로 큐에 추가됨)
            robotRealTimeRepository.updateStatusOnly(robotId, RobotStatus.IDLE);

            Long initialQueueSize = redisTemplate.opsForList().size(AVAILABLE_ROBOTS_KEY);
            assertThat(initialQueueSize).isEqualTo(1);

            // when - Lua 스크립트가 원자적으로 실행
            Optional<Long> result = robotAvailableQueueRepository.acquireRobotId();

            // then - 큐에서 제거되고 상태가 변경되는 것이 원자적으로 수행됨
            assertThat(result).contains(robotId);

            Long finalQueueSize = redisTemplate.opsForList().size(AVAILABLE_ROBOTS_KEY);
            assertThat(finalQueueSize).isEqualTo(0);

            Optional<RobotRealTimeInfo> robotState = robotRealTimeRepository.findById(robotId);
            assertThat(robotState.get().getStatus()).isEqualTo(RobotStatus.BUSY);
        }

        @DisplayName("acquireRobotScript는 macAddress와 battery는 유지하고 status와 updatedAt만 변경한다")
        @Test
        void acquireScriptPreservesOtherFields() {
            // given
            Long robotId = 1L;
            String originalMacAddress = "AA:BB:CC:DD";
            int originalBattery = 75;

            initRobotHash(robotId, originalMacAddress, RobotStatus.BUSY, originalBattery);

            // updateStatusOnly로 IDLE로 변경 (자동으로 큐에 추가됨)
            robotRealTimeRepository.updateStatusOnly(robotId, RobotStatus.IDLE);

            // when - Lua 스크립트 실행
            Optional<Long> result = robotAvailableQueueRepository.acquireRobotId();

            // then - macAddress와 battery는 유지됨
            assertThat(result).contains(robotId);

            Optional<RobotRealTimeInfo> robotState = robotRealTimeRepository.findById(robotId);
            assertThat(robotState).isPresent();
            assertThat(robotState.get().getMacAddress()).isEqualTo(originalMacAddress);
            assertThat(robotState.get().getBattery()).isEqualTo(originalBattery);
            assertThat(robotState.get().getStatus()).isEqualTo(RobotStatus.BUSY);
            assertThat(robotState.get().getUpdatedAt()).isNotNull();
        }

        @DisplayName("존재하지 않는 로봇 ID가 큐에 있어도 배정은 성공한다 (상태만 새로 생성됨)")
        @Test
        void acquireNonExistentRobotCreatesNewState() {
            // given - 큐에만 로봇 ID가 있고 실제 해시는 없음
            Long robotId = 999L;

            // updateStatusOnly로 생성하고 IDLE로 변경 (자동으로 큐에 추가됨)
            robotRealTimeRepository.updateStatusOnly(robotId, RobotStatus.IDLE);

            // when - Lua 스크립트가 LPOP -> HSET (새로 생성)
            Optional<Long> result = robotAvailableQueueRepository.acquireRobotId();

            // then
            assertThat(result).contains(robotId);

            Optional<RobotRealTimeInfo> robotState = robotRealTimeRepository.findById(robotId);
            assertThat(robotState).isPresent();
            assertThat(robotState.get().getStatus()).isEqualTo(RobotStatus.BUSY);
            assertThat(robotState.get().getUpdatedAt()).isNotNull();
        }
    }
}
