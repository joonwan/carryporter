package com.e101.carryporter.domain.auth.controller.dto.request;

import jakarta.validation.constraints.NotNull;

public record LockRequestDto(
        @NotNull(message = "미션 ID는 필수입니다.")
        Long missionId
) {
}
