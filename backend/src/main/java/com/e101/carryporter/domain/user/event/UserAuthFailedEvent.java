package com.e101.carryporter.domain.user.event;

public record UserAuthFailedEvent(
        Long missionId,
        Long userId, // Redis Key로 쓸 정보
        String robotMacAddress
) {}
