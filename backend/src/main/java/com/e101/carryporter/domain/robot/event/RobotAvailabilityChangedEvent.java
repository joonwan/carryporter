package com.e101.carryporter.domain.robot.event;

import com.e101.carryporter.domain.robot.entity.RobotStatus;

public record RobotAvailabilityChangedEvent(Long robotId, String robotCode, RobotStatus previousStatus, RobotStatus newStatus) { }
