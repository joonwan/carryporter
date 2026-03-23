package com.e101.carryporter.domain.mission.repository;

import com.e101.carryporter.domain.location.entity.Location;
import com.e101.carryporter.domain.location.repository.LocationRepository;
import com.e101.carryporter.domain.locker.entity.Locker;
import com.e101.carryporter.domain.locker.entity.UserLockerStatus;
import com.e101.carryporter.domain.mission.entity.Mission;
import com.e101.carryporter.domain.mission.entity.MissionStatus;
import com.e101.carryporter.domain.user.entity.User;
import com.e101.carryporter.domain.user.repository.UserRepository;
import com.e101.carryporter.support.IntegrationTestSupport;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class MissionRepositoryTest extends IntegrationTestSupport {

    @Autowired
    MissionRepository missionRepository;

    @Autowired
    UserRepository userRepository;

    @Autowired
    LocationRepository locationRepository;

    @Autowired
    EntityManager em;

    @DisplayName("미션을 저장할 수 있다.")
    @Test
    void saveMission() {
        // given
        User user = User.createUser("test@mm.com");
        userRepository.save(user);

        Location callLocation = Location.createLocation("Gate A12", "탑승구 A12");
        locationRepository.save(callLocation);

        Mission mission = Mission.createMission(user, callLocation);

        // when
        Long savedId = missionRepository.save(mission);
        flushAndClear();

        Mission findMission = missionRepository.findById(savedId)
                .orElseThrow(() -> new EntityNotFoundException("Mission not found"));

        // then
        assertThat(findMission.getUser().getId()).isEqualTo(user.getId());
        assertThat(findMission.getCallLocation().getId()).isEqualTo(callLocation.getId());
        assertThat(findMission.getMissionStatus()).isEqualTo(MissionStatus.REQUESTED);
    }

    @DisplayName("존재하지 않는 미션의 pk 로 조회시 빈 옵셔널이 반환된다")
    @Test
    void findByNotExistId() {
        // given
        Long notExistMissionId = 99999L;

        // when
        Optional<Mission> missionOpt = missionRepository.findById(notExistMissionId);

        // then
        assertThat(missionOpt).isEmpty();
    }

    @DisplayName("사용자 ID로 미션 목록을 조회한다")
    @Test
    void findByUserId() {
        // given
        User user1 = User.createUser("user1@example.com");
        User user2 = User.createUser("user2@example.com");
        userRepository.save(user1);
        userRepository.save(user2);

        Location location1 = Location.createLocation("Location1", "Test Location 1");
        Location location2 = Location.createLocation("Location2", "Test Location 2");
        locationRepository.save(location1);
        locationRepository.save(location2);

        Locker locker1 = Locker.createLocker("L001");
        Locker locker2 = Locker.createLocker("L002");
        Locker locker3 = Locker.createLocker("L003");
        em.persist(locker1);
        em.persist(locker2);
        em.persist(locker3);

        Mission mission1 = Mission.createMission(user1, location1);
        mission1.assignLocker(locker1);
        Mission mission2 = Mission.createMission(user1, location2);
        mission2.assignLocker(locker2);
        Mission mission3 = Mission.createMission(user2, location1);
        mission3.assignLocker(locker3);

        missionRepository.save(mission1);
        missionRepository.save(mission2);
        missionRepository.save(mission3);
        flushAndClear();

        // when
        List<Mission> missions = missionRepository.findByUserId(user1.getId());

        // then
        assertThat(missions).hasSize(2)
                .extracting("user.id")
                .containsOnly(user1.getId());
    }

    @DisplayName("사용자 ID로 미션을 조회할 때 사물함 정보를 포함한다")
    @Test
    void findByUserIdWithLocker() {
        // given
        User user = User.createUser("user@example.com");
        userRepository.save(user);

        Location location = Location.createLocation("Location1", "Test Location");
        locationRepository.save(location);

        Locker locker = Locker.createLocker("L001");
        em.persist(locker);

        Mission mission = Mission.createMission(user, location);
        mission.assignLocker(locker);
        missionRepository.save(mission);
        flushAndClear();

        // when
        List<Mission> missions = missionRepository.findByUserId(user.getId());

        // then
        assertThat(missions).hasSize(1);
        Mission foundMission = missions.get(0);
        assertThat(foundMission.getLocker()).isNotNull();
        assertThat(foundMission.getLocker().getId()).isEqualTo(locker.getId());
        assertThat(foundMission.getUserLockerStatus()).isEqualTo(UserLockerStatus.OCCUPIED);
        assertThat(foundMission.getLockerAssignedAt()).isNotNull();
    }

    @DisplayName("사용자 ID로 미션 조회 시 해당 사용자의 미션이 없으면 빈 리스트를 반환한다")
    @Test
    void findByUserIdWithNoMissions() {
        // given
        User user = User.createUser("user@example.com");
        userRepository.save(user);
        flushAndClear();

        // when
        List<Mission> missions = missionRepository.findByUserId(user.getId());

        // then
        assertThat(missions).isEmpty();
    }

    @DisplayName("사용자 ID로 미션을 조회할 때 여러 사물함 상태를 가진 미션들을 조회한다")
    @Test
    void findByUserIdWithDifferentLockerStatuses() {
        // given
        User user = User.createUser("user@example.com");
        userRepository.save(user);

        Location location = Location.createLocation("Location1", "Test Location");
        locationRepository.save(location);

        Locker locker1 = Locker.createLocker("L001");
        Locker locker2 = Locker.createLocker("L002");
        Locker locker3 = Locker.createLocker("L003");
        em.persist(locker1);
        em.persist(locker2);
        em.persist(locker3);

        // READY 상태
        Mission mission1 = Mission.createMission(user, location);
        missionRepository.save(mission1);

        // OCCUPIED 상태
        Mission mission2 = Mission.createMission(user, location);
        mission2.assignLocker(locker2);
        missionRepository.save(mission2);

        // COMPLETED 상태
        Mission mission3 = Mission.createMission(user, location);
        mission3.assignLocker(locker3);
        mission3.finish();
        missionRepository.save(mission3);

        flushAndClear();

        // when
        List<Mission> missions = missionRepository.findByUserId(user.getId());

        // then
        assertThat(missions).hasSize(3);
        assertThat(missions)
                .extracting("userLockerStatus")
                .containsExactlyInAnyOrder(
                        UserLockerStatus.READY,
                        UserLockerStatus.OCCUPIED,
                        UserLockerStatus.COMPLETED
                );
    }

    @DisplayName("사용자 ID와 미션 상태로 미션을 조회한다")
    @Test
    void findByUserIdAndMissionStatus() {
        // given
        User user = User.createUser("user@example.com");
        userRepository.save(user);

        Location location = Location.createLocation("Location1", "Test Location");
        locationRepository.save(location);

        Locker locker = Locker.createLocker("L001");
        em.persist(locker);

        Mission mission1 = Mission.createMission(user, location);
        Mission mission2 = Mission.createMission(user, location);
        mission2.assignLocker(locker);
        mission2.finish();

        missionRepository.save(mission1);
        missionRepository.save(mission2);
        flushAndClear();

        // when
        Optional<Mission> foundMission = missionRepository.findByUserIdAndMissionStatus(
                user.getId(),
                MissionStatus.REQUESTED
        );

        // then
        assertThat(foundMission).isPresent();
        assertThat(foundMission.get().getMissionStatus()).isEqualTo(MissionStatus.REQUESTED);
        assertThat(foundMission.get().getUser().getId()).isEqualTo(user.getId());
    }

    @DisplayName("사용자 ID와 미션 상태로 미션을 조회할 때 해당하는 미션이 없으면 빈 Optional을 반환한다")
    @Test
    void findByUserIdAndMissionStatusNotFound() {
        // given
        User user = User.createUser("user@example.com");
        userRepository.save(user);

        Location location = Location.createLocation("Location1", "Test Location");
        locationRepository.save(location);

        Mission mission = Mission.createMission(user, location);
        missionRepository.save(mission);
        flushAndClear();

        // when
        Optional<Mission> foundMission = missionRepository.findByUserIdAndMissionStatus(
                user.getId(),
                MissionStatus.FINISHED
        );

        // then
        assertThat(foundMission).isEmpty();
    }

    @DisplayName("사용자 ID와 미션 상태로 미션을 조회할 때 다른 사용자의 미션은 조회되지 않는다")
    @Test
    void findByUserIdAndMissionStatusWithDifferentUser() {
        // given
        User user1 = User.createUser("user1@example.com");
        User user2 = User.createUser("user2@example.com");
        userRepository.save(user1);
        userRepository.save(user2);

        Location location = Location.createLocation("Location1", "Test Location");
        locationRepository.save(location);

        Mission mission1 = Mission.createMission(user1, location);
        Mission mission2 = Mission.createMission(user2, location);

        missionRepository.save(mission1);
        missionRepository.save(mission2);
        flushAndClear();

        // when
        Optional<Mission> foundMission = missionRepository.findByUserIdAndMissionStatus(
                user1.getId(),
                MissionStatus.REQUESTED
        );

        // then
        assertThat(foundMission).isPresent();
        assertThat(foundMission.get().getUser().getId()).isEqualTo(user1.getId());
    }

    @DisplayName("사용자 ID와 미션 상태로 조회 시 동일 조건의 미션이 여러 개면 하나를 반환한다")
    @Test
    void findByUserIdAndMissionStatusWithMultipleMatches() {
        // given
        User user = User.createUser("user@example.com");
        userRepository.save(user);

        Location location = Location.createLocation("Location1", "Test Location");
        locationRepository.save(location);

        Mission mission1 = Mission.createMission(user, location);
        Mission mission2 = Mission.createMission(user, location);

        missionRepository.save(mission1);
        missionRepository.save(mission2);
        flushAndClear();

        // when
        Optional<Mission> foundMission = missionRepository.findByUserIdAndMissionStatus(
                user.getId(),
                MissionStatus.REQUESTED
        );

        // then
        assertThat(foundMission).isPresent();
        assertThat(foundMission.get().getMissionStatus()).isEqualTo(MissionStatus.REQUESTED);
        assertThat(foundMission.get().getUser().getId()).isEqualTo(user.getId());
    }

    private void flushAndClear() {
        em.flush();
        em.clear();
    }

}
