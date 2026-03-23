package com.e101.carryporter.domain.robot.listener;

import com.e101.carryporter.domain.robot.event.RobotAvailabilityChangedEvent;
import com.e101.carryporter.domain.robot.repository.RobotRealTimeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@Slf4j
@RequiredArgsConstructor
public class RobotRedisSyncHandler {

    private final RobotRealTimeRepository robotRealTimeRepository;

    @Async
    @Retryable(
            retryFor = {Exception.class},       // 모든 예외에 대해
            maxAttempts = 3,                    // 최대 재시도 횟수는 3번 (default)
            backoff = @Backoff(delay = 1000)    // 1초 간격으로
    )
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleRobotAvailabilityChangedEvent(RobotAvailabilityChangedEvent event) {
        log.debug("{} 로봇 상태 변경 {} -> {}", event.robotCode(), event.previousStatus(), event.newStatus());
        robotRealTimeRepository.updateStatusOnly(event.robotId(),event.newStatus());
    }

    // 3번다 실패했을 경우
    @Recover
    public void recover(Exception e, RobotAvailabilityChangedEvent event) {
        log.error("Redis 와 DB 로봇 상태 동기화 실패 (3회 시도 초과) robot id : {}, status : {} -> {} ", event.robotId(), event.previousStatus(), event.newStatus(), e);
        // todo 알림 로직 또는 failed_event table 생성 후 후속조치 필요
    }
}
