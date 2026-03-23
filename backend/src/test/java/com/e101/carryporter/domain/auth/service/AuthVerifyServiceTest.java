package com.e101.carryporter.domain.auth.service;

import com.e101.carryporter.domain.auth.repository.UserPasswordRedisRepository;
import com.e101.carryporter.domain.auth.service.dto.request.VerifyPasswordServiceRequestDto;
import com.e101.carryporter.domain.location.entity.Location;
import com.e101.carryporter.domain.location.repository.LocationRepository;
import com.e101.carryporter.domain.mission.entity.Mission;
import com.e101.carryporter.domain.mission.entity.MissionStatus;
import com.e101.carryporter.domain.mission.repository.MissionRepository;
import com.e101.carryporter.domain.robot.entity.Robot;
import com.e101.carryporter.domain.robot.repository.RobotRepository; // ✅ 로봇 리포지토리 추가
import com.e101.carryporter.domain.user.entity.User;
import com.e101.carryporter.domain.user.event.UserAuthSuccessEvent;
import com.e101.carryporter.domain.user.repository.UserRepository;
import com.e101.carryporter.support.IntegrationTestSupport;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
class UserAuthServiceTest extends IntegrationTestSupport {

    @Autowired AuthService userAuthService;
    @Autowired UserRepository userRepository;
    @Autowired MissionRepository missionRepository;
    @Autowired LocationRepository locationRepository;
    @Autowired RobotRepository robotRepository; // ✅ 주입
    @Autowired UserPasswordRedisRepository userPasswordRedisRepository;
    @Autowired RedisTemplate<String, String> redisTemplate;
    @Autowired EntityManager em;

    @AfterEach
    void tearDown() {
        Optional.ofNullable(redisTemplate.getConnectionFactory())
                .map(RedisConnectionFactory::getConnection)
                .ifPresent(conn -> conn.serverCommands().flushDb());
    }

    @DisplayName("로봇이 도착(ARRIVED) 상태일 때 비밀번호가 일치하면 인증 성공 이벤트가 발행된다.")
    @Test
    void verifyPassword_Success() {
        // given
        // 1. 유저 저장
        User user = User.builder()
                .mmEmail("robot_auth@ssafy.com")
                .role(com.e101.carryporter.domain.user.entity.Role.BASIC)
                .build();
        Long userId = userRepository.save(user);

        // 2. 위치 저장
        Location location = Location.createLocation("Loc-" + UUID.randomUUID(), "테스트");
        locationRepository.save(location);

        // 3. 로봇 저장 (Robot.createRobot 팩토리 메서드 사용)
        Robot robot = Robot.createRobot("R-001", "AA:BB:CC:DD:EE:01");
        robotRepository.save(robot); // ✅ 리포지토리 사용

        // 4. 미션 저장 (ReflectionTestUtils로 로봇 강제 주입)
        Mission mission = Mission.builder()
                .user(user)
                .callLocation(location)
                .missionStatus(MissionStatus.ARRIVED)
                .build();

        // 엔티티 수정 없이 로봇 필드에 값 주입 ("robot"은 필드명)
        ReflectionTestUtils.setField(mission, "robot", robot);

        Long missionId = missionRepository.save(mission);

        // 5. Redis 비밀번호 설정
        userPasswordRedisRepository.save(userId, 1234);

        // 영속성 컨텍스트 초기화 (실제 DB 조회 테스트를 위해)
        flushAndClear();

        VerifyPasswordServiceRequestDto request = new VerifyPasswordServiceRequestDto(
                userId,
                missionId,
                1234
        );

        // when
        userAuthService.unlockRequest(request);

        // then
        long eventCount = events.stream(UserAuthSuccessEvent.class).count();
        assertThat(eventCount).isEqualTo(1);
    }

    @DisplayName("비밀번호가 틀리면 예외가 발생한다.")
    @Test
    void verifyPassword_Fail_WrongPassword() {
        // given
        User user = User.builder()
                .mmEmail("fail@ssafy.com")
                .role(com.e101.carryporter.domain.user.entity.Role.BASIC)
                .build();
        Long userId = userRepository.save(user);

        Location location = Location.createLocation("Loc-Fail", "desc");
        locationRepository.save(location);

        // 로봇 저장
        Robot robot = Robot.createRobot("R-002", "AA:BB:CC:DD:EE:02");
        robotRepository.save(robot);

        // 미션 저장 (로봇 주입)
        Mission mission = Mission.builder()
                .user(user)
                .callLocation(location)
                .missionStatus(MissionStatus.ARRIVED)
                .build();
        ReflectionTestUtils.setField(mission, "robot", robot);

        Long missionId = missionRepository.save(mission);

        userPasswordRedisRepository.save(userId, 1234); // 정답: 1234

        flushAndClear();

        VerifyPasswordServiceRequestDto request = new VerifyPasswordServiceRequestDto(
                userId,
                missionId,
                9999 // 오답: 9999
        );

        // when & then
        assertThatThrownBy(() -> userAuthService.unlockRequest(request))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private void flushAndClear() {
        em.flush();
        em.clear();
    }
}
