package com.e101.carryporter.domain.user.exception;

import com.e101.carryporter.global.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum UserErrorCode implements ErrorCode {

    UNAUTHORIZED("올바르지 않은 비밀번호 입니다", HttpStatus.UNAUTHORIZED),
    USER_NOT_FOUND("해당 사용자를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    DUPLICATED_USER_EMAIL("이미 가입된 사용자 입니다.", HttpStatus.CONFLICT),
    DUPLICATED_ADMIN_NAME("이미 사용중인 관리자 이름입니다.", HttpStatus.CONFLICT);

    private final String message;
    private final HttpStatus httpStatus;

}
