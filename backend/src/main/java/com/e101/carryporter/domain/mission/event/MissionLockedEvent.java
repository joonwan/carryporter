package com.e101.carryporter.domain.mission.event;

public record MissionLockedEvent(
        Long missionId,
        Long userId,
        String robotMacAddress
) {
    //잠시 메모용 주석입니당 개발 하면서 삭제할게욥!!
    //missionId : db에서 해당 미션 상태를 변경하기 위해 필요
    //robotMacAddress : 로봇에게 문잠그기 명령 보내기 위해 필요
    //userId : 사용자 화면에 잠금 알림을 보내고 sse 연결을 끊기 위해 필요
}
