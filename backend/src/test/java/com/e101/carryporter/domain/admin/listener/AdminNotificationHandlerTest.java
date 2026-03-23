package com.e101.carryporter.domain.admin.listener;

import com.e101.carryporter.domain.robot.event.RobotAssignedEvent;
import com.e101.carryporter.domain.robot.event.RobotReturnedAdminEvent;
import com.e101.carryporter.domain.sse.listener.AdminSseNotificationHandler;
import com.e101.carryporter.domain.sse.service.SseService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AdminNotificationHandlerTest {

    @InjectMocks
    AdminSseNotificationHandler adminNotificationHandler;

    @Mock
    SseService sseService;

    @DisplayName("사용자 호출 시(첫 호출/재호출) 관리자에게 RobotAssignedEvent를 브로드캐스트한다")
    @Test
    void handleRobotAssignedEventTest() {
        // given
        RobotAssignedEvent event = RobotAssignedEvent.builder()
                .userId(1L)
                .missionId(10L)
                .robotCode("R-001")
                .callLocationName("A-Zone")
                .lockerCode(null) // FIRST 호출 가정
                .requestType("FIRST/RECALL")
                .build();

        // when
        adminNotificationHandler.handleRobotAssignedEvent(event);

        // then
        // 백엔드 핸들러 코드에 따라 Map이 아닌 event 객체 그대로 전송되는지 확인
        verify(sseService).broadcastToAdmins(eq("RobotAssignedEvent"), eq(event));
    }

    @DisplayName("로봇이 복귀했을 때 관리자에게 RobotReturnedAdminEvent를 브로드캐스트한다")
    @Test
    void handleRobotReturnedAdminEventTest() {
        // given
        RobotReturnedAdminEvent event = new RobotReturnedAdminEvent(
                1L,        // userId
                "R-001",   // robotCode
                10L,       // missionId
                2L,
                "L-05",    // lockerCode
                "로봇이 복귀했습니다. 박스를 사물함에 넣고 상태를 선택하세요." // message
        );

        // when
        adminNotificationHandler.handleRobotReturnedAdminEvent(event);

        // then
        verify(sseService).broadcastToAdmins(eq("RobotReturnedAdminEvent"), eq(event));
    }
}