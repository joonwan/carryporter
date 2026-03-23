package com.e101.carryporter.domain.mission.service;

import com.e101.carryporter.domain.location.entity.Location;
import com.e101.carryporter.domain.location.service.LocationService;
import com.e101.carryporter.domain.locker.entity.Locker;
import com.e101.carryporter.domain.locker.entity.LockerStatus;
import com.e101.carryporter.domain.mission.entity.Mission;
import com.e101.carryporter.domain.mission.entity.MissionStatus;
import com.e101.carryporter.domain.mission.event.MissionCreatedEvent;
import com.e101.carryporter.domain.mission.event.ReturnStartedEvent;
import com.e101.carryporter.domain.mission.exception.MissionErrorCode;
import com.e101.carryporter.domain.mission.repository.MissionRepository;
import com.e101.carryporter.domain.mission.service.dto.request.CreateMissionServiceRequestDto;
import com.e101.carryporter.domain.robot.entity.Robot;
import com.e101.carryporter.domain.robot.entity.RobotStatus;
import com.e101.carryporter.domain.robot.event.RobotAvailabilityChangedEvent;
import com.e101.carryporter.domain.robot.exception.RobotErrorCode;
import com.e101.carryporter.domain.robot.repository.RobotRepository;
import com.e101.carryporter.domain.user.entity.User;
import com.e101.carryporter.domain.user.service.UserService;
import com.e101.carryporter.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class MissionService {

    private final MissionRepository missionRepository;
    private final UserService userService;
    private final LocationService locationService;
    private final RobotRepository robotRepository;
    private final ApplicationEventPublisher eventPublisher;

    public Mission findById(Long missionId) {
        return missionRepository.findById(missionId)
                .orElseThrow(() -> new BusinessException(MissionErrorCode.MISSION_NOT_FOUND));
    }

    @Transactional
    public Long createMission(Long userId, CreateMissionServiceRequestDto request) {

        // 사용자 와 호출 위치 조회
        User user = userService.findById(userId);
        Location location = locationService.findById(request.getCallLocationId());

        // STORING 상태 있으면 조회 없으면 새로 생성
        Mission mission = missionRepository.findByUserIdAndMissionStatus(user.getId(), MissionStatus.STORING)
                .orElseGet(() -> Mission.createMission(user, location));


        mission.updateLocation(location);
        // 새로 생성된 mission 은 save 전까지 id 가 null
        boolean isNew = mission.getId() == null;
        Long createdMissionId = missionRepository.save(mission);

        // 새 미션 생성 완료 이벤트 발행
        eventPublisher.publishEvent(new MissionCreatedEvent(createdMissionId, userId, isNew));

        return createdMissionId;
    }

    @Transactional
    public void assignRobot(Long missionId, Long robotId) {
        Mission mission = missionRepository.findById(missionId)
                .orElseThrow(() -> new BusinessException(MissionErrorCode.MISSION_NOT_FOUND));

        Robot robot = robotRepository.findById(robotId)
                .orElseThrow(() -> new BusinessException(RobotErrorCode.ROBOT_NOT_FOUND));

        // robot 이 idle 상태인지 검증
        validateIdleRobot(robot);

        // 이전 상태 저장
        RobotStatus previousStatus = robot.getRobotStatus();

        mission.assignRobot(robot);

        eventPublisher.publishEvent(new RobotAvailabilityChangedEvent(robot.getId(), robot.getRobotCode(), previousStatus, robot.getRobotStatus()));
    }

    @Transactional
    public void dispatch(Long missionId) {
        Mission mission = missionRepository.findById(missionId)
                .orElseThrow(() -> new BusinessException(MissionErrorCode.MISSION_NOT_FOUND));

        mission.dispatch();
    }

    @Transactional
    public void failMission(Long missionId) {
        log.debug("미션 실패!! mission id = {}", missionId);
        Mission mission = missionRepository.findById(missionId)
                .orElseThrow(() -> new BusinessException(MissionErrorCode.MISSION_NOT_FOUND));
        mission.failed();
    }

    @Transactional
    public void completeArrival(Long missionId) {
        log.debug("로봇 목적지에 도착!! mission id = {}", missionId);
        Mission mission = missionRepository.findById(missionId)
                .orElseThrow(() -> new BusinessException(MissionErrorCode.MISSION_NOT_FOUND));

        mission.arrive();
    }

    @Transactional
    public void completeReturn(Long missionId) {
        log.debug("복귀 완료 mission id = {}", missionId);
        Mission mission = missionRepository.findById(missionId)
                .orElseThrow(() -> new BusinessException(MissionErrorCode.MISSION_NOT_FOUND));

        mission.returned();
    }

    @Transactional
    public void completeLock(Long missionId) {
        log.debug("잠금 완료 mission id = {}", missionId);
        Mission mission = missionRepository.findById(missionId)
                .orElseThrow(() -> new BusinessException(MissionErrorCode.MISSION_NOT_FOUND));

        mission.lock();
    }

    @Transactional
    public void completeUnlock(Long missionId) {
        log.debug("잠금 해제 완료 mission id = {}", missionId);
        Mission mission = missionRepository.findById(missionId)
                .orElseThrow(() -> new BusinessException(MissionErrorCode.MISSION_NOT_FOUND));

        mission.unlock();
    }

    @Transactional
    public void store(Long missionId, Long robotId) {
        log.debug("미션 보관 missionId = {}, robotId = {}", missionId, robotId);
        Mission mission = missionRepository.findById(missionId)
                .orElseThrow(() -> new BusinessException(MissionErrorCode.MISSION_NOT_FOUND));
        mission.store();

        Robot robot = robotRepository.findById(robotId)
                .orElseThrow(() -> new BusinessException(RobotErrorCode.ROBOT_NOT_FOUND));

        RobotStatus previousStatus = robot.getRobotStatus();
        robot.changeStatus(RobotStatus.IDLE);

        eventPublisher.publishEvent(new RobotAvailabilityChangedEvent(robot.getId(), robot.getRobotCode(), previousStatus, robot.getRobotStatus()));


    }

    // 끝나야 로봇도 대기큐로 이동
    @Transactional
    public void finish(Long missionId, Long robotId) {
        log.debug("미션 종료 missionId = {}, robotId = {}", missionId, robotId);
        Mission mission = missionRepository.findById(missionId)
                .orElseThrow(() -> new BusinessException(MissionErrorCode.MISSION_NOT_FOUND));

        Robot robot = robotRepository.findById(robotId)
                .orElseThrow(() -> new BusinessException(RobotErrorCode.ROBOT_NOT_FOUND));

        mission.finish();

        // locker 할당 해제
        Locker locker = mission.getLocker();
        if (locker != null) {
            locker.updateStatus(LockerStatus.AVAILABLE);
        }

        RobotStatus previousStatus = robot.getRobotStatus();
        robot.changeStatus(RobotStatus.IDLE);

        eventPublisher.publishEvent(new RobotAvailabilityChangedEvent(robot.getId(), robot.getRobotCode(), previousStatus, robot.getRobotStatus()));
    }

    public void returnToMainStation(Long missionId, Long userId) {
        Mission mission = missionRepository.findById(missionId)
                .orElseThrow(() -> new BusinessException(MissionErrorCode.MISSION_NOT_FOUND));

        // 사용자의 미션인지 검증
        validateAuthorization(mission, userId);

        Robot robot = mission.getRobot();
        String macAddress = robot.getMacAddress();

        eventPublisher.publishEvent(new ReturnStartedEvent(
                missionId,
                macAddress,
                0.0,
                0.0,
                mission.getLocker().getLockerCode(),
                LocalDateTime.now()
        ));
    }

    @Transactional
    public void startReturning(Long missionId) {
        Mission mission = missionRepository.findById(missionId)
                .orElseThrow(() -> new BusinessException(MissionErrorCode.MISSION_NOT_FOUND));

        mission.returning();
    }

    private void validateIdleRobot(Robot robot) {
        if (!robot.getRobotStatus().equals(RobotStatus.IDLE)) {
            throw new BusinessException(RobotErrorCode.INVALID_STATUS_CHANGE);
        }
    }

    private void validateAuthorization(Mission mission, Long userId) {
        log.debug("mission's userId = {}, request userId = {}", mission.getUser().getId(), userId);

        if (!mission.getUser().getId().equals(userId)) {
            throw new BusinessException(MissionErrorCode.FORBIDDEN);
        }
    }

    // warning: FINISHED 제외한 모든 진행중인 미션을 FAILED로 변경
    @Transactional
    public void failAllExceptFinished() {
        missionRepository.failAllExceptFinished();
    }
}
