package com.e101.carryporter.domain.admin.controller.dto.response;

import com.e101.carryporter.domain.mission.entity.Mission;
import com.e101.carryporter.domain.mission.entity.MissionStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class MissionResponseDto {

    private Long id;
    private Long userId;
    private Long robotId;
    private String robotCode;
    private Long lockerId;
    private String lockerCode;
    private String callLocationName;
    private MissionStatus missionStatus;
    private LocalDateTime robotAssignedAt;
    private LocalDateTime lockerAssignedAt;
    private LocalDateTime startedAt;
    private LocalDateTime arrivedAt;
    private LocalDateTime finishedAt;
    private LocalDateTime createdAt;

    public static MissionResponseDto from(Mission mission) {
        return MissionResponseDto.builder()
                .id(mission.getId())
                .userId(mission.getUser().getId())
                .robotId(mission.getRobot() != null ? mission.getRobot().getId() : null)
                .robotCode(mission.getRobot() != null ? mission.getRobot().getRobotCode() : null)
                .lockerId(mission.getLocker() != null ? mission.getLocker().getId() : null)
                .lockerCode(mission.getLocker() != null ? mission.getLocker().getLockerCode() : null)
                .callLocationName(mission.getCallLocation().getLocationName())
                .missionStatus(mission.getMissionStatus())
                .robotAssignedAt(mission.getRobotAssignedAt())
                .lockerAssignedAt(mission.getLockerAssignedAt())
                .startedAt(mission.getStartedAt())
                .arrivedAt(mission.getArrivedAt())
                .finishedAt(mission.getFinishedAt())
                .createdAt(mission.getCreatedAt())
                .build();
    }
}
