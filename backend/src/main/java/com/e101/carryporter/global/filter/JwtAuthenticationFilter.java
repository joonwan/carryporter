package com.e101.carryporter.global.filter;

import com.e101.carryporter.domain.user.entity.Role;
import com.e101.carryporter.global.utils.JwtUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;

    // 검사를 건너뛸 URL 목록 (context-path 이후의 경로)
    private static final List<String> WHITELIST = Arrays.asList(
            "/auth/request", // 인증번호 요청
            "/auth/verify",   // 로그인
            "/auth/reissue",
            "/api/auth/request",
            "/api/auth/verify",
            "/api/auth/reissue",
            "/admin/join",    // 관리자 회원가입
            "/admin/login",   // 관리자 로그인
            "/api/admin/join",
            "/api/admin/login",   // 관리자 로그인
            "/api/test/sse",
            "/test/sse"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        String requestURI = request.getServletPath();

        log.info("request uri = {}", requestURI);

        // 1. 화이트리스트에 있는 주소는 검사 안 하고 통과 (context-path 제외된 경로로 비교)
        if (isWhitelisted(requestURI)) {
            log.info("화이트리스트 통과");
            filterChain.doFilter(request, response);
            return;
        }

        // 2. 헤더에서 토큰 꺼내기
        String token = resolveToken(request);

        // 3. 토큰 유효성 검사
        if (token != null && jwtUtils.validateToken(token)) {
            // 토큰이 유효하면 유저 정보를 request에 담아둠 (컨트롤러에서 쓰기 위해)

            String email = jwtUtils.getMmEmailFromToken(token);
            Long userId = jwtUtils.getUserIdFromToken(token);
            Role role = jwtUtils.getRoleFromToken(token);

            request.setAttribute("mmEmail", email);
            request.setAttribute("userId", userId);
            request.setAttribute("role", role);

            log.debug("사용자 정보: mmId = {}, userId = {}, role = {}", userId, userId, role);

            filterChain.doFilter(request, response); // 통과!
        } else {
            // 4. 토큰이 없거나 이상하면 401 에러 (진입 차단)
            log.info("유효하지 않은 토큰 접근: {}", requestURI);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "인증되지 않은 사용자입니다.");
        }
    }

    // Authorization 헤더에서 "Bearer " 떼고 토큰만 가져오는 메서드
    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    private boolean isWhitelisted(String uri) {
        return WHITELIST.stream().anyMatch(uri::startsWith);
    }
}
