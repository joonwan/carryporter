package com.e101.carryporter.domain.sse.service;

import com.e101.carryporter.domain.sse.repository.SseEmitterRepository;
import com.e101.carryporter.domain.user.entity.Role;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class SseService {

    private final SseEmitterRepository emitterRepository;

    // 연결 유지 시간: 60분
    private static final Long DEFAULT_TIMEOUT = 60L * 1000 * 60;

    public SseEmitter subscribe(Long id, String role) {
        // [핵심] 기존에 연결된 Emitter가 있다면 강제로 끊어서 정리
        stopExistingEmitter(id, role);

        SseEmitter emitter = new SseEmitter(DEFAULT_TIMEOUT);

        // 콜백 설정 - emitter 인스턴스 비교로 race condition 방지
        // OLD emitter의 콜백이 뒤늦게 실행되어도 NEW emitter를 삭제하지 않음
        emitter.onCompletion(() -> removeEmitterIfMatch(id, role, emitter, "Completion"));
        emitter.onTimeout(() -> removeEmitterIfMatch(id, role, emitter, "Timeout"));
        emitter.onError((e) -> removeEmitterIfMatch(id, role, emitter, "Error"));

        // Repository 저장
        if (Role.ADMIN.name().equals(role)) {
            emitterRepository.saveAdmin(id, emitter);
        } else {
            emitterRepository.saveUser(id, emitter);
        }

        // 최초 연결 더미 전송 (이때 실패하면 즉시 정리됨)
        sendToClient(emitter, id, "CONNECT", "Connected! [Role: " + role + "]");

        return emitter;
    }

    /**
     * 기존 연결을 찾아 명시적으로 종료시키는 메서드
     */
    private void stopExistingEmitter(Long id, String role) {
        SseEmitter existing;
        if (Role.ADMIN.name().equals(role)) {
            existing = emitterRepository.findAdmin(id);
        } else {
            existing = emitterRepository.findUser(id);
        }

        if (existing != null) {
            log.info("[SSE] 기존 연결 종료 시도 | ID: {}", id);
            try {
                existing.complete();
            } catch (Exception e) {
                log.warn("[SSE] 기존 연결 종료 중 에러 발생 | ID: {}", id);
            }
        }
    }

    /**
     * 맵에 저장된 emitter가 자신과 동일한 인스턴스일 때만 제거 (race condition 방지)
     */
    private void removeEmitterIfMatch(Long id, String role, SseEmitter emitter, String reason) {
        boolean removed;
        if (Role.ADMIN.name().equals(role)) {
            removed = emitterRepository.deleteAdminIfMatch(id, emitter);
        } else {
            removed = emitterRepository.deleteUserIfMatch(id, emitter);
        }
        if (removed) {
            log.debug("[SSE] 연결 제거 완료 | ID: {} | 사유: {}", id, reason);
        } else {
            log.debug("[SSE] 연결 제거 스킵 (이미 새 연결로 교체됨) | ID: {} | 사유: {}", id, reason);
        }
    }

    /**
     * 중앙 집중형 하트비트 - 15초마다 전체 연결에 ping
     */
    @Scheduled(fixedRate = 15000) // 15초마다 실행
    public void sendHeartbeat() {
        Map<Long, SseEmitter> allEmitters = new HashMap<>();
        allEmitters.putAll(emitterRepository.findAllUsers());
        allEmitters.putAll(emitterRepository.findAllAdmins());

        if (!allEmitters.isEmpty()) {
            log.info("[SSE-HEARTBEAT] 하트비트 전송 | 연결 수: {}", allEmitters.size());
        }

        allEmitters.forEach((id, emitter) -> {
            try {
                emitter.send(SseEmitter.event().name("heartbeat").data("ping"));
            } catch (IOException e) {
                // 연결이 끊긴 것이 확인되면 Repository에서 제거
                log.info("[SSE-HEARTBEAT] 하트비트 전송 실패, 연결 정리 | ID: {}", id);
                emitter.completeWithError(e);
                // 양쪽 다 시도 (role 정보를 알 수 없으므로)
                emitterRepository.deleteUser(id);
                emitterRepository.deleteAdmin(id);
            }
        });
    }

    /**
     * [USER] 모든 사용자에게 알림 전송
     */
    public void broadcastToUsers(String eventName, Object data) {
        Map<Long, SseEmitter> users = emitterRepository.findAllUsers();

        log.info("[SSE-SERVICE] 사용자 전체 전송 | event={} | count={}",
                eventName, users.size());

        users.forEach((id, emitter) -> {
            sendToClient(emitter, id, eventName, data);
        });
    }

    /**
     * [ADMIN] 모든 관리자에게 알림 전송
     */
    public void broadcastToAdmins(String eventName, Object data) {
        Map<Long, SseEmitter> admins = emitterRepository.findAllAdmins();
        log.info("[SSE-SERVICE] 관리자 전체 전송 | event={} | count={}",
                eventName, admins.size());
        admins.forEach((id, emitter) -> {
            sendToClient(emitter, id, eventName, data);
        });
    }

    /**
     * [USER] 특정 사용자에게 알림 전송
     */
    public void sendToUser(Long userId, String eventName, Object data) {
        SseEmitter emitter = emitterRepository.findUser(userId);
        if (emitter != null) {
            log.debug("[SSE-SERVICE] 사용자 전송 시도 | ID: {} | Event: {}", userId, eventName);
            sendToClient(emitter, userId, eventName, data);
        } else {
            log.warn("[SSE-SERVICE] 전송 실패 (구독 중인 유저 없음) | ID: {}", userId);
        }
    }

    /**
     * 실제 전송 로직
     */
    private void sendToClient(SseEmitter emitter, Long id, String eventName, Object data) {
        if (emitter == null) return;

        try {
            emitter.send(SseEmitter.event()
                    .id(String.valueOf(id))
                    .name(eventName)
                    .data(data));
            log.debug("[SSE-SERVICE] 클라이언트에게 전송 완료 | ID: {}", id);
        } catch (IOException e) {
            log.debug("[SSE-SERVICE] 클라이언트 연결 끊김 (IOException) | ID: {}", id);
            // 여기서 발생한 IOException이 DispatcherServlet까지 가지 않도록 catch하여 종료 처리
            emitter.completeWithError(e);
            // 양쪽 다 제거 시도
            emitterRepository.deleteUser(id);
            emitterRepository.deleteAdmin(id);
        } catch (Exception e) {
            log.error("[SSE-SERVICE] 알 수 없는 전송 에러 | ID: {}", id, e);
        }
    }
}
