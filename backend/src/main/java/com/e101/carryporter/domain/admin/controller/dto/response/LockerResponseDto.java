package com.e101.carryporter.domain.admin.controller.dto.response;

import com.e101.carryporter.domain.locker.entity.Locker;
import com.e101.carryporter.domain.locker.entity.LockerStatus;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LockerResponseDto {

    private Long lockerId;
    private String lockerCode;
    private LockerStatus status;

    public static LockerResponseDto from(Locker locker) {
        return LockerResponseDto.builder()
                .lockerId(locker.getId())
                .lockerCode(locker.getLockerCode())
                .status(locker.getLockerStatus())
                .build();
    }
}
