package com.e101.carryporter.domain.admin.controller.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class LoginRequestDto {

    @NotBlank(message = "matter most email 은 필수 입니다.")
    private String mmEmail;

    @NotBlank(message = "password 는 필수 입니다.")
    private String password;

}
