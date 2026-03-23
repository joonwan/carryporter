package com.e101.carryporter.domain.mission.listener;

import com.e101.carryporter.domain.auth.repository.LoginFailCountRedisRepository;
import com.e101.carryporter.domain.mission.event.MissionAbortedEvent;
import com.e101.carryporter.domain.user.event.UserAuthFailedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class FailureCountHandler {

    private final LoginFailCountRedisRepository redisRepository;
    private final ApplicationEventPublisher eventPublisher;

    private static final int MAX_FAIL_COUNT = 3;

    /**
     * 비밀번호 인증 실패 이벤트 핸들러
     * @Async: 메인 로직(응답)에 지장을 주지 않기 위해 비동기 처리
     * @EventListener: 트랜잭션 롤백 여부와 상관없이 무조건 실행되어야 함
     */
    @Async
    @EventListener
    public void handleUserAuthFailed(UserAuthFailedEvent event) {
        // 1. Redis 카운트 증가 (Repository 내부에서 TTL 설정됨)
        Long currentCount = redisRepository.increment(event.userId());

        // Redis 장애로 0이 반환된 경우 로직을 무시하거나, 로그만 남기고 통과시킴
        if (currentCount == 0) {
            log.warn("[AUTH] Redis 장애로 실패 카운트 증가 실패. userId: {}", event.userId());
            return;
        }

        log.info("[AUTH] 비밀번호 인증 실패! missionId: {}, userId: {}, 누적 실패: {}/{}",
                event.missionId(), event.userId(), currentCount, MAX_FAIL_COUNT);

        // 2. 임계치(3회) 도달 확인
        if (currentCount >= MAX_FAIL_COUNT) {
            handleFailureLimitExceeded(event);
        }
    }

    /**
     * 실패 횟수 초과 시 처리 로직
     */
    private void handleFailureLimitExceeded(UserAuthFailedEvent event) {
        log.error("[AUTH] ⛔️ 인증 실패 횟수 초과! 미션을 중단합니다. missionId: {}", event.missionId());

        // 1. 미션 중단 이벤트 발행
        // -> MqttCommandHandler (로봇 복귀)
        // -> UserSseNotificationHandler (사용자 알림)
        eventPublisher.publishEvent(new MissionAbortedEvent(
                event.missionId(),
                event.userId(),
                event.robotMacAddress(),
                "비밀번호 입력 횟수 초과(3회)"
        ));

        // 2. 카운트 초기화 (미션이 중단되었으므로 카운트 리셋)
        redisRepository.reset(event.userId());
    }
}