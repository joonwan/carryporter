package com.e101.carryporter.domain.mission.controller.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CreateMissionResponseDto {
    private Long missionId;

    public static CreateMissionResponseDto of(Long missionId) {
        return new CreateMissionResponseDto(missionId);
    }
}
