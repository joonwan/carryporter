package com.e101.carryporter.domain.auth.service.dto.request;

public record VerifyPasswordServiceRequestDto(
        Long userId,
        Long missionId,
        Integer password
) {
}
