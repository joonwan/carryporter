package com.e101.carryporter.domain.locker.service.dto.response;

import com.e101.carryporter.domain.locker.entity.UserLockerStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class UserLockerServiceResponseDto {
    private Long lockerId;

    private UserLockerStatus userLockerStatus;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Seoul")
    private LocalDateTime updatedAt;
}
