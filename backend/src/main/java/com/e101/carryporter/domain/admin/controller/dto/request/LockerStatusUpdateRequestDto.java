package com.e101.carryporter.domain.admin.controller.dto.request;

import com.e101.carryporter.domain.locker.entity.LockerStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class LockerStatusUpdateRequestDto {

    @NotNull(message = "상태는 필수입니다")
    private LockerStatus status;
}
