package com.e101.carryporter.domain.auth.controller.dto.request;

import com.e101.carryporter.domain.auth.service.dto.request.VerifyCodeServiceRequestDto;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class VerifyCodeRequestDto {

    @NotBlank(message = "이메일은 필수 입력값입니다.")
    @Email(message = "올바른 이메일 형식이 아닙니다.")
    private String email;

    @NotNull(message = "인증번호는 필수 입력값입니다.")
    private Integer code;

    // Service 계층으로 넘기기 위한 변환 (의존성 분리)
    public VerifyCodeServiceRequestDto toServiceRequestDto() {
        return new VerifyCodeServiceRequestDto(this.email, this.code);
    }
}
