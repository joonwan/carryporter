package com.e101.carryporter.domain.robot.repository;

import com.e101.carryporter.domain.robot.entity.Robot;
import com.e101.carryporter.domain.robot.entity.RobotStatus;
import com.e101.carryporter.support.IntegrationTestSupport;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class RobotRepositoryTest extends IntegrationTestSupport {

    @Autowired
    RobotRepository robotRepository;

    @Autowired
    EntityManager em;

    @AfterEach
    void tearDown() {
        robotRepository.clearAll();
    }

    @DisplayName("로봇을 저장할 수 있다.")
    @Test
    void saveRobot() {
        // given
        Robot robot = Robot.createRobot("R-001", "AA:BB:CC:DD:EE:FF");

        // when
        Long savedId = robotRepository.save(robot);
        flushAndClear();

        Robot findRobot = robotRepository.findById(savedId)
                .orElseThrow(() -> new EntityNotFoundException("Robot not found"));

        // then
        assertThat(findRobot.getRobotCode()).isEqualTo(robot.getRobotCode());
        assertThat(findRobot.getMacAddress()).isEqualTo(robot.getMacAddress());
        assertThat(findRobot.getRobotStatus()).isEqualTo(RobotStatus.IDLE);
    }

    @DisplayName("존재하지 않는 로봇의 pk 로 조회시 빈 옵셔널이 반환된다")
    @Test
    void findByNotExistId() {
        // given
        Long notExistRobotId = 99999L;

        // when
        Optional<Robot> robotOpt = robotRepository.findById(notExistRobotId);

        // then
        assertThat(robotOpt).isEmpty();
    }

    @DisplayName("MAC 주소로 로봇을 조회할 수 있다")
    @Test
    void findByMacAddress() {
        // given
        String macAddress = "AA:BB:CC:DD:EE:FF";
        Robot robot = Robot.createRobot("R-001", macAddress);
        robotRepository.save(robot);
        flushAndClear();

        // when
        Optional<Robot> findRobot = robotRepository.findByMacAddress(macAddress);

        // then
        assertThat(findRobot).isPresent();
        assertThat(findRobot.get().getMacAddress()).isEqualTo(macAddress);
        assertThat(findRobot.get().getRobotCode()).isEqualTo("R-001");
    }

    @DisplayName("존재하지 않는 MAC 주소로 조회시 빈 옵셔널이 반환된다")
    @Test
    void findByNotExistMacAddress() {
        // given
        String notExistMacAddress = "00:00:00:00:00:00";

        // when
        Optional<Robot> robotOpt = robotRepository.findByMacAddress(notExistMacAddress);

        // then
        assertThat(robotOpt).isEmpty();
    }

    private void flushAndClear() {
        em.flush();
        em.clear();
    }

}
