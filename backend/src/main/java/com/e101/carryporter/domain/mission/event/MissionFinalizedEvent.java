package com.e101.carryporter.domain.mission.event;

/**
 * 관리자가 최종 점검 완료 후 발행되는 이벤트
 * - 관리자 점검 완료 API 호출 시 발행
 */
public record MissionFinalizedEvent(
        Long missionId,
        Long robotId,
        String message
) {
}
