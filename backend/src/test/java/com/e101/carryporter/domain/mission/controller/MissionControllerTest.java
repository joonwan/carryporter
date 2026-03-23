package com.e101.carryporter.domain.mission.controller;

import com.e101.carryporter.domain.mission.controller.dto.request.CreateMissionRequestDto;
import com.e101.carryporter.domain.mission.service.dto.request.CreateMissionServiceRequestDto;
import com.e101.carryporter.domain.user.entity.Role;
import com.e101.carryporter.support.WebMvcTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;


import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MissionControllerTest extends WebMvcTestSupport {

    @Test
    @DisplayName("미션 생성 API 호출 시 200 OK를 반환한다")
    void createMission() throws Exception {
        // given
        CreateMissionRequestDto requestDto = new CreateMissionRequestDto(1L);
        Long userId = 1L;

        given(missionService.createMission(anyLong(), any(CreateMissionServiceRequestDto.class)))
                .willReturn(1L);

        // when & then
        mockMvc.perform(post("/missions")
                        .requestAttr("userId", userId)
                        .requestAttr("role", Role.BASIC)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto))
                        .with(request -> {
                            request.setServletPath("/missions");
                            return request;
                        }))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.missionId").value(1L));
    }

    @Test
    @DisplayName("미션 생성 API 호출 시 callLocationId가 null이면 400 Bad Request를 반환한다")
    void createMission_WithNullCallLocationId_ReturnsBadRequest() throws Exception {
        // given
        CreateMissionRequestDto requestDto = new CreateMissionRequestDto(null);
        Long userId = 1L;

        // when & then
        mockMvc.perform(post("/missions")
                        .requestAttr("userId", userId)
                        .requestAttr("role", Role.BASIC)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto))
                        .with(request -> {
                            request.setServletPath("/missions");
                            return request;
                        }))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.status").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("미션 생성 API 호출 시 callLocationId가 음수이면 400 Bad Request를 반환한다")
    void createMission_WithNegativeCallLocationId_ReturnsBadRequest() throws Exception {
        // given
        CreateMissionRequestDto requestDto = new CreateMissionRequestDto(-1L);
        Long userId = 1L;

        // when & then
        mockMvc.perform(post("/missions")
                        .requestAttr("userId", userId)
                        .requestAttr("role", Role.BASIC)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto))
                        .with(request -> {
                            request.setServletPath("/missions");
                            return request;
                        }))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.status").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.timestamp").exists());
    }
}