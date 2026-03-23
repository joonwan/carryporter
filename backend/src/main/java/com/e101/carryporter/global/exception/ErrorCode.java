package com.e101.carryporter.global.exception;

import org.springframework.http.HttpStatus;

public interface ErrorCode {

    String getMessage();
    HttpStatus getHttpStatus();

}
