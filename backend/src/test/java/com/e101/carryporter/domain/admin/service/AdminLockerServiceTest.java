package com.e101.carryporter.domain.admin.service;

import com.e101.carryporter.domain.location.entity.Location;
import com.e101.carryporter.domain.location.repository.LocationRepository;
import com.e101.carryporter.domain.locker.entity.Locker;
import com.e101.carryporter.domain.locker.entity.LockerStatus;
import com.e101.carryporter.domain.locker.entity.UserLockerStatus;
import com.e101.carryporter.domain.locker.repository.LockerRepository;
import com.e101.carryporter.domain.mission.entity.Mission;
import com.e101.carryporter.domain.mission.repository.MissionRepository;
import com.e101.carryporter.domain.mission.service.MissionService;
import com.e101.carryporter.domain.mission.service.dto.request.CreateMissionServiceRequestDto;
import com.e101.carryporter.domain.user.entity.User;
import com.e101.carryporter.domain.user.repository.UserRepository;
import com.e101.carryporter.global.exception.BusinessException;
import com.e101.carryporter.support.IntegrationTestSupport;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AdminLockerServiceTest extends IntegrationTestSupport {

    @Autowired
    AdminLockerService adminLockerService;

    @Autowired
    MissionService missionService;

    @Autowired
    UserRepository userRepository;

    @Autowired
    LocationRepository locationRepository;

    @Autowired
    LockerRepository lockerRepository;

    @Autowired
    MissionRepository missionRepository;

    @Autowired
    EntityManager em;

    @DisplayName("미션에 사물함을 배정하면 사물함이 정상적으로 할당된다.")
    @Test
    void assignLocker() {
        // given
        User user = User.createUser("test@mm.com");
        Location location = Location.createLocation("Gate A12", "탑승구 A12");
        Locker locker = Locker.createLocker("LOCKER-001");

        Long userId = userRepository.save(user);
        Long locationId = locationRepository.save(location);
        Long lockerId = lockerRepository.save(locker);

        CreateMissionServiceRequestDto request = CreateMissionServiceRequestDto.builder()
                .callLocationId(locationId)
                .build();

        Long missionId = missionService.createMission(userId, request);
        flushAndClear();

        // when
        adminLockerService.assignLocker(missionId, lockerId);
        flushAndClear();

        // then
        Mission mission = missionRepository.findById(missionId).orElseThrow();
        assertThat(mission.getLocker()).isNotNull();
        assertThat(mission.getLocker().getId()).isEqualTo(lockerId);
        assertThat(mission.getLocker().getLockerCode()).isEqualTo("LOCKER-001");
        assertThat(mission.getLockerAssignedAt()).isNotNull();
        assertThat(mission.getUserLockerStatus()).isEqualTo(UserLockerStatus.OCCUPIED);

        // locker 상태가 OCCUPIED로 변경되었는지 확인
        assertThat(mission.getLocker().getLockerStatus()).isEqualTo(LockerStatus.OCCUPIED);
    }

    @DisplayName("존재하지 않는 미션에 사물함을 배정하면 예외가 발생한다.")
    @Test
    void assignLockerToNonExistentMission() {
        // given
        Locker locker = Locker.createLocker("LOCKER-001");
        Long lockerId = lockerRepository.save(locker);
        flushAndClear();

        Long nonExistentMissionId = 999L;

        // when & then
        assertThatThrownBy(() -> adminLockerService.assignLocker(nonExistentMissionId, lockerId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("미션을 찾을 수 없습니다");
    }

    @DisplayName("미션에 존재하지 않는 사물함을 배정하면 예외가 발생한다.")
    @Test
    void assignNonExistentLockerToMission() {
        // given
        User user = User.createUser("test@mm.com");
        Location location = Location.createLocation("Gate A12", "탑승구 A12");

        Long userId = userRepository.save(user);
        Long locationId = locationRepository.save(location);

        CreateMissionServiceRequestDto request = CreateMissionServiceRequestDto.builder()
                .callLocationId(locationId)
                .build();

        Long missionId = missionService.createMission(userId, request);
        flushAndClear();

        Long nonExistentLockerId = 999L;

        // when & then
        assertThatThrownBy(() -> adminLockerService.assignLocker(missionId, nonExistentLockerId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("사물함을 찾을 수 없습니다");
    }

    @DisplayName("changeStatusAll 호출 시 모든 locker가 지정된 상태로 변경된다.")
    @Test
    void changeStatusAll() {
        // given - 다양한 상태의 locker 생성
        Locker locker1 = Locker.createLocker("LOCKER-001");
        locker1.updateStatus(LockerStatus.OCCUPIED);
        lockerRepository.save(locker1);

        Locker locker2 = Locker.createLocker("LOCKER-002");
        locker2.updateStatus(LockerStatus.OCCUPIED);
        lockerRepository.save(locker2);

        Locker locker3 = Locker.createLocker("LOCKER-003");
        locker3.updateStatus(LockerStatus.OCCUPIED);
        lockerRepository.save(locker3);

        Locker locker4 = Locker.createLocker("LOCKER-004");
        // AVAILABLE 상태 (기본값)
        lockerRepository.save(locker4);

        flushAndClear();

        // when - 모든 locker를 AVAILABLE로 변경
        adminLockerService.changeStatusAll(LockerStatus.AVAILABLE);
        flushAndClear();

        // then - 모든 locker가 AVAILABLE 상태로 변경
        Locker updatedLocker1 = lockerRepository.findById(locker1.getId()).orElseThrow();
        Locker updatedLocker2 = lockerRepository.findById(locker2.getId()).orElseThrow();
        Locker updatedLocker3 = lockerRepository.findById(locker3.getId()).orElseThrow();
        Locker updatedLocker4 = lockerRepository.findById(locker4.getId()).orElseThrow();

        assertThat(updatedLocker1.getLockerStatus()).isEqualTo(LockerStatus.AVAILABLE);
        assertThat(updatedLocker2.getLockerStatus()).isEqualTo(LockerStatus.AVAILABLE);
        assertThat(updatedLocker3.getLockerStatus()).isEqualTo(LockerStatus.AVAILABLE);
        assertThat(updatedLocker4.getLockerStatus()).isEqualTo(LockerStatus.AVAILABLE);
    }

    @DisplayName("changeStatusAll 호출 시 locker가 없어도 정상 동작한다.")
    @Test
    void changeStatusAll_withNoLockers() {
        // given - locker 없음

        // when & then - 예외 없이 정상 실행
        adminLockerService.changeStatusAll(LockerStatus.AVAILABLE);
    }

    @DisplayName("changeStatusAll로 AVAILABLE에서 OCCUPIED로 변경할 수 있다.")
    @Test
    void changeStatusAll_toOccupied() {
        // given - AVAILABLE 상태의 locker들
        Locker locker1 = Locker.createLocker("LOCKER-001");
        lockerRepository.save(locker1);

        Locker locker2 = Locker.createLocker("LOCKER-002");
        lockerRepository.save(locker2);

        flushAndClear();

        // when - OCCUPIED 상태로 변경
        adminLockerService.changeStatusAll(LockerStatus.OCCUPIED);
        flushAndClear();

        // then
        Locker updatedLocker1 = lockerRepository.findById(locker1.getId()).orElseThrow();
        Locker updatedLocker2 = lockerRepository.findById(locker2.getId()).orElseThrow();

        assertThat(updatedLocker1.getLockerStatus()).isEqualTo(LockerStatus.OCCUPIED);
        assertThat(updatedLocker2.getLockerStatus()).isEqualTo(LockerStatus.OCCUPIED);
    }

    private void flushAndClear() {
        em.flush();
        em.clear();
    }
}
