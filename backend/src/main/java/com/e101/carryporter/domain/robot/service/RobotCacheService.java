package com.e101.carryporter.domain.robot.service;

import com.e101.carryporter.domain.robot.entity.Robot;
import com.e101.carryporter.domain.robot.entity.RobotRealTimeInfo;
import com.e101.carryporter.domain.robot.entity.RobotStatus;
import com.e101.carryporter.domain.robot.exception.RobotErrorCode;
import com.e101.carryporter.domain.robot.repository.RobotAvailableQueueRepository;
import com.e101.carryporter.domain.robot.repository.RobotMacMappingRepository;
import com.e101.carryporter.domain.robot.repository.RobotRealTimeRepository;
import com.e101.carryporter.domain.robot.repository.RobotRepository;
import com.e101.carryporter.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * 로봇 Redis 캐시 관리 서비스
 * Cache Aside 전략을 적용하여 Redis 캐시를 담당
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class RobotCacheService {

    private final RobotRepository robotRepository;
    private final RobotMacMappingRepository macMappingRepository;
    private final RobotRealTimeRepository realTimeRepository;
    private final RobotAvailableQueueRepository queueRepository;

    /**
     * MAC 주소로 로봇 ID 조회 (Cache Aside 전략)
     * 1. Redis에서 조회
     * 2. 없으면 DB 조회 후 Redis에 캐싱
     */
    public Long getRobotIdByMacAddress(String macAddress) {
        // 1. Redis 캐시에서 조회
        Optional<Long> cachedRobotId = macMappingRepository.findByMacAddress(macAddress);
        if (cachedRobotId.isPresent()) {
            log.debug("캐시 히트: MAC={}, robotId={}", macAddress, cachedRobotId.get());
            return cachedRobotId.get();
        }

        // 2. 캐시 미스 - DB에서 조회
        log.debug("캐시 미스: MAC={}, DB 조회 시도", macAddress);
        Robot robot = robotRepository.findByMacAddress(macAddress)
                .orElseThrow(() -> new BusinessException(RobotErrorCode.ROBOT_NOT_FOUND));

        // 3. Redis에 캐싱
        macMappingRepository.save(macAddress, robot.getId());
        log.info("캐시 저장 완료: MAC={}, robotId={}", macAddress, robot.getId());

        return robot.getId();
    }

    /**
     * 로봇 실시간 상태 조회 (Cache Aside 전략)
     * 1. Redis에서 조회
     * 2. 없으면 DB 조회 후 Redis에 초기 상태 등록
     */
    public RobotRealTimeInfo getRealTimeInfo(Long robotId) {
        // 1. Redis에서 조회
        Optional<RobotRealTimeInfo> cached = realTimeRepository.findById(robotId);
        if (cached.isPresent()) {
            log.debug("실시간 상태 캐시 히트: robotId={}", robotId);
            return cached.get();
        }

        // 2. 캐시 미스 - DB에서 조회 후 Redis 초기화
        log.debug("실시간 상태 캐시 미스: robotId={}, DB 조회 및 초기화", robotId);
        Robot robot = robotRepository.findById(robotId)
                .orElseThrow(() -> new BusinessException(RobotErrorCode.ROBOT_NOT_FOUND));

        // 3. Redis에 초기 상태 등록 (기본값으로 초기화)
        RobotRealTimeInfo initialInfo = RobotRealTimeInfo.builder()
                .macAddress(robot.getMacAddress())
                .status(robot.getRobotStatus())
                .battery(100) // 기본값
                .build();

        realTimeRepository.registerRobotStatus(robotId, initialInfo);
        log.info("실시간 상태 초기화 완료: robotId={}, status={}", robotId, robot.getRobotStatus());

        return initialInfo;
    }

    /**
     * 로봇 상태 업데이트 (Redis 동기화)
     * 주의: 상태의 Source of Truth는 DB이므로, DB 업데이트 후 이 메서드로 Redis 동기화
     */
    public void updateRobotStatus(Long robotId, RobotStatus newStatus) {
        realTimeRepository.updateStatusOnly(robotId, newStatus);
        log.info("로봇 상태 Redis 동기화: robotId={}, status={}", robotId, newStatus);
    }

    /**
     * 로봇 배터리 업데이트
     * Redis만 업데이트 (실시간 배터리는 Redis가 Source of Truth)
     */
    public void updateBattery(Long robotId, int battery) {
        realTimeRepository.updateBatteryOnly(robotId, battery);
        log.debug("배터리 업데이트: robotId={}, battery={}%", robotId, battery);
    }

    /**
     * Redis에 MAC 매핑 저장
     */
    public void saveMacMapping(String macAddress, Long robotId) {
        macMappingRepository.save(macAddress, robotId);
        log.info("MAC 매핑 캐시 저장: MAC={}, robotId={}", macAddress, robotId);
    }

    /**
     * Redis에 로봇 실시간 상태 등록
     */
    public void registerRobotStatus(Long robotId, RobotRealTimeInfo realTimeInfo) {
        realTimeRepository.registerRobotStatus(robotId, realTimeInfo);
        log.info("실시간 상태 등록: robotId={}, status={}", robotId, realTimeInfo.getStatus());
    }

    /**
     * 가용 로봇 획득
     * Redis Queue에서 원자적으로 획득
     */
    public Optional<Long> acquireAvailableRobot() {
        Optional<Long> robotId = queueRepository.acquireRobotId();
        robotId.ifPresent(id -> log.info("가용 로봇 획득: robotId={}", id));
        return robotId;
    }

    /**
     * 로봇을 가용 상태로 복구
     * 상태를 IDLE로 변경하면 Lua Script가 자동으로 큐에 추가
     */
    public void releaseRobot(Long robotId) {
        updateRobotStatus(robotId, RobotStatus.IDLE);
        log.info("로봇 가용 상태 복구: robotId={}", robotId);
    }

    /**
     * MAC 매핑 캐시 무효화
     * 로봇 정보 변경 시 호출
     */
    public void invalidateMacMapping(String macAddress) {
        macMappingRepository.delete(macAddress);
        log.info("MAC 매핑 캐시 무효화: MAC={}", macAddress);
    }
}
