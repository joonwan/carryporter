package com.e101.carryporter.domain.mission.event;

public record MissionCreatedEvent(Long missionId, Long userId, boolean isNew) {}