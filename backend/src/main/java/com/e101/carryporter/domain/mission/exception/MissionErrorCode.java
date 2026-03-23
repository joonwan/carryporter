package com.e101.carryporter.domain.mission.exception;

import com.e101.carryporter.global.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum MissionErrorCode implements ErrorCode {

    MISSION_NOT_FOUND("해당 미션을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    FORBIDDEN("해당 미션에 접근할 권한이 없습니다", HttpStatus.FORBIDDEN);

    private final String message;
    private final HttpStatus httpStatus;

}
