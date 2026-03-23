package com.e101.carryporter.domain.sse.listener;

import com.e101.carryporter.domain.mission.event.*;
import com.e101.carryporter.domain.robot.event.*;
import com.e101.carryporter.domain.sse.service.SseService;
import com.e101.carryporter.domain.user.event.UserAuthSuccessEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class AdminSseNotificationHandler {
    private final SseService sseService;

    /**
     * 1. 사용자 호출 후 로봇 배정 완료 알림
     */
    @EventListener
    public void handleRobotAssignedEvent(RobotAssignedEvent event) {
        sseService.broadcastToAdmins("RobotAssignedEvent", event);
    }

    /**
     * 2. 로봇 관리자가 출발누를 때 알림 (dispatch)
     */
    @Async
    @TransactionalEventListener
    public void handleMissionStartedEvent(MissionStartedEvent event) {
        log.debug("[AdminSseNotificationHandler] 사용자에게 출발!! mission id = {}", event.missionId());
        sseService.broadcastToAdmins("MissionStartedEvent", event);
    }

    /**
     * 3. 로봇 도착 알림
     */
    @Async
    @TransactionalEventListener
    public void handleRobotArrivalEvent(RobotArrivalEvent event) {
        log.debug("[AdminSseNotificationHandler] 사용자에게 도착!! mission id = {}", event.missionId());
        sseService.broadcastToAdmins("RobotArrivalEvent", event);
    }

    /**
     * 4. 사용자 로봇 잠금 해제 알림 (관리자에게)
     */
    @Async
    @TransactionalEventListener
    public void handleUserAuthSuccessEvent(UserAuthSuccessEvent event) {
        log.debug("[AdminSseNotificationHandler] 사용자 잠금해제!! mission id = {}", event.missionId());
        sseService.broadcastToAdmins("UserAuthSuccessEvent", event);
    }

    /**
     * 5. 사용자 잠금 알림 (관리자에게)
     */

    @Async
    @TransactionalEventListener
    public void handleMissionLockRequestEvent(MissionLockRequestEvent event) {
        log.debug("[AdminSseNotificationHandler] 사용자 잠금!! mission id = {}", event.missionId());
        sseService.broadcastToAdmins("MissionLockRequestEvent", event);
    }

    /**
     * 6. 로복 복귀 시작 알림
     */

    @Async
    @TransactionalEventListener
    public void handleReturnStartedEvent(ReturnStartedEvent event) {
        log.debug("[AdminSseNotificationHandler] 로봇 복귀 시작!! mission id = {}", event.missionId());
        sseService.broadcastToAdmins("ReturnStartedEvent", event);
    }

    /**
     * 7. 로봇 복귀 알림 -> 관리자 판단 후에 최종적으로 반납 or 보관 선택
     */

    @Async
    @TransactionalEventListener
    public void handleRobotReturnedAdminEvent(RobotReturnedAdminEvent event) {
        log.debug("[AdminSseNotificationHandler] 로봇 복귀 완료!! mission id = {}", event.missionId());
        sseService.broadcastToAdmins("RobotReturnedAdminEvent", event);
    }

    /**
     * 8. 미션 종료 알림
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleMissionFinalizedEvent(MissionFinalizedEvent event) {
        log.debug("[AdminSseNotificationHandler] mission 종료: missionId = {}, message = {}", event.missionId(), event.message());
        sseService.broadcastToAdmins("MissionFinalizedEvent", event);
    }

    /**
     * 9. 사물함 보관 알림
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleMissionStoredEvent(MissionStoredEvent event) {
        log.debug("[AdminSseNotificationHandler] 물건을 사물함에 보관 완료!: missionId = {}, message = {}", event.missionId(), "물건을 사물함에 보관 완료!");
        sseService.broadcastToAdmins("MissionStoredEvent", event);
    }

    /**
     * 10. 로봇 배정 실패 알림
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleMissionFailedEvent(MissionFailedEvent event) {
        log.debug("[AdminSseNotificationHandler] mission 생성 실패: missionId = {}, userId = {}, message = {}", event.missionId(), event.userId(), event.message());
        sseService.broadcastToAdmins("MissionFailedEvent", event);
    }

    /**
     * 11. 로봇 긴급 멈춤 알림
     */
    public void handleRobotEmergencyEvent(RobotEmergencyEvent event){
        log.debug("[AdminSseEmergencyEventHandler] mission 생성 실패:");
        sseService.broadcastToAdmins("RobotEmergencyEvent", event.msg());
    }

}



