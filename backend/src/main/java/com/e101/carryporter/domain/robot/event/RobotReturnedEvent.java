package com.e101.carryporter.domain.robot.event;

/**
 * 로봇이 관리소에 도착했을 때 발행되는 이벤트
 * - MQTT로부터 관리소 도착 메시지 수신 시 발행
 * - AdminNotificationHandler가 관리자에게 SSE 알림 전송
 */
public record RobotReturnedEvent(
        Long missionId,
        Long userId,
        Long robotId,
        String robotMacAddress
) {
}
