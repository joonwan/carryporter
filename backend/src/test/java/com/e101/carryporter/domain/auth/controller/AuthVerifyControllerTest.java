package com.e101.carryporter.domain.auth.controller;

import com.e101.carryporter.domain.auth.controller.dto.request.VerifyPasswordRequestDto;
import com.e101.carryporter.domain.auth.service.dto.request.VerifyPasswordServiceRequestDto;
import com.e101.carryporter.support.WebMvcTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.willDoNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthVerifyControllerTest extends WebMvcTestSupport {

    @Test
    @DisplayName("올바른 미션ID와 비밀번호 입력 시 인증에 성공한다.")
    void verifyPasswordSuccess() throws Exception {
        // given
        Long userId = 1L; // 가상의 유저 ID
        Long missionId = 100L;
        Integer password = 1234;
        VerifyPasswordRequestDto requestDto = new VerifyPasswordRequestDto(missionId, password);

        // stubbing
        willDoNothing().given(authService).unlockRequest(any(VerifyPasswordServiceRequestDto.class));

        // when & then
        mockMvc.perform(post("/auth/unlock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto))
                        .requestAttr("userId", userId)) // ✅ 핵심: 필터가 넣어준 것처럼 속성 주입
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("비밀번호 인증 요청 성공"));
    }

    @Test
    @DisplayName("비밀번호 누락 시 400 에러를 반환한다.")
    void verifyPasswordValidationFail() throws Exception {
        // given
        VerifyPasswordRequestDto requestDto = new VerifyPasswordRequestDto(100L, null);

        // when & then
        mockMvc.perform(post("/auth/unlock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto))
                        .requestAttr("userId", 1L)) // Validation 체크 중에도 userId는 필요하다고 가정
                .andDo(print())
                .andExpect(status().isBadRequest());
    }
}