package com.e101.carryporter.domain.mission.event;

public record MissionFailedEvent(
        Long missionId,
        Long userId,
        String message
) {
    public static MissionFailedEvent bizError(Long missionId, Long userId, String message) {
        return new MissionFailedEvent(missionId, userId, message);
    }

    public static MissionFailedEvent systemError(Long missionId, Long userId, String message) {
        return new MissionFailedEvent(missionId, userId, message);
    }
}
