package com.e101.carryporter.domain.robot.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@Slf4j
@RequiredArgsConstructor
public class RobotMacMappingRepository {

    private static final String MAC_TO_PK_PREFIX = "robot:mac:";

    private final RedisTemplate<String, Object> redisTemplate;

    // mac - pk 저장 메서드
    public void save(String macAddress, Long robotId) {
        try {
            redisTemplate.opsForValue().set(getKey(macAddress), robotId.toString());
            log.debug("mac 매핑 저장: macAddress={}, robotId={}", macAddress, robotId);
        } catch (Exception e) {
            log.error("mac 매핑 저장 실패 : macAddress = {}, robotId = {}", macAddress, robotId, e);
            throw e;
        }
    }

    // mac address 기반 pk 조회 메서드
    public Optional<Long> findByMacAddress(String macAddress) {
        try {
            return Optional.ofNullable(redisTemplate.opsForValue().get(getKey(macAddress)))
                    .map(v -> Long.valueOf(v.toString()));
        } catch (Exception e) {
            log.error("mac address 기반 pk 조회 실패 macAddress = {}", macAddress, e);
            throw e;
        }
    }

    // mac 주소 기반 pk 삭제
    public void delete(String macAddress) {
        try {
            redisTemplate.delete(getKey(macAddress));
            log.debug("mac 매핑 삭제: macAddress={}", macAddress);
        } catch (Exception e) {
            log.error("mac address 기반 pk 삭제 실패: macAddress = {}", macAddress, e);
        }

    }

    private String getKey(String macAddress) {
        return MAC_TO_PK_PREFIX + macAddress;
    }
}
