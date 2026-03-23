package com.e101.carryporter.domain.admin.controller.dto.request;

import com.e101.carryporter.domain.robot.service.dto.request.DispatchServiceRequestDto;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class DispatchRequestDto {

    @NotNull(message = "빈값은 허용되지 않습니다.")
    @Min(value = 0, message = "0 이상의 수를 넣을 수 있습니다.")
    private Long robotId;

    @NotNull(message = "빈값은 허용되지 않습니다.")
    @Min(value = 0, message = "0 이상의 수를 넣을 수 있습니다.")
    private Long callLocationId;

    public DispatchServiceRequestDto toServiceRequestDto(Long missionId) {
        return DispatchServiceRequestDto.builder()
                .robotId(robotId)
                .missionId(missionId)
                .callLocationId(callLocationId)
                .build();
    }

}
