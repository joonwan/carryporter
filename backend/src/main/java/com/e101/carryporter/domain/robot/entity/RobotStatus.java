package com.e101.carryporter.domain.robot.entity;

public enum RobotStatus {
    IDLE("가용 가능 상태"),
    BUSY("미션 수행중인 상태"),
    OFFLINE("오프라인 상태");

    private final String description;

    RobotStatus(String description) {
        this.description = description;
    }
}
