package com.e101.carryporter.domain.auth.controller;

import com.e101.carryporter.domain.auth.controller.dto.request.AuthRequestDto;
import com.e101.carryporter.domain.auth.controller.dto.request.VerifyCodeRequestDto;
import com.e101.carryporter.domain.auth.controller.dto.response.AuthResponseDto;
import com.e101.carryporter.domain.auth.controller.dto.response.TokenResponseDto;
import com.e101.carryporter.domain.auth.service.dto.request.AuthServiceReqeustDto;
import com.e101.carryporter.domain.auth.service.dto.request.VerifyCodeServiceRequestDto;
import com.e101.carryporter.support.WebMvcTestSupport;
import jakarta.servlet.http.Cookie; // 쿠키 테스트를 위해 필요
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AuthControllerTest extends WebMvcTestSupport {

    // --- 1. 인증번호 요청 (기존 코드 유지) ---
    @Test
    @DisplayName("올바른 이메일과 비밀번호로 요청 시 200 OK 응답을 받는다.")
    void requestAuthSuccess() throws Exception {
        // given
        AuthRequestDto requestDto = new AuthRequestDto("correct@ssafy.com", 1234);

        given(authService.requestAuth(any(AuthServiceReqeustDto.class)))
                .willReturn(new AuthResponseDto("SUCCESS", "인증번호가 전송되었습니다.", 82, 123));

        // when & then
        mockMvc.perform(post("/auth/request")
                        .content(objectMapper.writeValueAsString(requestDto))
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print()) // 테스트 결과를 콘솔에 출력 (디버깅용)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("인증번호가 전송되었습니다."));
    }

    @Test
    @DisplayName("잘못된 이메일 형식 요청 시 400 에러가 발생한다.")
    void requestAuthValidationError() throws Exception {
        // given
        AuthRequestDto requestDto = new AuthRequestDto("wrong-email-format", 1234);

        // when & then
        mockMvc.perform(post("/auth/request")
                        .content(objectMapper.writeValueAsString(requestDto))
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    // --- 2. [NEW] 인증번호 검증 및 토큰 발급 테스트 ---
    @Test
    @DisplayName("인증번호 검증 성공 시 AccessToken은 Body에, RefreshToken은 Cookie에 담긴다.")
    void verifyAuthSuccess() throws Exception {
        // given
        VerifyCodeRequestDto requestDto = new VerifyCodeRequestDto("correct@ssafy.com", 1234);

        // Service는 모든 정보가 담긴 DTO를 리턴한다고 가정 (Builder 사용)
        TokenResponseDto tokenResponse = TokenResponseDto.builder()
                .accessToken("access-token-example")
                .refreshToken("refresh-token-example") // Service에서는 값이 있음
                .grantType("Bearer")
                .expiresIn(3600L)
                .build();

        given(authService.verifyAuth(any(VerifyCodeServiceRequestDto.class)))
                .willReturn(tokenResponse);

        // when & then
        mockMvc.perform(post("/auth/verify")
                        .content(objectMapper.writeValueAsString(requestDto))
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                // 1. Body 검증: AccessToken과 GrantType은 있어야 함
                .andExpect(jsonPath("$.accessToken").value("access-token-example"))
                .andExpect(jsonPath("$.grantType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(3600))
                // 2. Body 검증: RefreshToken은 Body에 없어야 함 (null)
                .andExpect(jsonPath("$.refreshToken").doesNotExist())
                // 3. Cookie 검증: RefreshToken이 쿠키에 있어야 함
                .andExpect(cookie().value("refreshToken", "refresh-token-example"))
                .andExpect(cookie().httpOnly("refreshToken", true)) // HttpOnly 여부 확인
                .andExpect(cookie().secure("refreshToken", true));  // Secure 여부 확인
    }

    // --- 3. [NEW] 토큰 재발급 테스트 ---
    @Test
    @DisplayName("리프레쉬 토큰이 담긴 쿠키로 재발급 요청 시 성공한다.")
    void reissueSuccess() throws Exception {
        // given
        String oldRefreshToken = "old-refresh-token";

        // 재발급 결과
        TokenResponseDto newTokenResponse = TokenResponseDto.builder()
                .accessToken("new-access-token")
                .refreshToken("new-refresh-token")
                .grantType("Bearer")
                .expiresIn(3600L)
                .build();

        given(authService.reissue(oldRefreshToken))
                .willReturn(newTokenResponse);

        // when & then
        // ★ .cookie()를 사용하여 요청에 쿠키를 포함시킴
        mockMvc.perform(post("/auth/reissue")
                        .cookie(new Cookie("refreshToken", oldRefreshToken)))
                .andDo(print())
                .andExpect(status().isOk())
                // Body 검증
                .andExpect(jsonPath("$.accessToken").value("new-access-token"))
                .andExpect(jsonPath("$.refreshToken").doesNotExist()) // Body엔 없어야 함
                // Cookie 검증 (새 토큰으로 교체되었는지)
                .andExpect(cookie().value("refreshToken", "new-refresh-token"));
    }

    @Test
    @DisplayName("리프레쉬 토큰 쿠키 없이 재발급 요청 시 401 에러가 발생한다.")
    void reissueFail_NoCookie() throws Exception {
        // when & then (쿠키 없이 요청)
        mockMvc.perform(post("/auth/reissue"))
                .andDo(print())
                .andExpect(status().isUnauthorized()); // 401 Expect
    }
}