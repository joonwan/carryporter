package com.e101.carryporter.domain.mission.event;

public record MissionAbortedEvent(
        Long missionId,
        Long userId,
        String robotMacAddress,
        String reason   //선택사항) 왜 중단됐는지 ex) 비밀번호 3회 오류
) {
}
