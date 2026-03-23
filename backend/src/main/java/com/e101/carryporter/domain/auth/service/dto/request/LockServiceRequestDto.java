package com.e101.carryporter.domain.auth.service.dto.request;

public record LockServiceRequestDto(
        Long userId,
        Long missionId
) {
}