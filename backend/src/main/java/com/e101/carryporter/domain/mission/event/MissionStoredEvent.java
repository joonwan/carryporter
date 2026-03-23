package com.e101.carryporter.domain.mission.event;

public record MissionStoredEvent(
        Long missionId,
        Long robotId
) {
}
