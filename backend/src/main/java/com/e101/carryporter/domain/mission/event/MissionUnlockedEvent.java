package com.e101.carryporter.domain.mission.event;

public record MissionUnlockedEvent(
        Long missionId,
        Long userId,
        String robotMacAddress
) {
}
