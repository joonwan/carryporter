package com.e101.carryporter.domain.locker.service;

import com.e101.carryporter.domain.location.entity.Location;
import com.e101.carryporter.domain.locker.entity.Locker;
import com.e101.carryporter.domain.locker.entity.LockerStatus;
import com.e101.carryporter.domain.locker.entity.UserLockerStatus;
import com.e101.carryporter.domain.locker.service.dto.response.UserLockerServiceResponseDto;
import com.e101.carryporter.domain.locker.service.dto.response.UserLockersServiceResponseDto;
import com.e101.carryporter.domain.mission.entity.Mission;
import com.e101.carryporter.domain.mission.repository.MissionRepository;
import com.e101.carryporter.domain.user.entity.User;
import com.e101.carryporter.support.IntegrationTestSupport;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LockerServiceTest extends IntegrationTestSupport {

    @Autowired
    private LockerService lockerService;

    @Autowired
    private MissionRepository missionRepository;

    @Autowired
    private EntityManager em;

    @DisplayName("사용자의 사물함 이용 내역을 조회한다")
    @Test
    void getUserLockers() {
        // given
        User user = User.createUser("user@example.com");
        em.persist(user);

        Location location = Location.createLocation("Location1", "Test Location");
        em.persist(location);

        Locker locker1 = Locker.createLocker("L001");
        Locker locker2 = Locker.createLocker("L002");
        em.persist(locker1);
        em.persist(locker2);

        Mission mission1 = Mission.createMission(user, location);
        mission1.assignLocker(locker1);
        missionRepository.save(mission1);

        Mission mission2 = Mission.createMission(user, location);
        mission2.assignLocker(locker2);
        missionRepository.save(mission2);

        em.flush();
        em.clear();

        // when
        UserLockersServiceResponseDto result = lockerService.getUserLockers(user.getId());

        // then
        List<UserLockerServiceResponseDto> lockers = result.userLockerServiceResponseDtos();
        assertThat(lockers).hasSize(2);
        assertThat(lockers).extracting("lockerId")
                .containsExactlyInAnyOrder(locker1.getId(), locker2.getId());
        assertThat(lockers).extracting("userLockerStatus")
                .containsOnly(UserLockerStatus.OCCUPIED);
        assertThat(lockers).allMatch(dto -> dto.getUpdatedAt() != null);
    }

    @DisplayName("사용자의 사물함 이용 내역이 없으면 빈 리스트를 반환한다")
    @Test
    void getUserLockersWithNoHistory() {
        // given
        User user = User.createUser("user@example.com");
        em.persist(user);
        em.flush();
        em.clear();

        // when
        UserLockersServiceResponseDto result = lockerService.getUserLockers(user.getId());

        // then
        assertThat(result.userLockerServiceResponseDtos()).isEmpty();
    }

    @DisplayName("사용자의 OCCUPIED와 COMPLETED 상태의 사물함 이용 내역을 조회한다")
    @Test
    void getUserLockersWithDifferentStatuses() {
        // given
        User user = User.createUser("user@example.com");
        em.persist(user);

        Location location = Location.createLocation("Location1", "Test Location");
        em.persist(location);

        Locker locker1 = Locker.createLocker("L001");
        Locker locker2 = Locker.createLocker("L002");
        em.persist(locker1);
        em.persist(locker2);

        // OCCUPIED 상태
        Mission mission1 = Mission.createMission(user, location);
        mission1.assignLocker(locker1);
        missionRepository.save(mission1);

        // COMPLETED 상태
        Mission mission2 = Mission.createMission(user, location);
        mission2.assignLocker(locker2);
        mission2.finish();
        missionRepository.save(mission2);

        em.flush();
        em.clear();

        // when
        UserLockersServiceResponseDto result = lockerService.getUserLockers(user.getId());

        // then
        List<UserLockerServiceResponseDto> lockers = result.userLockerServiceResponseDtos();
        assertThat(lockers).hasSize(2);
        assertThat(lockers).extracting("userLockerStatus")
                .containsExactlyInAnyOrder(
                        UserLockerStatus.OCCUPIED,
                        UserLockerStatus.COMPLETED
                );
    }

    @DisplayName("여러 사용자의 사물함 내역 중 특정 사용자의 내역만 조회한다")
    @Test
    void getUserLockersOnlyForSpecificUser() {
        // given
        User user1 = User.createUser("user1@example.com");
        User user2 = User.createUser("user2@example.com");
        em.persist(user1);
        em.persist(user2);

        Location location = Location.createLocation("Location1", "Test Location");
        em.persist(location);

        Locker locker1 = Locker.createLocker("L001");
        Locker locker2 = Locker.createLocker("L002");
        Locker locker3 = Locker.createLocker("L003");
        em.persist(locker1);
        em.persist(locker2);
        em.persist(locker3);

        // user1의 미션
        Mission mission1 = Mission.createMission(user1, location);
        mission1.assignLocker(locker1);
        missionRepository.save(mission1);

        Mission mission2 = Mission.createMission(user1, location);
        mission2.assignLocker(locker2);
        missionRepository.save(mission2);

        // user2의 미션
        Mission mission3 = Mission.createMission(user2, location);
        mission3.assignLocker(locker3);
        missionRepository.save(mission3);

        em.flush();
        em.clear();

        // when
        UserLockersServiceResponseDto result = lockerService.getUserLockers(user1.getId());

        // then
        List<UserLockerServiceResponseDto> lockers = result.userLockerServiceResponseDtos();
        assertThat(lockers).hasSize(2);
        assertThat(lockers).extracting("lockerId")
                .containsExactlyInAnyOrder(locker1.getId(), locker2.getId())
                .doesNotContain(locker3.getId());
    }

    @DisplayName("사물함 배정 시간이 기록된 사물함 내역을 조회한다")
    @Test
    void getUserLockersWithAssignedTime() {
        // given
        User user = User.createUser("user@example.com");
        em.persist(user);

        Location location = Location.createLocation("Location1", "Test Location");
        em.persist(location);

        Locker locker = Locker.createLocker("L001");
        em.persist(locker);

        Mission mission = Mission.createMission(user, location);
        mission.assignLocker(locker);
        missionRepository.save(mission);

        em.flush();
        em.clear();

        // when
        UserLockersServiceResponseDto result = lockerService.getUserLockers(user.getId());

        // then
        List<UserLockerServiceResponseDto> lockers = result.userLockerServiceResponseDtos();
        assertThat(lockers).hasSize(1);
        UserLockerServiceResponseDto dto = lockers.get(0);
        assertThat(dto.getLockerId()).isEqualTo(locker.getId());
        assertThat(dto.getUserLockerStatus()).isEqualTo(UserLockerStatus.OCCUPIED);
        assertThat(dto.getUpdatedAt()).isNotNull();
    }
}
