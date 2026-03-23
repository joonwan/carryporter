package com.e101.carryporter.domain.robot.event;

public record RobotArrivalEvent(
        Long missionId, // DB 상태 변경
        Long userId,     // SSE 알림 (사용자 화면 전환: "비밀번호를 입력해주세요")
        String robotCode
) {
}
