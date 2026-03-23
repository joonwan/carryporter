package com.e101.carryporter.domain.robot.service;

import com.e101.carryporter.domain.admin.event.AdminLockRequestEvent;
import com.e101.carryporter.domain.admin.event.AdminUnlockRequestEvent;
import com.e101.carryporter.domain.location.entity.Location;
import com.e101.carryporter.domain.location.repository.LocationRepository;
import com.e101.carryporter.domain.locker.entity.Locker;
import com.e101.carryporter.domain.locker.repository.LockerRepository;
import com.e101.carryporter.domain.mission.entity.MissionStatus;
import com.e101.carryporter.domain.mission.entity.Mission;
import com.e101.carryporter.domain.mission.event.MissionFinalizedEvent;
import com.e101.carryporter.domain.mission.event.MissionStartedEvent;
import com.e101.carryporter.domain.mission.repository.MissionRepository;
import com.e101.carryporter.domain.mission.service.MissionService;
import com.e101.carryporter.domain.robot.entity.Robot;
import com.e101.carryporter.domain.robot.entity.RobotRealTimeInfo;
import com.e101.carryporter.domain.robot.entity.RobotStatus;
import com.e101.carryporter.domain.robot.event.RobotAssignedEvent;
import com.e101.carryporter.domain.robot.repository.RobotRepository;
import com.e101.carryporter.domain.user.entity.User;
import com.e101.carryporter.domain.user.repository.UserRepository;
import com.e101.carryporter.global.exception.BusinessException;
import com.e101.carryporter.support.IntegrationTestSupport;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.event.ApplicationEvents;
import java.util.Optional;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RobotServiceTest extends IntegrationTestSupport {

    @Autowired
    RobotRepository robotRepository;

    @Autowired
    RobotService robotService;

    @Autowired
    MissionService missionService;

    @Autowired
    UserRepository userRepository;

    @Autowired
    MissionRepository missionRepository;

    @Autowired
    LocationRepository locationRepository;

    @Autowired
    LockerRepository lockerRepository;

    @Autowired
    EntityManager em;

    @Autowired
    ApplicationEvents events;

    @Autowired
    RobotCacheService cacheService;

    @Autowired
    RedisTemplate<String, Object> redisTemplate;

    @AfterEach
    void tearDown() {
        // Redis 전체 삭제
        redisTemplate.getConnectionFactory().getConnection().flushAll();

    }

    @DisplayName("로봇을 pk 기반으로 조회할 수 있다.")
    @Test
    void findById() {
        // given
        Robot robot = Robot.createRobot("test code", "aa:bb:cc");
        robotRepository.save(robot);

        flushAndClear();

        // when
        Robot findRobot = robotService.findById(robot.getId());

        // then
        assertThat(findRobot.getRobotCode()).isEqualTo(robot.getRobotCode());
        assertThat(findRobot.getMacAddress()).isEqualTo(robot.getMacAddress());
    }

    @DisplayName("로봇이 없을 경우 예외가 발생한다.")
    @Test
    void findByNotExistsRobotId() {
        // given
        Long notExistsRobotId = 9999L;

        // when then
        assertThatThrownBy(() -> robotService.findById(notExistsRobotId))
                .isInstanceOf(BusinessException.class)
                .hasMessage("해당 로봇을 찾을 수 없습니다.");
    }

    @DisplayName("관리자 잠금 해제 요청이 들어올 경우 잠금해제 할 수 있다.")
    @Test
    void unlockByAdmin() {
        // given
        User user = User.createUser("test@mm.com");
        userRepository.save(user);

        Robot robot = Robot.createRobot("test code", "aa:bb:cc");
        robotRepository.save(robot);

        Location callLocation = Location.createLocation("test location", "description");
        locationRepository.save(callLocation);

        Mission mission = Mission.createMission(user, callLocation);
        mission.assignRobot(robot);

        missionRepository.save(mission);

        flushAndClear();

        // when
        robotService.unlockByAdmin(mission.getId());

        // then
        long publishedCount = events.stream(AdminUnlockRequestEvent.class).count();
        assertThat(publishedCount).isEqualTo(1);

        AdminUnlockRequestEvent publishedEvent = events.stream(AdminUnlockRequestEvent.class)
                .findFirst()
                .orElseThrow();

        assertThat(publishedEvent.missionId()).isEqualTo(mission.getId());
        assertThat(publishedEvent.robotMacAddress()).isEqualTo(robot.getMacAddress());
    }
    @DisplayName("관리자 잠금 요청이 들어올 경우 잠금 할 수 있다.")
    @Test
    void lockByAdmin() {
        // given
        User user = User.createUser("test@mm.com");
        userRepository.save(user);

        Robot robot = Robot.createRobot("test code", "aa:bb:cc");
        robotRepository.save(robot);

        Location callLocation = Location.createLocation("test location", "description");
        locationRepository.save(callLocation);

        Mission mission = Mission.createMission(user, callLocation);

        mission.assignRobot(robot);

        missionRepository.save(mission);

        flushAndClear();

        // when
        System.out.println("robot id " + robot.getId());
        robotService.lockByAdmin(mission.getId());

        // then
        long publishedCount = events.stream(AdminLockRequestEvent.class).count();
        assertThat(publishedCount).isEqualTo(1);

        AdminLockRequestEvent publishedEvent = events.stream(AdminLockRequestEvent.class)
                .findFirst()
                .orElseThrow();

        assertThat(publishedEvent.missionId()).isEqualTo(mission.getId());
        assertThat(publishedEvent.robotMacAddress()).isEqualTo(robot.getMacAddress());
    }

    @DisplayName("관리자 권한 이동 요청이 들어올 경우 MissionStartedEvent 가 발행된다.")
    @Test
    void dispatch() {
        // given
        User user = User.createUser("test@mm.com");
        userRepository.save(user);

        Robot robot = Robot.createRobot("test code", "aa:bb:cc");
        robotRepository.save(robot);

        Location callLocation = Location.createLocation("test location", "description");
        locationRepository.save(callLocation);

        Mission mission = Mission.createMission(user, callLocation);

        // ✅ [필수 추가] 서비스 로직에서 mission.getRobot()을 사용하므로, 미션에 로봇을 할당해야 합니다.
        // Mission 엔티티의 로봇 할당 메서드 이름에 맞춰주세요 (예: assignRobot, setRobot, updateRobot 등)
        mission.assignRobot(robot);

        missionRepository.save(mission);

        flushAndClear();

        // when
        // ✅ [변경] DTO 대신 missionId만 전달
        robotService.dispatch(mission.getId());

        // then
        long publishedCount = events.stream(MissionStartedEvent.class).count();
        assertThat(publishedCount).isEqualTo(1);

        MissionStartedEvent publishedEvent = events.stream(MissionStartedEvent.class)
                .findFirst()
                .orElseThrow();

        assertThat(publishedEvent.missionId()).isEqualTo(mission.getId());
        assertThat(publishedEvent.robotMacAddress()).isEqualTo(robot.getMacAddress());
        // 이벤트 생성자에서 user id와 robotCode도 확인 가능
        assertThat(publishedEvent.robotCode()).isEqualTo(robot.getRobotCode());
    }

    @DisplayName("관리자 최종 점검 완료 시 MissionFinalizedEvent가 발행된다.")
    @Test
    void finalizeMission() {
        // given
        User user = User.createUser("test@mm.com");
        userRepository.save(user);

        Robot robot = Robot.createRobot("test code", "aa:bb:cc");
        robotRepository.save(robot);

        Location callLocation = Location.createLocation("test location", "description");
        locationRepository.save(callLocation);

        Mission mission = Mission.createMission(user, callLocation);
        mission.assignRobot(robot);

        missionRepository.save(mission);

        mission.assignRobot(robot);
        flushAndClear();

        // when
        robotService.finalizeMission(mission.getId());

        // then
        long publishedCount = events.stream(MissionFinalizedEvent.class).count();
        assertThat(publishedCount).isEqualTo(1);

        MissionFinalizedEvent publishedEvent = events.stream(MissionFinalizedEvent.class)
                .findFirst()
                .orElseThrow();

        assertThat(publishedEvent.missionId()).isEqualTo(mission.getId());
        assertThat(publishedEvent.robotId()).isEqualTo(robot.getId());
    }

    @DisplayName("존재하지 않는 로봇으로 finalizeMission 호출 시 예외가 발생한다.")
    @Test
    void finalizeMissionWithNotExistsRobot() {
        // given
        Long missionId = 1L;
        Long notExistsRobotId = 9999L;

        // when then
        assertThatThrownBy(() -> robotService.finalizeMission(missionId))
                .isInstanceOf(BusinessException.class)
                .hasMessage("해당 미션을 찾을 수 없습니다.");
    }

    @DisplayName("새 미션에 로봇을 배정하면 RobotAssignedEvent가 FIRST 타입으로 발행된다.")
    @Test
    void assignRobotToMission_WithNewMission_ShouldPublishRobotAssignedEventWithFIRST() {
        // given
        User user = User.createUser("test@mm.com");
        userRepository.save(user);

        Robot robot = Robot.createRobot("e101-TEST01", "AA:BB:CC:DD:EE:FF");
        robotRepository.save(robot);

        Location callLocation = Location.createLocation("Gate A12", "탑승구 A12");
        locationRepository.save(callLocation);

        Mission mission = Mission.createMission(user, callLocation);
        Long missionId = missionRepository.save(mission);

        // Redis에 로봇 등록
        cacheService.saveMacMapping(robot.getMacAddress(), robot.getId());
        cacheService.registerRobotStatus(robot.getId(), RobotRealTimeInfo.builder()
                .macAddress(robot.getMacAddress())
                .status(RobotStatus.IDLE)
                .battery(100)
                .build());

        flushAndClear();

        // when - isNew = true
        robotService.assignRobotToMission(missionId, user.getId(), true);

        // then
        long publishedCount = events.stream(RobotAssignedEvent.class).count();
        assertThat(publishedCount).isEqualTo(1);

        RobotAssignedEvent publishedEvent = events.stream(RobotAssignedEvent.class)
                .findFirst()
                .orElseThrow();

        assertThat(publishedEvent.userId()).isEqualTo(user.getId());
        assertThat(publishedEvent.missionId()).isEqualTo(missionId);
        assertThat(publishedEvent.robotCode()).isEqualTo(robot.getRobotCode());
        assertThat(publishedEvent.callLocationName()).isEqualTo(callLocation.getLocationName());
        assertThat(publishedEvent.lockerCode()).isNull(); // 새 미션이므로 null
        assertThat(publishedEvent.requestType()).isEqualTo("FIRST"); // 새 미션이므로 FIRST
    }

    @DisplayName("STORING 상태 미션에 로봇을 배정하면 RobotAssignedEvent가 RECALL 타입으로 발행된다.")
    @Test
    void assignRobotToMission_WithStoringMission_ShouldPublishRobotAssignedEventWithRECALL() {
        // given
        User user = User.createUser("test@mm.com");
        userRepository.save(user);

        Robot robot = Robot.createRobot("e101-TEST02", "BB:CC:DD:EE:FF:AA");
        robotRepository.save(robot);

        Location callLocation = Location.createLocation("Gate B5", "탑승구 B5");
        locationRepository.save(callLocation);

        Locker locker = Locker.createLocker("L001");
        lockerRepository.save(locker);

        Mission mission = Mission.createMission(user, callLocation);
        mission.assignLocker(locker); // 보관함 할당
        Long missionId = missionRepository.save(mission);

        // 미션을 STORING 상태로 변경
        em.createQuery("UPDATE Mission m SET m.missionStatus = :status WHERE m.id = :id")
                .setParameter("status", MissionStatus.STORING)
                .setParameter("id", missionId)
                .executeUpdate();

        // Redis에 로봇 등록
        cacheService.saveMacMapping(robot.getMacAddress(), robot.getId());
        cacheService.registerRobotStatus(robot.getId(), RobotRealTimeInfo.builder()
                .macAddress(robot.getMacAddress())
                .status(RobotStatus.IDLE)
                .battery(100)
                .build());

        flushAndClear();

        // when - isNew = false
        robotService.assignRobotToMission(missionId, user.getId(), false);

        // then
        long publishedCount = events.stream(RobotAssignedEvent.class).count();
        assertThat(publishedCount).isEqualTo(1);

        RobotAssignedEvent publishedEvent = events.stream(RobotAssignedEvent.class)
                .findFirst()
                .orElseThrow();

        assertThat(publishedEvent.userId()).isEqualTo(user.getId());
        assertThat(publishedEvent.missionId()).isEqualTo(missionId);
        assertThat(publishedEvent.robotCode()).isEqualTo(robot.getRobotCode());
        assertThat(publishedEvent.callLocationName()).isEqualTo(callLocation.getLocationName());
        assertThat(publishedEvent.lockerCode()).isEqualTo(locker.getLockerCode()); // 보관 중이므로 lockerCode 존재
        assertThat(publishedEvent.requestType()).isEqualTo("RECALL"); // 재호출이므로 RECALL
    }

    // ==================== registerRobot 테스트 ====================

    @DisplayName("신규 로봇 등록 시 DB와 Redis 캐시가 모두 생성된다")
    @Test
    void registerNewRobot() {
        // given
        String macAddress = "AA:BB:CC:DD:EE:FF";

        // when
        Robot robot = robotService.registerRobot(macAddress);
        flushAndClear();

        // then - DB 저장 확인
        Robot savedRobot = robotRepository.findById(robot.getId()).orElseThrow();
        assertThat(savedRobot.getMacAddress()).isEqualTo(macAddress);
        assertThat(savedRobot.getRobotCode()).startsWith("e101-");
        assertThat(savedRobot.getRobotStatus()).isEqualTo(RobotStatus.IDLE);

        // then - Redis MAC 매핑 확인
        Long cachedRobotId = cacheService.getRobotIdByMacAddress(macAddress);
        assertThat(cachedRobotId).isEqualTo(robot.getId());

        // then - Redis 실시간 상태 확인
        RobotRealTimeInfo realTimeInfo = cacheService.getRealTimeInfo(robot.getId());
        assertThat(realTimeInfo.getMacAddress()).isEqualTo(macAddress);
        assertThat(realTimeInfo.getStatus()).isEqualTo(RobotStatus.IDLE);
        assertThat(realTimeInfo.getBattery()).isEqualTo(100);

        // then - Redis 가용 로봇 큐 확인 (IDLE 상태면 자동 추가)
        Optional<Long> availableRobotId = cacheService.acquireAvailableRobot();
        assertThat(availableRobotId).isPresent();
        assertThat(availableRobotId.get()).isEqualTo(robot.getId());
    }

    @DisplayName("이미 등록된 로봇을 재등록하면 기존 로봇이 반환되고 캐시가 동기화된다")
    @Test
    void registerExistingRobot() {
        // given - 기존 로봇 생성
        String macAddress = "AA:BB:CC:DD:EE:FF";
        Robot existingRobot = Robot.createRobot("e101-EXISTING", macAddress);
        robotRepository.save(existingRobot);
        flushAndClear();

        // when - 같은 MAC 주소로 재등록
        Robot registeredRobot = robotService.registerRobot(macAddress);

        // then - 기존 로봇 반환 확인
        assertThat(registeredRobot.getId()).isEqualTo(existingRobot.getId());
        assertThat(registeredRobot.getRobotCode()).isEqualTo("e101-EXISTING");

        // then - Redis 캐시 동기화 확인
        Long cachedRobotId = cacheService.getRobotIdByMacAddress(macAddress);
        assertThat(cachedRobotId).isEqualTo(existingRobot.getId());

        RobotRealTimeInfo realTimeInfo = cacheService.getRealTimeInfo(existingRobot.getId());
        assertThat(realTimeInfo.getMacAddress()).isEqualTo(macAddress);
        assertThat(realTimeInfo.getStatus()).isEqualTo(RobotStatus.IDLE);
    }

//    @DisplayName("동일 MAC 주소로 동시 등록 시도 시 하나의 로봇만 생성된다")
//    @Test
//    void registerRobotConcurrently() throws InterruptedException {
//        // given
//        String macAddress = "AA:BB:CC:DD:EE:FF";
//        int threadCount = 5;
//
//        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
//        CountDownLatch latch = new CountDownLatch(threadCount);
//        List<Future<Robot>> futures = new ArrayList<>();
//
//        // when - 동시에 5개 스레드에서 같은 MAC 주소로 등록
//        for (int i = 0; i < threadCount; i++) {
//            Future<Robot> future = executorService.submit(() -> {
//                try {
//                    latch.countDown();
//                    latch.await(); // 모든 스레드가 동시에 시작
//                    return robotService.registerRobot(macAddress);
//                } catch (Exception e) {
//                    throw new RuntimeException(e);
//                }
//            });
//            futures.add(future);
//        }
//
//        // then - 모든 요청이 성공적으로 완료
//        List<Robot> robots = new ArrayList<>();
//        for (Future<Robot> future : futures) {
//            try {
//                robots.add(future.get(10, TimeUnit.SECONDS));
//            } catch (ExecutionException | TimeoutException e) {
//                throw new RuntimeException(e);
//            }
//        }
//
//        executorService.shutdown();
//        executorService.awaitTermination(10, TimeUnit.SECONDS);
//
//        // then - 모든 스레드가 같은 로봇을 반환
//        assertThat(robots).hasSize(threadCount);
//        Long firstRobotId = robots.get(0).getId();
//        assertThat(robots).allMatch(robot -> robot.getId().equals(firstRobotId));
//
//        // then - DB에는 하나의 로봇만 존재
//        flushAndClear();
//        List<Robot> allRobots = em.createQuery("SELECT r FROM Robot r WHERE r.macAddress = :macAddress", Robot.class)
//                .setParameter("macAddress", macAddress)
//                .getResultList();
//        assertThat(allRobots).hasSize(1);
//
//        // then - Redis 캐시도 정상 동기화
//        Long cachedRobotId = cacheService.getRobotIdByMacAddress(macAddress);
//        assertThat(cachedRobotId).isEqualTo(firstRobotId);
//    }

//    @DisplayName("여러 다른 MAC 주소로 동시 등록 시 모두 정상 등록된다")
//    @Test
//    @Transactional(propagation = Propagation.REQUIRES_NEW)
//    void registerMultipleRobotsConcurrently() throws InterruptedException, ExecutionException, TimeoutException {
//        // given
//        int robotCount = 10;
//        ExecutorService executorService = Executors.newFixedThreadPool(robotCount);
//        CountDownLatch latch = new CountDownLatch(robotCount);
//        List<Future<Robot>> futures = new ArrayList<>();
//
//        // when - 동시에 여러 다른 MAC 주소로 등록
//        for (int i = 0; i < robotCount; i++) {
//            final String macAddress = String.format("AA:BB:CC:DD:EE:%02X", i);
//            Future<Robot> future = executorService.submit(() -> {
//                try {
//                    latch.countDown();
//                    latch.await();
//                    return robotService.registerRobot(macAddress);
//                } catch (Exception e) {
//                    throw new RuntimeException(e);
//                }
//            });
//            futures.add(future);
//        }
//
//        // then - 모든 요청이 성공
//        List<Robot> robots = new ArrayList<>();
//        for (Future<Robot> future : futures) {
//            robots.add(future.get(10, TimeUnit.SECONDS));
//        }
//
//        executorService.shutdown();
//        executorService.awaitTermination(10, TimeUnit.SECONDS);
//
//        // then - 10개의 서로 다른 로봇 생성
//        assertThat(robots).hasSize(robotCount);
//        assertThat(robots.stream().map(Robot::getId).distinct()).hasSize(robotCount);
//
//        // then - 모든 로봇이 Redis 캐시에 등록됨
//        for (int i = 0; i < robotCount; i++) {
//            String macAddress = String.format("AA:BB:CC:DD:EE:%02X", i);
//            Long cachedRobotId = cacheService.getRobotIdByMacAddress(macAddress);
//            assertThat(cachedRobotId).isNotNull();
//        }
//    }

    @DisplayName("Redis 캐시가 없어도 DB에서 조회 후 캐싱된다 (Cache Aside)")
    @Test
    void registerRobotWithoutRedisCache() {
        // given - DB에만 로봇 존재 (Redis 없음)
        String macAddress = "AA:BB:CC:DD:EE:FF";
        Robot existingRobot = Robot.createRobot("e101-NOCACHE", macAddress);
        robotRepository.save(existingRobot);
        flushAndClear();

        // Redis 캐시 삭제 (Cache Miss 상황 시뮬레이션)
        redisTemplate.delete("robot:mac:" + macAddress);

        // when - 재등록 (캐시 없음)
        Robot registeredRobot = robotService.registerRobot(macAddress);

        // then - 기존 로봇 반환
        assertThat(registeredRobot.getId()).isEqualTo(existingRobot.getId());

        // then - Redis 캐시 재생성 확인
        Long cachedRobotId = cacheService.getRobotIdByMacAddress(macAddress);
        assertThat(cachedRobotId).isEqualTo(existingRobot.getId());
    }

    @DisplayName("changeStatusAll 호출 시 모든 로봇이 지정된 상태로 변경되고 RobotAvailabilityChangedEvent가 발행된다.")
    @Test
    void changeStatusAll() {
        // given - 다양한 상태의 로봇 생성
        Robot robot1 = Robot.createRobot("e101-TEST01", "AA:BB:CC:DD:EE:01");
        robot1.changeStatus(RobotStatus.BUSY);
        robotRepository.save(robot1);

        Robot robot2 = Robot.createRobot("e101-TEST02", "AA:BB:CC:DD:EE:02");
        robot2.changeStatus(RobotStatus.OFFLINE);
        robotRepository.save(robot2);

        Robot robot3 = Robot.createRobot("e101-TEST03", "AA:BB:CC:DD:EE:03");
        robot3.changeStatus(RobotStatus.IDLE);
        robotRepository.save(robot3);

        // Redis에 로봇 상태 등록
        cacheService.registerRobotStatus(robot1.getId(), RobotRealTimeInfo.builder()
                .macAddress(robot1.getMacAddress())
                .status(RobotStatus.BUSY)
                .battery(100)
                .build());

        cacheService.registerRobotStatus(robot2.getId(), RobotRealTimeInfo.builder()
                .macAddress(robot2.getMacAddress())
                .status(RobotStatus.OFFLINE)
                .battery(100)
                .build());

        cacheService.registerRobotStatus(robot3.getId(), RobotRealTimeInfo.builder()
                .macAddress(robot3.getMacAddress())
                .status(RobotStatus.IDLE)
                .battery(100)
                .build());

        flushAndClear();

        // when
        robotService.changeStatusAll(RobotStatus.IDLE);
        flushAndClear();

        // then - DB 상태 확인
        Robot updatedRobot1 = robotRepository.findById(robot1.getId()).orElseThrow();
        Robot updatedRobot2 = robotRepository.findById(robot2.getId()).orElseThrow();
        Robot updatedRobot3 = robotRepository.findById(robot3.getId()).orElseThrow();

        assertThat(updatedRobot1.getRobotStatus()).isEqualTo(RobotStatus.IDLE);
        assertThat(updatedRobot2.getRobotStatus()).isEqualTo(RobotStatus.IDLE);
        assertThat(updatedRobot3.getRobotStatus()).isEqualTo(RobotStatus.IDLE);

        // then - 이벤트 발행 확인 (3개의 로봇에 대해 3번 발행)
        long eventCount = events.stream(com.e101.carryporter.domain.robot.event.RobotAvailabilityChangedEvent.class).count();
        assertThat(eventCount).isEqualTo(3);
    }

    private void flushAndClear() {
        em.flush();
        em.clear();
    }
}