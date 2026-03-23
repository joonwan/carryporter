package com.e101.carryporter.domain.sse.controller;
import com.e101.carryporter.domain.sse.service.SseService;
import com.e101.carryporter.domain.user.entity.Role;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/sse")
@RequiredArgsConstructor
@Slf4j
public class SseController {

    private final SseService sseService;

    @GetMapping(value = "/subscribe", produces = "text/event-stream;charset=UTF-8")
    public SseEmitter subscribe(
            // JwtAuthenticationFilter가 헤더를 파싱해서 넣어준 userId
            @RequestAttribute("userId") Long userId,

            // AuthorizationFilter가 관리자 확인 후 넣어준 역할 정보 (null일 수 있음)
            @RequestAttribute(value = "role", required = false) Role userRole,

            HttpServletResponse response
    ) {
        // 1. Nginx 버퍼링 방지 (실시간 전송을 위해 필수)
        response.addHeader("X-Accel-Buffering", "no");

        // 2. Role 정보 결정
        // 필터에서 관리자 확인을 거쳐 userRole을 넣어줬다면 그 값을 쓰고, 아니면 기본 ROLE_USER 사용
        // 1. 권한 확정 로직
        String roleName = userRole.name();

        log.info("[SSE-SUBSCRIBE] 구독 시작 - userId: {}, role: {}", userId, roleName);

        // 3. 서비스 호출
        return sseService.subscribe(userId, roleName);
    }

    /**
     * [2. 알림 발송용] - 브라우저 탭 2번에서 호출하는 곳
     * 이 주소를 호출하면 서버가 내부적으로 sseService를 통해 1번 탭에 알림을 쏩니다.
     */
    @GetMapping("/send/{userId}")
    public String sendTest(
            @PathVariable Long userId,
            @RequestParam String status
    ) {
        // status: eventName이 됨 (예: ASSIGNED, ARRIVED 등)
        String message = "실시간 알림 테스트입니다. 상태: " + status;

        sseService.sendToUser(userId, status, message);

        return "ID [" + userId + "]에게 [" + status + "] 알림 발송 완료!";
    }
}