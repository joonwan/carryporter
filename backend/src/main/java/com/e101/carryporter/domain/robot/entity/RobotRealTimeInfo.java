package com.e101.carryporter.domain.robot.entity;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RobotRealTimeInfo {

    private String macAddress;
    private RobotStatus status;
    private int battery;
    private LocalDateTime updatedAt;

    public static RobotRealTimeInfo of(String macAddress, RobotStatus status, int battery) {
        return RobotRealTimeInfo.builder()
                .macAddress(macAddress)
                .status(status)
                .battery(battery)
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Builder
    private RobotRealTimeInfo(String macAddress, RobotStatus status, int battery, LocalDateTime updatedAt) {
        this.macAddress = macAddress;
        this.status = status;
        this.battery = battery;
        this.updatedAt = updatedAt;
    }
}
