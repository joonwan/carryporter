package com.e101.carryporter.domain.admin.service;

import com.e101.carryporter.domain.admin.controller.dto.response.LockerResponseDto;
import com.e101.carryporter.domain.locker.entity.Locker;
import com.e101.carryporter.domain.locker.entity.LockerStatus;
import com.e101.carryporter.domain.locker.exception.LockerErrorCode;
import com.e101.carryporter.domain.locker.repository.LockerRepository;
import com.e101.carryporter.domain.mission.entity.Mission;
import com.e101.carryporter.domain.mission.exception.MissionErrorCode;
import com.e101.carryporter.domain.mission.repository.MissionRepository;
import com.e101.carryporter.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminLockerService {

    private final LockerRepository lockerRepository;
    private final MissionRepository missionRepository;

    public List<LockerResponseDto> getAllLockers() {
        return lockerRepository.findAll().stream()
                .map(LockerResponseDto::from)
                .toList();
    }

    public LockerResponseDto getLocker(Long lockerId) {
        return lockerRepository.findById(lockerId)
                .map(LockerResponseDto::from)
                .orElseThrow(() -> new BusinessException(LockerErrorCode.LOCKER_NOT_FOUND));
    }

    @Transactional
    public LockerResponseDto updateLockerStatus(Long lockerId, LockerStatus status) {
        Locker locker = lockerRepository.findById(lockerId)
                .orElseThrow(() -> new BusinessException(LockerErrorCode.LOCKER_NOT_FOUND));
        locker.updateStatus(status);
        return LockerResponseDto.from(locker);
    }

    @Transactional
    public void assignLocker(Long missionId, Long lockerId) {
        Mission mission = missionRepository.findById(missionId)
                .orElseThrow(() -> new BusinessException(MissionErrorCode.MISSION_NOT_FOUND));

        Locker locker = lockerRepository.findById(lockerId)
                .orElseThrow(() -> new BusinessException(LockerErrorCode.LOCKER_NOT_FOUND));

        mission.assignLocker(locker);
        locker.updateStatus(LockerStatus.OCCUPIED);
    }

    @Transactional
    public void changeStatusAll(LockerStatus newStatus) {
        lockerRepository.findAll()
                .forEach(l -> {
                    l.updateStatus(newStatus);
                });
    }
}
