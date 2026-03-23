package com.e101.carryporter.domain.robot.event;

import lombok.Builder;

//로봇 배정 완료 후에 첫 호출이면 {requestType : "FIRST"}, 재호출이면 {requestType:"RECALL"} 해당 이벤트 발생 시켜주세요!!
@Builder
public record RobotAssignedEvent(
        Long userId,            // 호출한 사용자 ID
        Long missionId,         // 현재 진행 중인 미션 식별자
        String robotCode,       // 배정된 로봇 코드
        String callLocationName,  // 호출지 정보 (첫 호출 시 필요)
        String lockerCode,        // 사물함 코드 (재호출/복귀 시에만 포함, 첫 호출 시 null)
        String requestType,      // "FIRST" (보관 요청), "RECALL" (수령/반납을 위한 재호출) 등으로 구분 가능
        String robotMacAddress
) {
    /**
     * 관리자 화면은 lockerId 의 null 기준으로 분기 -> 백에서 첫호출/재호출에 따라 사물함 번호를 채워줌
     * 관리자 화면 -> 첫호출
     * {호출지 정보}에서 호출한 {사용자}의 {미션번호}에 {로봇이름}이 배정되었습니다. 사물함을 배정해주세요
     * RobotAssignedEvent event = RobotAssignedEvent.builder()
     *     .userId(userId)
     *     .robotCode(assignedRobotCode)
     *     .missionId(newMissionId)
     *     .callLocationId(locationId)
     *     .build(); // lockerId는 자동으로 null
     *
     * 관리자화면 -> 재호출
     * {호출지 정보}에서 호출한 {사용자}의 {미션번호}에 {로봇이름}이 배정되었습니다.
     * {사물함번호}에 넣어주세요
     */
}