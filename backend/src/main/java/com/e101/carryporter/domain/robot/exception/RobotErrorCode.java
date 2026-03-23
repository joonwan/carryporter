package com.e101.carryporter.domain.robot.exception;

import com.e101.carryporter.global.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum RobotErrorCode implements ErrorCode {

    ROBOT_NOT_FOUND("해당 로봇을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    ROBOT_NOT_AVAILABLE("가용 가능한 로봇이 없습니다.", HttpStatus.CONFLICT),
    INVALID_STATUS_CHANGE("가용가능한 로봇만 운행 상태로 변경할 수 있습니다", HttpStatus.BAD_REQUEST)
    ;

    private final String message;
    private final HttpStatus httpStatus;

}
