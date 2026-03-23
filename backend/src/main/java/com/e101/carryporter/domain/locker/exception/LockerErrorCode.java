package com.e101.carryporter.domain.locker.exception;

import com.e101.carryporter.global.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum LockerErrorCode implements ErrorCode {

    LOCKER_NOT_FOUND("사물함을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    ;

    private final String message;
    private final HttpStatus httpStatus;

}
