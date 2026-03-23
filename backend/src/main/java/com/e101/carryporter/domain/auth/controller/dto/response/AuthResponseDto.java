package com.e101.carryporter.domain.auth.controller.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AuthResponseDto {
    private String status;
    private String message;
    private Integer code;
    private long expiresIn;
}
