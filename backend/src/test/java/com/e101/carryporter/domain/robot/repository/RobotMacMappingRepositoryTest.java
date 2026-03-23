package com.e101.carryporter.domain.robot.repository;

import com.e101.carryporter.support.IntegrationTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class RobotMacMappingRepositoryTest extends IntegrationTestSupport {

    @Autowired
    RobotMacMappingRepository robotMacMappingRepository;

    @Autowired
    RedisTemplate<String, Object> redisTemplate;

    @AfterEach
    void tearDown() {
        Optional.ofNullable(redisTemplate.getConnectionFactory())
                .map(RedisConnectionFactory::getConnection)
                .ifPresent(conn -> conn.serverCommands().flushDb());
    }

    @DisplayName("MAC 주소와 Robot ID를 매핑하여 저장하고 조회할 수 있다")
    @Test
    void save() {
        // given
        String macAddress = "AA:BB:CC:DD";
        Long robotId = 1L;

        // when
        robotMacMappingRepository.save(macAddress, robotId);
        Optional<Long> result = robotMacMappingRepository.findByMacAddress(macAddress);

        // then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(robotId);
    }

    @DisplayName("같은 MAC 주소에 여러 Robot ID를 저장하면 마지막 값으로 덮어쓴다")
    @Test
    void saveOverwrite() {
        // given
        String macAddress = "AA:BB:CC:DD";
        Long firstId = 1L;
        Long secondId = 2L;

        // when
        robotMacMappingRepository.save(macAddress, firstId);
        robotMacMappingRepository.save(macAddress, secondId);
        Optional<Long> result = robotMacMappingRepository.findByMacAddress(macAddress);

        // then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(secondId);
    }

    @DisplayName("존재하지 않는 MAC 주소로 조회하면 빈 Optional을 반환한다")
    @Test
    void findByMacAddressNotFound() {
        // given
        String notExistMacAddress = "AA:BB:CC:DD";

        // when
        Optional<Long> result = robotMacMappingRepository.findByMacAddress(notExistMacAddress);

        // then
        assertThat(result).isEmpty();
    }

    @DisplayName("MAC 주소 매핑을 삭제할 수 있다")
    @Test
    void delete() {
        // given
        String macAddress = "AA:BB:CC:DD";
        Long robotId = 1L;
        robotMacMappingRepository.save(macAddress, robotId);

        // when
        robotMacMappingRepository.delete(macAddress);
        Optional<Long> result = robotMacMappingRepository.findByMacAddress(macAddress);

        // then
        assertThat(result).isEmpty();
    }

    @DisplayName("존재하지 않는 MAC 주소를 삭제해도 예외가 발생하지 않는다")
    @Test
    void deleteNotFound() {
        // given
        String notExistMacAddress = "AA:BB:CC:DD";

        // when & then (예외 없이 정상 실행)
        robotMacMappingRepository.delete(notExistMacAddress);
    }
}
