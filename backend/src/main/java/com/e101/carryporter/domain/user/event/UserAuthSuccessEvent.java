package com.e101.carryporter.domain.user.event;

public record UserAuthSuccessEvent(
        Long missionId,
        Long userId,
        String robotMacAddress
) {
    //잠시 메모용 주석입니당 개발 하면서 삭제할게욥!!
    //비밀번호 인증 성공을 하면 반응하는건 ? -> 사용자 화면 변화, 로봇 열림
    // sse : 누구화면을 바꿔 -> userId 필요
    // mqtt : 어떤 로봇 문을 열어 -> robotMacAddress 필요
    //어떤 미션인지 로그는 남겨야지 -> missionId 필요

}
