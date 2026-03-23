package com.e101.carryporter.domain.admin.event;

public record AdminUnlockRequestEvent(
        Long missionId,
        String robotMacAddress
) {
}
