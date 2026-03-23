package com.e101.carryporter.domain.auth.controller.dto.request;

import jakarta.validation.constraints.NotNull;

public record VerifyPasswordRequestDto(
        @NotNull(message = "미션 ID는 필수입니다.")
        Long missionId,

        @NotNull(message = "비밀번호는 필수 입력값입니다.")
        Integer password
) {

}
