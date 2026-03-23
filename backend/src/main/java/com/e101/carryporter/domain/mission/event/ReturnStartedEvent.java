package com.e101.carryporter.domain.mission.event;

import java.time.LocalDateTime;

public record ReturnStartedEvent(
        Long missionId,
        String robotMacAddress,
        Double homeX,
        Double homeY,
        String lockerCode,
        LocalDateTime localDateTime
) {
}
