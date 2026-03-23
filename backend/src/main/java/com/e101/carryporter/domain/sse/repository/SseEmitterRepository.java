package com.e101.carryporter.domain.sse.repository;

import org.springframework.stereotype.Repository;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class SseEmitterRepository {

    // 동시성 문제를 방지하기 위해 ConcurrentHashMap 사용

    // 1. 일반 사용자용 저장소 (Key: userId)
    private final Map<Long, SseEmitter> userEmitters = new ConcurrentHashMap<>();

    // 2. 관리자용 저장소 (Key: adminId)
    // 관리자는 '관제' 목적이므로 전체 브로드캐스트가 자주 일어납니다.
    private final Map<Long, SseEmitter> adminEmitters = new ConcurrentHashMap<>();

    /* --- 사용자(User) 관련 메서드 --- */
    public void saveUser(Long userId, SseEmitter emitter) {
        userEmitters.put(userId, emitter);
    }

    public void deleteUser(Long userId) {
        userEmitters.remove(userId);
    }

    /**
     * 특정 emitter 인스턴스와 일치할 때만 제거 (race condition 방지)
     * 재구독 시 OLD emitter의 콜백이 NEW emitter를 삭제하는 것을 방지
     */
    public boolean deleteUserIfMatch(Long userId, SseEmitter emitter) {
        return userEmitters.remove(userId, emitter);
    }

    public SseEmitter findUser(Long userId) {
        return userEmitters.get(userId);
    }

    /* --- 관리자(Admin) 관련 메서드 --- */
    public void saveAdmin(Long adminId, SseEmitter emitter) {
        adminEmitters.put(adminId, emitter);
    }

    public void deleteAdmin(Long adminId) {
        adminEmitters.remove(adminId);
    }

    public boolean deleteAdminIfMatch(Long adminId, SseEmitter emitter) {
        return adminEmitters.remove(adminId, emitter);
    }

    public SseEmitter findAdmin(Long adminId) {
        return adminEmitters.get(adminId);
    }

    // 모든 관리자에게 알림을 보낼 때 사용
    public Map<Long, SseEmitter> findAllAdmins() {
        return adminEmitters;
    }

    public Map<Long, SseEmitter> findAllUsers() {
        return userEmitters;
    }
}