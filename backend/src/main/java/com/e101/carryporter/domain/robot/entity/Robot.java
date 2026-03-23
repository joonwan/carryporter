package com.e101.carryporter.domain.robot.entity;

import com.e101.carryporter.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "robots")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Robot extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "robot_id")
    private Long id;

    @Column(unique = true, nullable = false)
    private String robotCode;

    @Column(nullable = false, unique = true)
    private String macAddress;

    @Enumerated(EnumType.STRING)
    private RobotStatus robotStatus;

    public static Robot createRobot(String robotCode, String macAddress) {
        return Robot.builder()
                .robotCode(robotCode)
                .macAddress(macAddress)
                .robotStatus(RobotStatus.IDLE)
                .build();
    }

    @Builder
    private Robot(String robotCode, String macAddress, RobotStatus robotStatus) {
        this.robotCode = robotCode;
        this.macAddress = macAddress;
        this.robotStatus = robotStatus;
    }

    public void changeStatus(RobotStatus newStatus) {
        this.robotStatus = newStatus;
    }
}
