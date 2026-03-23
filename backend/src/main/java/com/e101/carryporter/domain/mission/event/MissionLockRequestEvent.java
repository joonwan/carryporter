package com.e101.carryporter.domain.mission.event;

public record MissionLockRequestEvent(
        Long missionId,
        Long userId,
        String robotMacAddress
) {
}
