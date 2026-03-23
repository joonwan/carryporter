package com.e101.carryporter.domain.robot.service.dto.request;

import lombok.Builder;
import lombok.Getter;

@Getter
public class DispatchServiceRequestDto {

    private Long missionId;
    private Long robotId;
    private Long callLocationId;

    @Builder
    private DispatchServiceRequestDto(Long missionId, Long robotId, Long callLocationId) {
        this.missionId = missionId;
        this.robotId = robotId;
        this.callLocationId = callLocationId;
    }
}
