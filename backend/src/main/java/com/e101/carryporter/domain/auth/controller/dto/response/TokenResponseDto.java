package com.e101.carryporter.domain.auth.controller.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TokenResponseDto {
    private String accessToken;
    private String refreshToken;
    private String grantType;
    private long expiresIn;
}
