package com.e101.carryporter.domain.robot.repository;

import com.e101.carryporter.domain.robot.entity.RobotRealTimeInfo;
import com.e101.carryporter.domain.robot.entity.RobotStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
@Slf4j
public class RobotRealTimeRepository {

    private static final String ROBOT_STATUS_PREFIX = "robot:status:";
    private static final String AVAILABLE_ROBOTS_KEY = "robot:available";

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private final RedisScript<Long> updateRobotInfoScript;
    private final RedisScript<Long> registerRobotScript;

    public RobotRealTimeRepository(
            RedisTemplate<String, Object> redisTemplate,
            ObjectMapper objectMapper,
            @Qualifier("updateRobotInfoScript") RedisScript<Long> updateRobotInfoScript,
            @Qualifier("registerRobotScript") RedisScript<Long> registerRobotScript
    ) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.updateRobotInfoScript = updateRobotInfoScript;
        this.registerRobotScript = registerRobotScript;
    }

    // redis 상태저장소와 hash 에 로봇 상태 원자적으로 저장
    public void registerRobotStatus(Long robotId, RobotRealTimeInfo robotRealTimeInfo) {
        String key = ROBOT_STATUS_PREFIX + robotId;

        try {
            // Lua Script 실행 (원자적으로 Hash 생성 + 큐 추가)
            redisTemplate.execute(
                    registerRobotScript,
                    List.of(key, AVAILABLE_ROBOTS_KEY),           // KEYS
                    String.valueOf(robotId),                      // ARGV[1]
                    robotRealTimeInfo.getMacAddress(),            // ARGV[2]
                    robotRealTimeInfo.getStatus().name(),         // ARGV[3]
                    String.valueOf(robotRealTimeInfo.getBattery()), // ARGV[4]
                    LocalDateTime.now().toString()                // ARGV[5]
            );

            log.info("신규 로봇 등록 완료: robotId={}, macAddress={}, status={}",
                    robotId, robotRealTimeInfo.getMacAddress(), robotRealTimeInfo.getStatus());

        } catch (Exception e) {
            log.error("신규 로봇 등록 실패: robotId={}", robotId, e);
            throw e;
        }
    }

    // status 만 업데이트
    public void updateStatusOnly(Long robotId, RobotStatus newStatus) {
        String key = ROBOT_STATUS_PREFIX + robotId;

        try {
            // Lua Script 실행 (Status update + Queue Management)
            redisTemplate.execute(
                    updateRobotInfoScript,
                    List.of(key, AVAILABLE_ROBOTS_KEY), // Keys
                    String.valueOf(robotId),         // Argv[1]
                    newStatus.name(),                // Argv[2] (Status)
                    "",                              // Argv[3] (Battery는 비워둠 -> Lua에서 무시됨)
                    LocalDateTime.now().toString()   // Argv[4]
            );
        } catch (Exception e) {
            log.error("상태 동기화 실패", e);
            throw e;
        }
    }

    // battery 만 update
    public void updateBatteryOnly(Long robotId, int battery) {
        String key = ROBOT_STATUS_PREFIX + robotId;

        try {

            Map<String, String> updates = Map.of(
                    "battery", String.valueOf(battery),
                    "updatedAt", LocalDateTime.now().toString()
            );

            redisTemplate.opsForHash().putAll(key, updates);

        } catch (Exception e) {
            // 배터리 업데이트 실패는 치명적이지 않음 (다음 1초 뒤에 또 오니까)
            log.warn("배터리 정보 갱신 실패 (무시 가능): {}", e.getMessage());
        }
    }

    // 로봇 실시간 상태 조회
    public Optional<RobotRealTimeInfo> findById(Long robotId) {
        String key = ROBOT_STATUS_PREFIX + robotId;
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(key);

        if (entries.isEmpty()) return Optional.empty();

        // Map -> Object 변환
        return Optional.of(objectMapper.convertValue(entries, RobotRealTimeInfo.class));
    }

}
