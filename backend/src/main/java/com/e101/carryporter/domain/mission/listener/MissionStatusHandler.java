package com.e101.carryporter.domain.mission.listener;

import com.e101.carryporter.domain.mission.event.MissionFinalizedEvent;
import com.e101.carryporter.domain.mission.event.MissionLockedEvent;
import com.e101.carryporter.domain.mission.event.MissionStoredEvent;
import com.e101.carryporter.domain.mission.event.MissionUnlockedEvent;
import com.e101.carryporter.domain.mission.event.ReturnStartedEvent;
import com.e101.carryporter.domain.mission.service.MissionService;
import com.e101.carryporter.domain.robot.event.RobotArrivalEvent;
import com.e101.carryporter.domain.robot.event.RobotReturnedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class MissionStatusHandler {

    private final MissionService missionService;

    @Async
    @EventListener
    public void handleRobotArrivalEvent(RobotArrivalEvent event) {
        missionService.completeArrival(event.missionId());
    }

    @Async
    @EventListener
    public void handleMissionLockedEvent(MissionLockedEvent event) {
        missionService.completeLock(event.missionId());
    }

    @Async
    @EventListener
    public void handleMissionUnlockedEvent(MissionUnlockedEvent event) {
        missionService.completeUnlock(event.missionId());
    }

    @Async
    @EventListener
    public void handleRobotReturnedEvent(RobotReturnedEvent event) {
        missionService.completeReturn(event.missionId());
    }

    @Async
    @EventListener
    public void handleMissionFinalizedEvent(MissionFinalizedEvent event) {
        missionService.finish(event.missionId(), event.robotId());
    }

    @Async
    @EventListener
    public void handleMissionStoredEvent(MissionStoredEvent event){
        missionService.store(event.missionId(), event.robotId());
    }

    @Async
    @EventListener
    public void handleReturnStartedEvent(ReturnStartedEvent event) {
        missionService.startReturning(event.missionId());
    }


}
