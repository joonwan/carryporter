package com.e101.carryporter.domain.admin.controller.dto.response;

import com.e101.carryporter.domain.robot.entity.Robot;
import com.e101.carryporter.domain.robot.entity.RobotStatus;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RobotResponseDto {

    private Long id;
    private String robotCode;
    private String macAddress;
    private RobotStatus robotStatus;

    public static RobotResponseDto from(Robot robot) {
        return RobotResponseDto.builder()
                .id(robot.getId())
                .robotCode(robot.getRobotCode())
                .macAddress(robot.getMacAddress())
                .robotStatus(robot.getRobotStatus())
                .build();
    }
}
