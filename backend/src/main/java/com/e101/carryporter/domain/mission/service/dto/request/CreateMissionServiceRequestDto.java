package com.e101.carryporter.domain.mission.service.dto.request;

import lombok.Builder;
import lombok.Getter;

@Getter
public class CreateMissionServiceRequestDto {

    private Long callLocationId;

    @Builder
    private CreateMissionServiceRequestDto(Long callLocationId) {
        this.callLocationId = callLocationId;
    }
}
