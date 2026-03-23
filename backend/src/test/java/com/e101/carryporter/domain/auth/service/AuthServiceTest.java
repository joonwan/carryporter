package com.e101.carryporter.domain.auth.service;

import com.e101.carryporter.domain.auth.repository.TempPasswordRedisRepository;
import com.e101.carryporter.domain.auth.service.dto.request.AuthServiceReqeustDto;
import com.e101.carryporter.domain.auth.service.dto.request.VerifyCodeServiceRequestDto;
import com.e101.carryporter.domain.auth.repository.EmailCodeRedisRepository;
import com.e101.carryporter.domain.auth.controller.dto.response.AuthResponseDto;
import com.e101.carryporter.domain.auth.controller.dto.response.TokenResponseDto;
import com.e101.carryporter.domain.user.entity.Role;
import com.e101.carryporter.domain.user.repository.UserRepository;
import com.e101.carryporter.global.utils.JwtUtils;
import com.e101.carryporter.support.IntegrationTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

class AuthServiceTest extends IntegrationTestSupport {

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private AuthService authService;

    @Autowired
    private EmailCodeRedisRepository emailCodeRepository;

    @Autowired
    private TempPasswordRedisRepository tempPasswordRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("인증번호를 요청하면 실제 Redis에 데이터가 저장되어야 한다.")
    void requestAuthIntegrationTest() {
        // given
        String email = "test@ssafy.com";
        AuthServiceReqeustDto command = new AuthServiceReqeustDto(email, 1234);

        // when
        AuthResponseDto response = authService.requestAuth(command);

        // then
        assertThat(response.getStatus()).isEqualTo("SUCCESS");
        // 진짜 Redis에서 값을 꺼내어 확인
        assertThat(emailCodeRepository.get(email)).isPresent();
    }

    @Test
    @DisplayName("인증번호가 일치하면 유저가 DB에 저장되고 토큰이 발급된다.")
    void verifyAuthIntegrationTest() {
        // given
        String email = "verify@ssafy.com";
        Integer code = 99;
        Integer tempPassword = 1234; // 서비스 로직에서 요구하는 임시 비번

        // 1. 인증번호 저장
        emailCodeRepository.save(email, code);

        // 2. ✅ 아까 빠뜨린 부분: 임시 비밀번호도 Redis에 같이 있어야 함!
        tempPasswordRepository.save(email, tempPassword);

        VerifyCodeServiceRequestDto command = new VerifyCodeServiceRequestDto(email, code);

        // when
        TokenResponseDto response = authService.verifyAuth(command);

        // then
        assertThat(response.getAccessToken()).isNotBlank();
        assertThat(response.getRefreshToken()).isNotBlank();
        assertThat(userRepository.findByMmEmail(email)).isPresent();
    }

    @Test
    @DisplayName("이미 가입된 유저가 다시 인증하면 새로운 유저를 생성하지 않고 기존 유저 정보를 사용한다.")
    void verifyAuthWithExistingUserTest() {
        // given
        String email = "existing@ssafy.com";
        Integer code1 = 123;
        Integer code2 = 456;
        Integer tempPassword = 1111;

        // 1. 첫 번째 인증 진행 (유저 생성)
        emailCodeRepository.save(email, code1);
        tempPasswordRepository.save(email, tempPassword);
        TokenResponseDto firstResponse = authService.verifyAuth(new VerifyCodeServiceRequestDto(email, code1));

        // 첫 번째 인증으로 생성된 userId 추출
        Long firstUserId = jwtUtils.getUserIdFromToken(firstResponse.getAccessToken());

        // 2. 두 번째 인증 진행 (기존 유저 재사용 확인용)
        emailCodeRepository.save(email, code2);
        tempPasswordRepository.save(email, tempPassword);
        VerifyCodeServiceRequestDto secondCommand = new VerifyCodeServiceRequestDto(email, code2);

        // when
        TokenResponseDto secondResponse = authService.verifyAuth(secondCommand);

        // then
        // [핵심] UserRepository를 쓰지 않고 토큰 정보로만 검증
        Long secondUserId = jwtUtils.getUserIdFromToken(secondResponse.getAccessToken());

        // 두 번의 인증 결과로 나온 userId가 동일하다면, 내부적으로 중복 생성되지 않았음을 증명함
        assertThat(secondUserId).isEqualTo(firstUserId);
        assertThat(secondResponse.getAccessToken()).isNotBlank();
    }

    @Test
    @DisplayName("일반 사용자 인증 시 발급된 액세스 토큰에 BASIC 권한이 포함된다")
    void verifyAuthAccessTokenContainsBasicRole() {
        // given
        String email = "user@ssafy.com";
        Integer code = 99;
        Integer tempPassword = 1234;

        emailCodeRepository.save(email, code);
        tempPasswordRepository.save(email, tempPassword);

        VerifyCodeServiceRequestDto command = new VerifyCodeServiceRequestDto(email, code);

        // when
        TokenResponseDto response = authService.verifyAuth(command);

        // then
        assertThat(response.getAccessToken()).isNotBlank();

        // JWT 토큰에서 role 추출
        Role extractedRole = jwtUtils.getRoleFromToken(response.getAccessToken());
        assertThat(extractedRole).isEqualTo(Role.BASIC);
    }

    @Test
    @DisplayName("일반 사용자 인증 시 발급된 액세스 토큰에서 모든 정보를 추출할 수 있다")
    void verifyAuthAccessTokenContainsAllInfo() {
        // given
        String email = "user@ssafy.com";
        Integer code = 99;
        Integer tempPassword = 1234;

        emailCodeRepository.save(email, code);
        tempPasswordRepository.save(email, tempPassword);

        VerifyCodeServiceRequestDto command = new VerifyCodeServiceRequestDto(email, code);

        // when
        TokenResponseDto response = authService.verifyAuth(command);

        // then
        String accessToken = response.getAccessToken();
        assertThat(accessToken).isNotBlank();

        // JWT에서 모든 정보 추출 및 검증
        assertThat(jwtUtils.getMmEmailFromToken(accessToken)).isEqualTo(email);
        assertThat(jwtUtils.getUserIdFromToken(accessToken)).isNotNull();
        assertThat(jwtUtils.getRoleFromToken(accessToken)).isEqualTo(Role.BASIC);
    }
}