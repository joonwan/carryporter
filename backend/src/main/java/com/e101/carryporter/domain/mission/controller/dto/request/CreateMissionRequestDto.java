package com.e101.carryporter.domain.mission.controller.dto.request;

import com.e101.carryporter.domain.mission.service.dto.request.CreateMissionServiceRequestDto;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CreateMissionRequestDto {

    @NotNull(message = "호출 위치 아이디 값은 필수 입니다.")
    @Min(value = 0, message = "0 이상의 값만 대입할 수 있다.")
    private Long callLocationId;

    public CreateMissionServiceRequestDto toServiceRequestDto() {
        return CreateMissionServiceRequestDto.builder()
                .callLocationId(callLocationId)
                .build();
    }

}
