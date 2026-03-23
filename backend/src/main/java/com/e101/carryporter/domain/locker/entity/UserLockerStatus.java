package com.e101.carryporter.domain.locker.entity;

public enum UserLockerStatus {
    READY("배정 대기 중"),
    OCCUPIED("사용 중"),
    COMPLETED("사용 완료")
    ;

    private final String description;

    UserLockerStatus(String description) {
        this.description = description;
    }
}
