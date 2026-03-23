package com.e101.carryporter.domain.locker.service.dto.response;

import java.util.List;

public record UserLockersServiceResponseDto(
        List<UserLockerServiceResponseDto> userLockerServiceResponseDtos
) {
}
