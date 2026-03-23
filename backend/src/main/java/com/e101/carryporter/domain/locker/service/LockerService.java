package com.e101.carryporter.domain.locker.service;

import com.e101.carryporter.domain.locker.entity.Locker;
import com.e101.carryporter.domain.locker.service.dto.response.UserLockerServiceResponseDto;
import com.e101.carryporter.domain.locker.service.dto.response.UserLockersServiceResponseDto;
import com.e101.carryporter.domain.locker.service.dto.response.UserStoringLockerServiceResponseDto;
import com.e101.carryporter.domain.mission.entity.Mission;
import com.e101.carryporter.domain.mission.entity.MissionStatus;
import com.e101.carryporter.domain.mission.repository.MissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class LockerService {

    private final MissionRepository missionRepository;

    public UserLockersServiceResponseDto getUserLockers(Long userId) {
        List<UserLockerServiceResponseDto> userLockers = missionRepository.findByUserId(userId)
                .stream()
                .map(m -> new UserLockerServiceResponseDto(
                        m.getLocker().getId(),
                        m.getUserLockerStatus(),
                        m.getLockerAssignedAt()))
                .toList();

        return new UserLockersServiceResponseDto(userLockers);
    }

    public UserStoringLockerServiceResponseDto getStoringLocker(Long userId) {
        Optional<Mission> missionsOpt = missionRepository.findByUserIdAndMissionStatus(userId, MissionStatus.STORING);

        if (missionsOpt.isEmpty()) {
            return new UserStoringLockerServiceResponseDto();
        }
        Mission mission = missionsOpt.get();
        Locker occupieeLocker = mission.getLocker();
        return new UserStoringLockerServiceResponseDto(occupieeLocker.getLockerCode(), mission.getLockerAssignedAt());

    }
}
