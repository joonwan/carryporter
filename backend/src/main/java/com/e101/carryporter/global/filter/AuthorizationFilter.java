package com.e101.carryporter.global.filter;

import com.e101.carryporter.domain.user.entity.User;
import com.e101.carryporter.domain.user.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthorizationFilter extends OncePerRequestFilter {

    private final UserRepository userRepository;

    // 인가 검사를 건너뛸 URL 목록 (인증 관련 경로)
    private static final List<String> WHITELIST = Arrays.asList(
            "/auth/request",
            "/auth/verify",
            "/auth/reissue",
            "/api/auth/request",
            "/api/auth/verify",
            "/api/auth/reissue",
            //프론트 테스트 용
            "/api/test/sse",
            "/test/sse"
    );

    // 관리자 전용 URL 목록
    private static final List<String> ADMIN_ONLY_PATHS = Arrays.asList(
            "/admin",
            "/api/admin",
            "/admin/join",
            "/admin/login"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        log.debug("authorization filter 호출!!");
        String requestURI = request.getServletPath();

        // 1. 화이트리스트에 있는 경로는 인가 검사 건너뜀
        if (isWhitelisted(requestURI)) {
            log.debug("white list !!");
            filterChain.doFilter(request, response);
            return;
        }

        // 2. JwtAuthenticationFilter에서 설정한 userId 가져오기
        Long userId = (Long) request.getAttribute("userId");

        // userId가 없으면 인증되지 않은 요청 (JwtAuthenticationFilter에서 이미 처리됨)
        if (userId == null) {
            log.debug("인증되지 않은 요청");
            filterChain.doFilter(request, response);
            return;
        }

        // 3. 관리자 전용 경로 확인
        if (isAdminOnlyPath(requestURI)) {
            Optional<User> userOptional = userRepository.findById(userId);

            if (userOptional.isEmpty()) {
                log.warn("인가 실패 - 사용자를 찾을 수 없음: userId={}, uri={}", userId, requestURI);
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "접근 권한이 없습니다.");
                return;
            }

            User user = userOptional.get();
            if (!user.isAdmin()) {
                log.warn("인가 실패 - 관리자 권한 필요: userId={}, role={}, uri={}", userId, user.getRole(), requestURI);
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "관리자 권한이 필요합니다.");
                return;
            }

            // 역할 정보를 request에 저장 (컨트롤러에서 사용 가능)
            log.info("인가 성공 - 관리자 접근: userId={}, uri={}, role = {}", userId, requestURI, user.getRole());
        }

        filterChain.doFilter(request, response);
    }

    private boolean isWhitelisted(String uri) {
        return WHITELIST.stream().anyMatch(uri::startsWith);
    }

    private boolean isAdminOnlyPath(String uri) {
        return ADMIN_ONLY_PATHS.stream().anyMatch(uri::startsWith);
    }
}
