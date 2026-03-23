package com.e101.carryporter.domain.admin.event;

public record AdminLockRequestEvent(
        Long missionId,
        String robotMacAddress
) {
}
