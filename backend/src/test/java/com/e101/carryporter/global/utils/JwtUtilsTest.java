package com.e101.carryporter.global.utils;

import com.e101.carryporter.domain.user.entity.Role;
import com.e101.carryporter.support.IntegrationTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

class JwtUtilsTest extends IntegrationTestSupport {

    @Autowired
    private JwtUtils jwtUtils; // 스프링이 만들어준 객체를 가져다 씀 (application.yml 설정 적용됨)

    @Test
    @DisplayName("Access Token 생성 및 검증 테스트 (일반 사용자)")
    void accessTokenTest() {
        // given (준비)
        String email = "test@ssafy.com";
        Long userId = 1L;
        Role role = Role.BASIC;

        // when (실행: Access Token 만들기)
        String token = jwtUtils.createAccessToken(email, userId, role);
        System.out.println("생성된 Access Token: " + token);

        // then (검증)
        // 1. 토큰이 null이 아니어야 함
        assertThat(token).isNotNull();

        // 2. 토큰이 유효하다고 판단되어야 함
        assertThat(jwtUtils.validateToken(token)).isTrue();

        // 3. 토큰의 주인(Subject)이 이메일과 같은지 확인
        String extractedEmail = jwtUtils.getMmEmailFromToken(token);
        assertThat(extractedEmail).isEqualTo(email);

        // 4. 토큰의 Claim(userId)이 원래 ID와 같은지 확인
        Long extractedUserId = jwtUtils.getUserIdFromToken(token);
        assertThat(extractedUserId).isEqualTo(userId);

        // 5. 토큰의 Claim(role)이 원래 role과 같은지 확인
        Role extractedRole = jwtUtils.getRoleFromToken(token);
        assertThat(extractedRole).isEqualTo(role);
    }

    @Test
    @DisplayName("Access Token 생성 및 검증 테스트 (관리자)")
    void accessTokenTestForAdmin() {
        // given
        String email = "admin@ssafy.com";
        Long userId = 2L;
        Role role = Role.ADMIN;

        // when
        String token = jwtUtils.createAccessToken(email, userId, role);
        System.out.println("생성된 Admin Access Token: " + token);

        // then
        assertThat(token).isNotNull();
        assertThat(jwtUtils.validateToken(token)).isTrue();
        assertThat(jwtUtils.getMmEmailFromToken(token)).isEqualTo(email);
        assertThat(jwtUtils.getUserIdFromToken(token)).isEqualTo(userId);
        assertThat(jwtUtils.getRoleFromToken(token)).isEqualTo(Role.ADMIN);
    }

    @Test
    @DisplayName("Refresh Token 생성 및 검증 테스트")
    void refreshTokenTest() {
        // given
        Long userId = 100L;

        // when (실행: Refresh Token 만들기)
        String refreshToken = jwtUtils.createRefreshToken(userId);
        System.out.println("생성된 Refresh Token: " + refreshToken);

        // then (검증)
        // 1. 토큰 존재 여부
        assertThat(refreshToken).isNotNull();

        // 2. 유효성 검증
        assertThat(jwtUtils.validateToken(refreshToken)).isTrue();

        // 3. Subject 확인 (Refresh Token은 이메일 대신 userId(String)를 Subject로 넣었음)
        // 메서드 이름이 getMmEmailFromToken이라 헷갈리지만, 실제로는 Subject를 꺼내는 메서드임
        String subject = jwtUtils.getMmEmailFromToken(refreshToken);

        // "100" (String) == "100" (String) 인지 확인
        assertThat(subject).isEqualTo(String.valueOf(userId));

        // 4. Claim 확인 (userId가 Claim에도 들어있는지 확인)
        Long extractedUserId = jwtUtils.getUserIdFromToken(refreshToken);
        assertThat(extractedUserId).isEqualTo(userId);
    }
}