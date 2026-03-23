package com.e101.carryporter.domain.locker.entity;

public enum LockerStatus {

    AVAILABLE("비어있음"), OCCUPIED("사용중")
    ;

    private final String description;

    LockerStatus(String description) {
        this.description = description;
    }
}
