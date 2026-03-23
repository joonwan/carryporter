package com.e101.carryporter.domain.admin.controller;

import com.e101.carryporter.domain.admin.controller.dto.request.DispatchRequestDto;
import com.e101.carryporter.domain.admin.controller.dto.request.FinalizeRequestDto;
import com.e101.carryporter.domain.admin.controller.dto.request.JoinRequestDto;
import com.e101.carryporter.domain.admin.controller.dto.request.LoginRequestDto;
import com.e101.carryporter.domain.admin.controller.dto.request.UnlockRobotRequestDto;
import com.e101.carryporter.domain.admin.controller.dto.response.LockerResponseDto;
import com.e101.carryporter.domain.admin.controller.dto.response.MissionResponseDto;
import com.e101.carryporter.domain.admin.controller.dto.response.RobotResponseDto;
import com.e101.carryporter.domain.auth.controller.dto.response.TokenResponseDto;
import com.e101.carryporter.domain.locker.entity.LockerStatus;
import com.e101.carryporter.domain.locker.exception.LockerErrorCode;
import com.e101.carryporter.domain.mission.entity.MissionStatus;
import com.e101.carryporter.domain.mission.exception.MissionErrorCode;
import com.e101.carryporter.domain.robot.entity.RobotStatus;
import com.e101.carryporter.domain.robot.exception.RobotErrorCode;
import com.e101.carryporter.domain.user.exception.UserErrorCode;
import com.e101.carryporter.global.exception.BusinessException;
import com.e101.carryporter.support.WebMvcTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;

class AdminControllerTest extends WebMvcTestSupport {

    @Test
    @DisplayName("관리자 계정 생성 요청 시 정상적으로 처리되고 204를 반환한다")
    void join() throws Exception {
        // given
        JoinRequestDto requestDto = new JoinRequestDto(
                "admin@mattermost.com",
                "관리자",
                "password123!"
        );

        given(adminService.join(anyString(), anyString(), anyString()))
                .willReturn(1L);

        // when & then
        mockMvc.perform(post("/admin/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andDo(print())
                .andExpect(status().isNoContent());

        verify(adminService, times(1)).join(
                requestDto.getMmEmail(),
                requestDto.getName(),
                requestDto.getPassword()
        );
    }

    @Test
    @DisplayName("관리자 계정 생성 시 mmEmail이 null이면 400 Bad Request를 반환한다")
    void joinWithNullMmEmail() throws Exception {
        // given
        JoinRequestDto requestDto = new JoinRequestDto(
                null,
                "관리자",
                "password123!"
        );

        // when & then
        mockMvc.perform(post("/admin/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(adminService, never()).join(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("관리자 계정 생성 시 mmEmail이 빈 문자열이면 400 Bad Request를 반환한다")
    void joinWithBlankMmEmail() throws Exception {
        // given
        JoinRequestDto requestDto = new JoinRequestDto(
                "   ",
                "관리자",
                "password123!"
        );

        // when & then
        mockMvc.perform(post("/admin/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(adminService, never()).join(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("관리자 계정 생성 시 name이 null이면 400 Bad Request를 반환한다")
    void joinWithNullName() throws Exception {
        // given
        JoinRequestDto requestDto = new JoinRequestDto(
                "admin@mattermost.com",
                null,
                "password123!"
        );

        // when & then
        mockMvc.perform(post("/admin/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(adminService, never()).join(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("관리자 계정 생성 시 name이 빈 문자열이면 400 Bad Request를 반환한다")
    void joinWithBlankName() throws Exception {
        // given
        JoinRequestDto requestDto = new JoinRequestDto(
                "admin@mattermost.com",
                "   ",
                "password123!"
        );

        // when & then
        mockMvc.perform(post("/admin/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(adminService, never()).join(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("관리자 계정 생성 시 password가 null이면 400 Bad Request를 반환한다")
    void joinWithNullPassword() throws Exception {
        // given
        JoinRequestDto requestDto = new JoinRequestDto(
                "admin@mattermost.com",
                "관리자",
                null
        );

        // when & then
        mockMvc.perform(post("/admin/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(adminService, never()).join(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("관리자 계정 생성 시 password가 빈 문자열이면 400 Bad Request를 반환한다")
    void joinWithBlankPassword() throws Exception {
        // given
        JoinRequestDto requestDto = new JoinRequestDto(
                "admin@mattermost.com",
                "관리자",
                "   "
        );

        // when & then
        mockMvc.perform(post("/admin/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(adminService, never()).join(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("관리자 계정 생성 시 모든 필드가 null이면 400 Bad Request를 반환한다")
    void joinWithAllNullFields() throws Exception {
        // given
        JoinRequestDto requestDto = new JoinRequestDto(
                null,
                null,
                null
        );

        // when & then
        mockMvc.perform(post("/admin/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(adminService, never()).join(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("중복된 이메일로 관리자 계정 생성 시 409 Conflict를 반환한다")
    void joinWithDuplicatedEmail() throws Exception {
        // given
        JoinRequestDto requestDto = new JoinRequestDto(
                "admin@mattermost.com",
                "관리자",
                "password123!"
        );

        given(adminService.join(anyString(), anyString(), anyString()))
                .willThrow(new BusinessException(UserErrorCode.DUPLICATED_USER_EMAIL));

        // when & then
        mockMvc.perform(post("/admin/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andDo(print())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(UserErrorCode.DUPLICATED_USER_EMAIL.getMessage()))
                .andExpect(jsonPath("$.status").value("CONFLICT"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(adminService, times(1)).join(
                requestDto.getMmEmail(),
                requestDto.getName(),
                requestDto.getPassword()
        );
    }

    @Test
    @DisplayName("중복된 이름으로 관리자 계정 생성 시 409 Conflict를 반환한다")
    void joinWithDuplicatedName() throws Exception {
        // given
        JoinRequestDto requestDto = new JoinRequestDto(
                "admin@mattermost.com",
                "관리자",
                "password123!"
        );

        given(adminService.join(anyString(), anyString(), anyString()))
                .willThrow(new BusinessException(UserErrorCode.DUPLICATED_ADMIN_NAME));

        // when & then
        mockMvc.perform(post("/admin/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andDo(print())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(UserErrorCode.DUPLICATED_ADMIN_NAME.getMessage()))
                .andExpect(jsonPath("$.status").value("CONFLICT"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(adminService, times(1)).join(
                requestDto.getMmEmail(),
                requestDto.getName(),
                requestDto.getPassword()
        );
    }

    @Test
    @DisplayName("관리자 로그인 시 정상적으로 처리되고 토큰과 쿠키를 반환한다")
    void login() throws Exception {
        // given
        LoginRequestDto requestDto = new LoginRequestDto(
                "admin@mattermost.com",
                "password123!"
        );

        TokenResponseDto tokenResponse = TokenResponseDto.builder()
                .accessToken("access-token-value")
                .refreshToken("refresh-token-value")
                .grantType("Bearer")
                .expiresIn(3600L)
                .build();

        given(adminService.login(anyString(), anyString()))
                .willReturn(tokenResponse);

        // when & then
        mockMvc.perform(post("/admin/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token-value"))
                .andExpect(jsonPath("$.refreshToken").doesNotExist()) // 보안상 body에는 없어야 함
                .andExpect(jsonPath("$.grantType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(3600))
                .andExpect(header().exists(HttpHeaders.SET_COOKIE))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, startsWith("refreshToken=")));

        verify(adminService, times(1)).login(
                requestDto.getMmEmail(),
                requestDto.getPassword()
        );
    }

    @Test
    @DisplayName("관리자 로그인 시 mmEmail이 null이면 400 Bad Request를 반환한다")
    void loginWithNullMmEmail() throws Exception {
        // given
        LoginRequestDto requestDto = new LoginRequestDto(
                null,
                "password123!"
        );

        // when & then
        mockMvc.perform(post("/admin/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(adminService, never()).login(anyString(), anyString());
    }

    @Test
    @DisplayName("관리자 로그인 시 mmEmail이 빈 문자열이면 400 Bad Request를 반환한다")
    void loginWithBlankMmEmail() throws Exception {
        // given
        LoginRequestDto requestDto = new LoginRequestDto(
                "   ",
                "password123!"
        );

        // when & then
        mockMvc.perform(post("/admin/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(adminService, never()).login(anyString(), anyString());
    }

    @Test
    @DisplayName("관리자 로그인 시 password가 null이면 400 Bad Request를 반환한다")
    void loginWithNullPassword() throws Exception {
        // given
        LoginRequestDto requestDto = new LoginRequestDto(
                "admin@mattermost.com",
                null
        );

        // when & then
        mockMvc.perform(post("/admin/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(adminService, never()).login(anyString(), anyString());
    }

    @Test
    @DisplayName("관리자 로그인 시 password가 빈 문자열이면 400 Bad Request를 반환한다")
    void loginWithBlankPassword() throws Exception {
        // given
        LoginRequestDto requestDto = new LoginRequestDto(
                "admin@mattermost.com",
                "   "
        );

        // when & then
        mockMvc.perform(post("/admin/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(adminService, never()).login(anyString(), anyString());
    }

    @Test
    @DisplayName("관리자 로그인 시 모든 필드가 null이면 400 Bad Request를 반환한다")
    void loginWithAllNullFields() throws Exception {
        // given
        LoginRequestDto requestDto = new LoginRequestDto(
                null,
                null
        );

        // when & then
        mockMvc.perform(post("/admin/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(adminService, never()).login(anyString(), anyString());
    }

    @Test
    @DisplayName("관리자 로그인 시 잘못된 비밀번호를 입력하면 401 Unauthorized를 반환한다")
    void loginWithWrongPassword() throws Exception {
        // given
        LoginRequestDto requestDto = new LoginRequestDto(
                "admin@mattermost.com",
                "wrongpassword"
        );

        given(adminService.login(anyString(), anyString()))
                .willThrow(new BusinessException(UserErrorCode.UNAUTHORIZED));

        // when & then
        mockMvc.perform(post("/admin/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(UserErrorCode.UNAUTHORIZED.getMessage()))
                .andExpect(jsonPath("$.status").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(adminService, times(1)).login(
                requestDto.getMmEmail(),
                requestDto.getPassword()
        );
    }

    @Test
    @DisplayName("관리자 로그인 시 존재하지 않는 이메일을 입력하면 404 Not Found를 반환한다")
    void loginWithNonExistentEmail() throws Exception {
        // given
        LoginRequestDto requestDto = new LoginRequestDto(
                "nonexistent@mattermost.com",
                "password123!"
        );

        given(adminService.login(anyString(), anyString()))
                .willThrow(new BusinessException(UserErrorCode.USER_NOT_FOUND));

        // when & then
        mockMvc.perform(post("/admin/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(UserErrorCode.USER_NOT_FOUND.getMessage()))
                .andExpect(jsonPath("$.status").value("NOT_FOUND"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(adminService, times(1)).login(
                requestDto.getMmEmail(),
                requestDto.getPassword()
        );
    }


    @Test
    @DisplayName("로봇 잠금 해제 API 호출 시 204 No Content를 반환한다")
    void unlockRobot() throws Exception {
        // given
        Long missionId = 1L;
        UnlockRobotRequestDto requestDto = createUnlockRobotRequestDto(1L);

        willDoNothing()
                .given(robotService)
                .unlockByAdmin(anyLong());

        // when & then
        mockMvc.perform(post("/admin/missions/{missionId}/unlock", missionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto))
                        .with(request -> {
                            request.setServletPath("/admin/missions/" + missionId + "/unlock");
                            return request;
                        }))
                .andDo(print())
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("로봇 잠금 API 호출 시 204 No Content를 반환한다")
    void lockRobot() throws Exception {
        // given
        Long missionId = 1L;
        UnlockRobotRequestDto requestDto = createUnlockRobotRequestDto(1L);

        willDoNothing()
                .given(robotService)
                .lockByAdmin(anyLong());

        // when & then
        mockMvc.perform(post("/admin/missions/{missionId}/lock", missionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto))
                        .with(request -> {
                            request.setServletPath("/admin/missions/" + missionId + "/lock");
                            return request;
                        }))
                .andDo(print())
                .andExpect(status().isNoContent());
    }




    @Test
    @DisplayName("로봇 이동 API 호출 시 204 No Content를 반환한다")
    void dispatch() throws Exception {
        // given
        Long missionId = 1L;
        // DTO 생성 로직 삭제

        willDoNothing()
                .given(robotService)
                .dispatch(missionId); // any() 대신 명확한 인자 전달 검증

        // when & then
        mockMvc.perform(post("/admin/missions/{missionId}/dispatch", missionId)
                        // Request Body가 없으므로 contentType, content 삭제
                        .with(request -> {
                            request.setServletPath("/admin/missions/" + missionId + "/dispatch");
                            return request;
                        }))
                .andDo(print())
                .andExpect(status().isNoContent());

        // verify: 서비스가 올바른 missionId로 호출되었는지 검증
        verify(robotService).dispatch(missionId);
    }

    @Test
    @DisplayName("미션 최종 완료 API 호출 시 204 No Content를 반환한다")
    void finalizeMission() throws Exception {
        // given
        Long missionId = 1L;
        FinalizeRequestDto requestDto = createFinalizeRequestDto(1L);

        willDoNothing()
                .given(robotService)
                .finalizeMission(anyLong());

        // when & then
        mockMvc.perform(post("/admin/missions/{missionId}/finalize", missionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto))
                        .with(request -> {
                            request.setServletPath("/admin/missions/" + missionId + "/finalize");
                            return request;
                        }))
                .andDo(print())
                .andExpect(status().isNoContent());
    }


    // ==================== 유저 수 조회 테스트 ====================

    @Test
    @DisplayName("전체 유저 수 조회 시 200 OK와 count를 반환한다")
    void getUserCount() throws Exception {
        // given
        given(adminService.getUserCount()).willReturn(100L);

        // when & then
        mockMvc.perform(get("/admin/users/count"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(100));

        verify(adminService, times(1)).getUserCount();
    }

    // ==================== 로봇 조회 테스트 ====================

    @Test
    @DisplayName("전체 로봇 조회 시 200 OK와 로봇 목록을 반환한다")
    void getAllRobots() throws Exception {
        // given
        List<RobotResponseDto> robots = List.of(
                RobotResponseDto.builder()
                        .id(1L)
                        .robotCode("ROBOT-001")
                        .macAddress("AA:BB:CC:DD:EE:01")
                        .robotStatus(RobotStatus.IDLE)
                        .build(),
                RobotResponseDto.builder()
                        .id(2L)
                        .robotCode("ROBOT-002")
                        .macAddress("AA:BB:CC:DD:EE:02")
                        .robotStatus(RobotStatus.BUSY)
                        .build()
        );

        given(adminService.getAllRobots()).willReturn(robots);

        // when & then
        mockMvc.perform(get("/admin/robots"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].robotCode").value("ROBOT-001"))
                .andExpect(jsonPath("$[0].robotStatus").value("IDLE"))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].robotCode").value("ROBOT-002"))
                .andExpect(jsonPath("$[1].robotStatus").value("BUSY"));

        verify(adminService, times(1)).getAllRobots();
    }

    @Test
    @DisplayName("로봇 단건 조회 시 200 OK와 로봇 정보를 반환한다")
    void getRobot() throws Exception {
        // given
        Long robotId = 1L;
        RobotResponseDto robot = RobotResponseDto.builder()
                .id(robotId)
                .robotCode("ROBOT-001")
                .macAddress("AA:BB:CC:DD:EE:01")
                .robotStatus(RobotStatus.IDLE)
                .build();

        given(adminService.getRobot(robotId)).willReturn(robot);

        // when & then
        mockMvc.perform(get("/admin/robots/{robotId}", robotId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.robotCode").value("ROBOT-001"))
                .andExpect(jsonPath("$.macAddress").value("AA:BB:CC:DD:EE:01"))
                .andExpect(jsonPath("$.robotStatus").value("IDLE"));

        verify(adminService, times(1)).getRobot(robotId);
    }

    @Test
    @DisplayName("존재하지 않는 로봇 조회 시 404 Not Found를 반환한다")
    void getRobot_NotFound() throws Exception {
        // given
        Long robotId = 999L;

        given(adminService.getRobot(robotId))
                .willThrow(new BusinessException(RobotErrorCode.ROBOT_NOT_FOUND));

        // when & then
        mockMvc.perform(get("/admin/robots/{robotId}", robotId))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(RobotErrorCode.ROBOT_NOT_FOUND.getMessage()))
                .andExpect(jsonPath("$.status").value("NOT_FOUND"));

        verify(adminService, times(1)).getRobot(robotId);
    }

    @Test
    @DisplayName("가용 로봇 수 조회 시 200 OK와 count를 반환한다")
    void getAvailableRobotCount() throws Exception {
        // given
        given(adminService.getAvailableRobotCount()).willReturn(5L);

        // when & then
        mockMvc.perform(get("/admin/robots/available/count"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(5));

        verify(adminService, times(1)).getAvailableRobotCount();
    }

    // ==================== 미션 조회 테스트 ====================

    @Test
    @DisplayName("전체 미션 조회 시 200 OK와 미션 목록을 반환한다 (최대 15개)")
    void getAllMissions() throws Exception {
        // given
        LocalDateTime now = LocalDateTime.now();
        List<MissionResponseDto> missions = List.of(
                MissionResponseDto.builder()
                        .id(1L)
                        .userId(10L)
                        .robotId(1L)
                        .robotCode("ROBOT-001")
                        .lockerId(1L)
                        .lockerCode("LOCKER-001")
                        .callLocationName("1층 로비")
                        .missionStatus(MissionStatus.MOVING)
                        .createdAt(now)
                        .build(),
                MissionResponseDto.builder()
                        .id(2L)
                        .userId(20L)
                        .robotId(2L)
                        .robotCode("ROBOT-002")
                        .lockerId(null)
                        .lockerCode(null)
                        .callLocationName("2층 회의실")
                        .missionStatus(MissionStatus.REQUESTED)
                        .createdAt(now.minusHours(1))
                        .build()
        );

        given(adminService.getAllMissions(15)).willReturn(missions);

        // when & then
        mockMvc.perform(get("/admin/missions"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].userId").value(10))
                .andExpect(jsonPath("$[0].robotCode").value("ROBOT-001"))
                .andExpect(jsonPath("$[0].missionStatus").value("MOVING"))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].missionStatus").value("REQUESTED"));

        verify(adminService, times(1)).getAllMissions(15);
    }

    @Test
    @DisplayName("미션 단건 조회 시 200 OK와 미션 정보를 반환한다")
    void getMission() throws Exception {
        // given
        Long missionId = 1L;
        LocalDateTime now = LocalDateTime.now();
        MissionResponseDto mission = MissionResponseDto.builder()
                .id(missionId)
                .userId(10L)
                .robotId(1L)
                .robotCode("ROBOT-001")
                .lockerId(1L)
                .lockerCode("LOCKER-001")
                .callLocationName("1층 로비")
                .missionStatus(MissionStatus.ARRIVED)
                .robotAssignedAt(now.minusMinutes(30))
                .robotAssignedAt(now.minusMinutes(40))
                .startedAt(now.minusMinutes(25))
                .arrivedAt(now.minusMinutes(5))
                .createdAt(now.minusMinutes(35))
                .build();

        given(adminService.getMission(missionId)).willReturn(mission);

        // when & then
        mockMvc.perform(get("/admin/missions/{missionId}", missionId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.userId").value(10))
                .andExpect(jsonPath("$.robotId").value(1))
                .andExpect(jsonPath("$.robotCode").value("ROBOT-001"))
                .andExpect(jsonPath("$.lockerId").value(1))
                .andExpect(jsonPath("$.lockerCode").value("LOCKER-001"))
                .andExpect(jsonPath("$.callLocationName").value("1층 로비"))
                .andExpect(jsonPath("$.missionStatus").value("ARRIVED"));

        verify(adminService, times(1)).getMission(missionId);
    }

    @Test
    @DisplayName("존재하지 않는 미션 조회 시 404 Not Found를 반환한다")
    void getMission_NotFound() throws Exception {
        // given
        Long missionId = 999L;

        given(adminService.getMission(missionId))
                .willThrow(new BusinessException(MissionErrorCode.MISSION_NOT_FOUND));

        // when & then
        mockMvc.perform(get("/admin/missions/{missionId}", missionId))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(MissionErrorCode.MISSION_NOT_FOUND.getMessage()))
                .andExpect(jsonPath("$.status").value("NOT_FOUND"));

        verify(adminService, times(1)).getMission(missionId);
    }

    // ==================== 사물함 조회 테스트 ====================

    @Test
    @DisplayName("전체 사물함 조회 시 200 OK와 사물함 목록을 반환한다")
    void getAllLockers() throws Exception {
        // given
        List<LockerResponseDto> lockers = List.of(
                LockerResponseDto.builder()
                        .lockerId(1L)
                        .lockerCode("LOCKER-001")
                        .status(LockerStatus.AVAILABLE)
                        .build(),
                LockerResponseDto.builder()
                        .lockerId(2L)
                        .lockerCode("LOCKER-002")
                        .status(LockerStatus.OCCUPIED)
                        .build()
        );

        given(adminLockerService.getAllLockers()).willReturn(lockers);

        // when & then
        mockMvc.perform(get("/admin/lockers"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].lockerId").value(1))
                .andExpect(jsonPath("$[0].lockerCode").value("LOCKER-001"))
                .andExpect(jsonPath("$[0].status").value("AVAILABLE"))
                .andExpect(jsonPath("$[1].lockerId").value(2))
                .andExpect(jsonPath("$[1].lockerCode").value("LOCKER-002"))
                .andExpect(jsonPath("$[1].status").value("OCCUPIED"));

        verify(adminLockerService, times(1)).getAllLockers();
    }

    @Test
    @DisplayName("사물함 단건 조회 시 200 OK와 사물함 정보를 반환한다")
    void getLocker() throws Exception {
        // given
        Long lockerId = 1L;
        LockerResponseDto locker = LockerResponseDto.builder()
                .lockerId(lockerId)
                .lockerCode("LOCKER-001")
                .status(LockerStatus.AVAILABLE)
                .build();

        given(adminLockerService.getLocker(lockerId)).willReturn(locker);

        // when & then
        mockMvc.perform(get("/admin/lockers/{lockerId}", lockerId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lockerId").value(1))
                .andExpect(jsonPath("$.lockerCode").value("LOCKER-001"))
                .andExpect(jsonPath("$.status").value("AVAILABLE"));

        verify(adminLockerService, times(1)).getLocker(lockerId);
    }

    @Test
    @DisplayName("존재하지 않는 사물함 조회 시 404 Not Found를 반환한다")
    void getLocker_NotFound() throws Exception {
        // given
        Long lockerId = 999L;

        given(adminLockerService.getLocker(lockerId))
                .willThrow(new BusinessException(LockerErrorCode.LOCKER_NOT_FOUND));

        // when & then
        mockMvc.perform(get("/admin/lockers/{lockerId}", lockerId))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(LockerErrorCode.LOCKER_NOT_FOUND.getMessage()))
                .andExpect(jsonPath("$.status").value("NOT_FOUND"));

        verify(adminLockerService, times(1)).getLocker(lockerId);
    }

    @Test
    @DisplayName("사물함 상태 변경 시 200 OK와 변경된 사물함 정보를 반환한다")
    void updateLockerStatus() throws Exception {
        // given
        Long lockerId = 1L;
        LockerResponseDto updatedLocker = LockerResponseDto.builder()
                .lockerId(lockerId)
                .lockerCode("LOCKER-001")
                .status(LockerStatus.OCCUPIED)
                .build();

        given(adminLockerService.updateLockerStatus(eq(lockerId), eq(LockerStatus.OCCUPIED)))
                .willReturn(updatedLocker);

        // when & then
        mockMvc.perform(patch("/admin/lockers/{lockerId}/status", lockerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"OCCUPIED\"}"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lockerId").value(1))
                .andExpect(jsonPath("$.lockerCode").value("LOCKER-001"))
                .andExpect(jsonPath("$.status").value("OCCUPIED"));

        verify(adminLockerService, times(1)).updateLockerStatus(lockerId, LockerStatus.OCCUPIED);
    }

    @Test
    @DisplayName("사물함 상태 변경 시 status가 null이면 400 Bad Request를 반환한다")
    void updateLockerStatus_NullStatus() throws Exception {
        // given
        Long lockerId = 1L;

        // when & then
        mockMvc.perform(patch("/admin/lockers/{lockerId}/status", lockerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": null}"))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(adminLockerService, never()).updateLockerStatus(anyLong(), any());
    }

    @Test
    @DisplayName("존재하지 않는 사물함 상태 변경 시 404 Not Found를 반환한다")
    void updateLockerStatus_NotFound() throws Exception {
        // given
        Long lockerId = 999L;

        given(adminLockerService.updateLockerStatus(eq(lockerId), eq(LockerStatus.OCCUPIED)))
                .willThrow(new BusinessException(LockerErrorCode.LOCKER_NOT_FOUND));

        // when & then
        mockMvc.perform(patch("/admin/lockers/{lockerId}/status", lockerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"OCCUPIED\"}"))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(LockerErrorCode.LOCKER_NOT_FOUND.getMessage()))
                .andExpect(jsonPath("$.status").value("NOT_FOUND"));

        verify(adminLockerService, times(1)).updateLockerStatus(lockerId, LockerStatus.OCCUPIED);
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

    // ==================== Helper Methods ====================

    private UnlockRobotRequestDto createUnlockRobotRequestDto(Long robotId) {
        return new UnlockRobotRequestDto(robotId);
    }

    private DispatchRequestDto createMoveRequestDto(Long robotId, Long callLocationId) {
        return new DispatchRequestDto(robotId, callLocationId);
    }

    private FinalizeRequestDto createFinalizeRequestDto(Long robotId) {
        return new FinalizeRequestDto(robotId);
    }
}
