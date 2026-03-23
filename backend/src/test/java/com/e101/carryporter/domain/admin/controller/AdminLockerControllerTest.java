package com.e101.carryporter.domain.admin.controller;

import com.e101.carryporter.domain.admin.controller.dto.response.LockerResponseDto;
import com.e101.carryporter.domain.locker.entity.LockerStatus;
import com.e101.carryporter.domain.locker.exception.LockerErrorCode;
import com.e101.carryporter.domain.mission.exception.MissionErrorCode;
import com.e101.carryporter.global.exception.BusinessException;
import com.e101.carryporter.support.WebMvcTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.times;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminLockerControllerTest extends WebMvcTestSupport {

    @Test
    @DisplayName("관리자 전체 사물함 조회")
    void getAllLockers() throws Exception {
        // given
        List<LockerResponseDto> lockers = List.of(
                LockerResponseDto.builder()
                        .lockerId(1L)
                        .lockerCode("A-001")
                        .status(LockerStatus.AVAILABLE)
                        .build(),
                LockerResponseDto.builder()
                        .lockerId(2L)
                        .lockerCode("A-002")
                        .status(LockerStatus.OCCUPIED)
                        .build()
        );

        given(adminLockerService.getAllLockers()).willReturn(lockers);

        // when & then
        mockMvc.perform(get("/admin/lockers"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].lockerId").value(1))
                .andExpect(jsonPath("$[0].lockerCode").value("A-001"))
                .andExpect(jsonPath("$[0].status").value("AVAILABLE"))
                .andExpect(jsonPath("$[1].lockerId").value(2))
                .andExpect(jsonPath("$[1].lockerCode").value("A-002"))
                .andExpect(jsonPath("$[1].status").value("OCCUPIED"));
    }

    @Test
    @DisplayName("관리자 사물함 단건 조회")
    void getLocker() throws Exception {
        // given
        LockerResponseDto locker = LockerResponseDto.builder()
                .lockerId(1L)
                .lockerCode("A-001")
                .status(LockerStatus.AVAILABLE)
                .build();

        given(adminLockerService.getLocker(1L)).willReturn(locker);

        // when & then
        mockMvc.perform(get("/admin/lockers/{lockerId}", 1L))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lockerId").value(1))
                .andExpect(jsonPath("$.lockerCode").value("A-001"))
                .andExpect(jsonPath("$.status").value("AVAILABLE"));
    }

    @Test
    @DisplayName("관리자 사물함 단건 조회 - 존재하지 않는 사물함")
    void getLocker_notFound() throws Exception {
        // given
        given(adminLockerService.getLocker(999L))
                .willThrow(new BusinessException(LockerErrorCode.LOCKER_NOT_FOUND));

        // when & then
        mockMvc.perform(get("/admin/lockers/{lockerId}", 999L))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("사물함을 찾을 수 없습니다."));
    }

    // ==================== 사물함 배정 테스트 ====================

    @Test
    @DisplayName("미션에 사물함 배정 시 204 No Content를 반환한다")
    void assignLockerToMission() throws Exception {
        // given
        Long missionId = 1L;
        Long lockerId = 1L;

        willDoNothing()
                .given(adminLockerService)
                .assignLocker(missionId, lockerId);

        // when & then
        mockMvc.perform(post("/admin/missions/{missionId}/lockers/{lockerId}", missionId, lockerId))
                .andDo(print())
                .andExpect(status().isNoContent());

        verify(adminLockerService, times(1)).assignLocker(missionId, lockerId);
    }

    @Test
    @DisplayName("미션에 사물함 배정 시 미션이 존재하지 않으면 404 Not Found를 반환한다")
    void assignLockerToMission_MissionNotFound() throws Exception {
        // given
        Long missionId = 999L;
        Long lockerId = 1L;

        doThrow(new BusinessException(MissionErrorCode.MISSION_NOT_FOUND))
                .when(adminLockerService)
                .assignLocker(missionId, lockerId);

        // when & then
        mockMvc.perform(post("/admin/missions/{missionId}/lockers/{lockerId}", missionId, lockerId))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(MissionErrorCode.MISSION_NOT_FOUND.getMessage()))
                .andExpect(jsonPath("$.status").value("NOT_FOUND"));

        verify(adminLockerService, times(1)).assignLocker(missionId, lockerId);
    }

    @Test
    @DisplayName("미션에 사물함 배정 시 사물함이 존재하지 않으면 404 Not Found를 반환한다")
    void assignLockerToMission_LockerNotFound() throws Exception {
        // given
        Long missionId = 1L;
        Long lockerId = 999L;

        doThrow(new BusinessException(LockerErrorCode.LOCKER_NOT_FOUND))
                .when(adminLockerService)
                .assignLocker(missionId, lockerId);

        // when & then
        mockMvc.perform(post("/admin/missions/{missionId}/lockers/{lockerId}", missionId, lockerId))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(LockerErrorCode.LOCKER_NOT_FOUND.getMessage()))
                .andExpect(jsonPath("$.status").value("NOT_FOUND"));

        verify(adminLockerService, times(1)).assignLocker(missionId, lockerId);
    }
}
