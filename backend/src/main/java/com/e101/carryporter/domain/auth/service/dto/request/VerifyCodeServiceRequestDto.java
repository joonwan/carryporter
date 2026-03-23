package com.e101.carryporter.domain.auth.service.dto.request;

public record VerifyCodeServiceRequestDto(String email, Integer code) {}