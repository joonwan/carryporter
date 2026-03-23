package com.e101.carryporter.domain.mission.entity;

import com.e101.carryporter.domain.locker.entity.LockerStatus;

public enum MissionStatus {
    REQUESTED("미션 요청 접수"),
    ASSIGNED("로봇 배정 완료"),
    MOVING("사용자에게 이동 중"),
    ARRIVED("목적지 도착 완료"),
    UNLOCKED("사용자 인증 및 잠금 해제"),
    LOCKED("수하물 적재 및 잠금 완료"),
    RETURNING("스테이션으로 복귀 중"),
    RETURNED("스테이션 복귀 완료"),
    STORING("저장 중"),
    FINISHED("서비스 종료 및 기록 완료"),
    FAILED("미션 실패");
    ;

    private final String description;

    MissionStatus(String description) {
        this.description = description;
    }

}
