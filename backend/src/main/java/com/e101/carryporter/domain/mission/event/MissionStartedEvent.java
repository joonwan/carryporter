package com.e101.carryporter.domain.mission.event;

public record MissionStartedEvent(
        Long userId,
        Long missionId,
        String robotCode,
        String robotMacAddress,
        String destination
) {
}
