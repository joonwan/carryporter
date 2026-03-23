package com.e101.carryporter.domain.robot.repository;

import com.e101.carryporter.domain.robot.entity.RobotStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@Slf4j
@RequiredArgsConstructor
public class RobotAvailableQueueRepository {

    private static final String AVAILABLE_ROBOTS_KEY = "robot:available";
    private static final String ROBOT_STATUS_PREFIX = "robot:status:";

    private final RedisTemplate<String, Object> redisTemplate;
    private final RedisScript<Long> acquireRobotScript;

    public Optional<Long> acquireRobotId() {
        try {
            Long robotId = redisTemplate.execute(
                    acquireRobotScript,
                    List.of(AVAILABLE_ROBOTS_KEY),
                    ROBOT_STATUS_PREFIX,
                    RobotStatus.BUSY.name(),
                    LocalDateTime.now().toString()
            );

            if (robotId == null) {
                return Optional.empty();
            }

            return Optional.of(robotId);
        } catch (Exception e) {
            log.error("로봇 획득 실패", e);
            throw e;
        }
    }

    /**
     * 현재 가용한 로봇의 수를 반환합니다.
     * @return 가용 로봇 수
     */
    public Long getAvailableRobotCount() {
        try {
            // LLEN robot:available
            return redisTemplate.opsForList().size(AVAILABLE_ROBOTS_KEY);
        } catch (Exception e) {
            log.error("가용 로봇 수 조회 실패", e);
            return 0L; // 오류 발생 시 0을 반환
        }
    }
}
