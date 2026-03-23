package com.e101.carryporter.global.listener;

import com.e101.carryporter.domain.admin.event.AdminLockRequestEvent;
import com.e101.carryporter.domain.admin.event.AdminUnlockRequestEvent;
import com.e101.carryporter.domain.mission.event.*;
import com.e101.carryporter.domain.user.event.UserAuthSuccessEvent;
import com.e101.carryporter.global.service.mqtt.MqttPublisherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@Slf4j
@RequiredArgsConstructor
public class MqttCommandHandler {

    private final MqttPublisherService mqttPublisherService;

    /**
     * 공통 사항
     * TransactionPhase.AFTER_COMMIT : 트랜잭션이 내에서 발생한 경우 트랜잭션 COMMIT이 된 경우에만 동작
     * fallbackExecution = true : 트랜잭션이 없는 환경에서 테스트 위함
     */



    /**
     * 관리자 잠금 요청 → 로봇에게 LOCK 명령 전송
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handleAdminLockRequest(AdminLockRequestEvent event) {
        log.info("[MQTT] 관리자 잠금 요청 - missionId: {}, robotMacAddress: {}",
                event.missionId(), event.robotMacAddress());
        mqttPublisherService.sendCommand(event.robotMacAddress(), "lock", "{}");
    }

    /**
     * 미션 시작 → 로봇에게 주행 좌표(START_NAV) 전송
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handleMissionStarted(MissionStartedEvent event) {
        log.info("[MQTT] 미션 시작 - missionId: {}, robotMacAddress: {}, destination: {})",
                event.missionId(), event.robotMacAddress(), event.destination());
        mqttPublisherService.sendDispatchCommand(event.robotMacAddress(), event.destination());
    }

    /**
     * 사용자 인증 성공 → 로봇에게 UNLOCK 명령 전송
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handleUserAuthSuccess(UserAuthSuccessEvent event) {
        log.info("[MQTT] 사용자 인증 성공 - missionId: {}, userId: {}, robotMacAddress: {}",
                event.missionId(), event.userId(), event.robotMacAddress());
        mqttPublisherService.sendCommand(event.robotMacAddress(), "unlock", "{}");
    }

    /**
     * 관리자 잠금해제 요청 → 로봇에게 UNLOCK 명령 전송
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handleAdminUnlockRequest(AdminUnlockRequestEvent event) {
        log.info("[MQTT] 관리자 잠금해제 요청 - missionId: {}, robotMacAddress: {}",
                event.missionId(), event.robotMacAddress());
        mqttPublisherService.sendCommand(event.robotMacAddress(), "unlock", "{}");
    }

    /**
     * 미션 중단 (인증 3회 실패 등) → 로봇 즉시 복귀 명령
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handleMissionAborted(MissionAbortedEvent event) {
        log.info("[MQTT] 미션 중단 - missionId: {}, robotMacAddress: {}, reason: {}",
                event.missionId(), event.robotMacAddress(), event.reason());
        mqttPublisherService.sendReturnCommand(event.robotMacAddress());
    }

    /**
     * 사용자 최종 잠금 → 로봇 잠금 명령
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handleMissionLocked(MissionLockRequestEvent event) {
        log.info("[MQTT] 미션 잠금 요청 완료 - missionId: {}, userId: {}, robotMacAddress: {}",
                event.missionId(), event.userId(), event.robotMacAddress());
        mqttPublisherService.sendCommand(event.robotMacAddress(), "lock", "{}");
    }

    /**
     * 복귀 시작 → 관리소 좌표로 주행 명령
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handleReturnStarted(ReturnStartedEvent event) {
        log.info("[MQTT] 복귀 시작 - missionId: {}, robotMacAddress: {}, home: ({}, {})",
                event.missionId(), event.robotMacAddress(), event.homeX(), event.homeY());
        mqttPublisherService.sendReturnCommand(event.robotMacAddress());
    }

}
