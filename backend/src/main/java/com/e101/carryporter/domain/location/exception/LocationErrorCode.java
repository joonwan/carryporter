package com.e101.carryporter.domain.location.exception;

import com.e101.carryporter.global.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum LocationErrorCode implements ErrorCode {

    LOCATION_NOT_FOUND("해당 위치를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),;

    private final String message;
    private final HttpStatus httpStatus;

}
