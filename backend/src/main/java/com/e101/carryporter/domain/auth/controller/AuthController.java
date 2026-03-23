package com.e101.carryporter.domain.auth.controller;

import com.e101.carryporter.domain.auth.controller.dto.request.AuthRequestDto;
import com.e101.carryporter.domain.auth.controller.dto.request.LockRequestDto;
import com.e101.carryporter.domain.auth.controller.dto.request.VerifyCodeRequestDto;
import com.e101.carryporter.domain.auth.controller.dto.request.VerifyPasswordRequestDto;
import com.e101.carryporter.domain.auth.controller.dto.response.AuthResponseDto;
import com.e101.carryporter.domain.auth.controller.dto.response.TokenResponseDto;
import com.e101.carryporter.domain.auth.service.AuthService;
import com.e101.carryporter.domain.auth.service.dto.request.LockServiceRequestDto;
import com.e101.carryporter.domain.auth.service.dto.request.VerifyPasswordServiceRequestDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 1단계: 인증번호 요청
     */
    @PostMapping("/request")
    public ResponseEntity<AuthResponseDto> requestAuth(@Valid @RequestBody AuthRequestDto requestDto) {
        AuthResponseDto response = authService.requestAuth(requestDto.toServiceRequestDto());
        return ResponseEntity.ok(response);
    }



    /**
     * 2단계: 인증번호 검증 및 토큰 발급 (수정 완료)
     */
    @PostMapping("/verify")
    public ResponseEntity<TokenResponseDto> verifyAuth(@Valid @RequestBody VerifyCodeRequestDto requestDto) {
        // 1. 서비스에서 토큰 정보(Access + Refresh + 만료시간 등)를 다 받아옴
        TokenResponseDto allTokens = authService.verifyAuth(requestDto.toServiceRequestDto());

        // 2. Refresh Token은 HttpOnly 쿠키로 굽기
        ResponseCookie refreshCookie = createRefreshTokenCookie(allTokens.getRefreshToken());

        // 3. Body에는 Access Token만 담아서 리턴 (Builder 패턴 사용)
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString()) // 쿠키 설정
                .body(TokenResponseDto.builder()
                        .accessToken(allTokens.getAccessToken())    // 서비스에서 받은 값 꺼내기
                        .refreshToken(null)                         // ★ 보안: Body에는 Refresh Token을 주지 않음 (null 처리)
                        .grantType("Bearer")                        // 혹은 allTokens.getGrantType()
                        .expiresIn(allTokens.getExpiresIn())        // 혹은 allTokens.getExpiresIn()
                        .build());
    }

    /**
     * 토큰 재발급 (수정 완료)
     */
    @PostMapping("/reissue")
    public ResponseEntity<TokenResponseDto> reissue(
            @CookieValue(name = "refreshToken", required = false) String refreshToken
    ) {
        if (refreshToken == null) {
            return ResponseEntity.status(401).build();
        }

        // 1. 서비스에서 재발급
        TokenResponseDto newTokens = authService.reissue(refreshToken);

        // 2. Refresh Token Rotation (새로 발급된 리프레쉬 토큰 쿠키 굽기)
        ResponseCookie refreshCookie = createRefreshTokenCookie(newTokens.getRefreshToken());

        // 3. 응답 반환 (Builder 패턴 사용)
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(TokenResponseDto.builder()
                        .accessToken(newTokens.getAccessToken())
                        .refreshToken(null) // ★ 보안: 여기서도 Body엔 넣지 않음
                        .grantType("Bearer")
                        .expiresIn(newTokens.getExpiresIn())
                        .build());
    }

    /**
     * 로그아웃
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        ResponseCookie deleteCookie = ResponseCookie.from("refreshToken", "")
                .path("/")
                .maxAge(0) // 즉시 만료
                .sameSite("None")
                .secure(true)
                .httpOnly(true)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, deleteCookie.toString())
                .build();
    }

    // --- Helper Method ---
    private ResponseCookie createRefreshTokenCookie(String refreshToken) {
        return ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(7 * 24 * 60 * 60)
                .sameSite("None")
                .build();
    }

    @PostMapping("/unlock")
    public ResponseEntity<String> verifyPassword(@RequestAttribute("userId") Long userId, @RequestBody @Valid VerifyPasswordRequestDto request){
        VerifyPasswordServiceRequestDto command = new VerifyPasswordServiceRequestDto(
                userId,
                request.missionId(),
                request.password()
        );

        authService.unlockRequest(command);

        return ResponseEntity.ok("비밀번호 인증 요청 성공");
    }

    @PostMapping("/lock")
    public ResponseEntity<String> lockRequest(@RequestAttribute("userId") Long userId, @RequestBody @Valid LockRequestDto request){
        LockServiceRequestDto command = new LockServiceRequestDto(
                userId,
                request.missionId()
        );
        //서비스 로직 -> missionLockEvent 발행
        authService.lockRequest(command);

        return ResponseEntity.ok("잠금 요청 성공");
    }
}