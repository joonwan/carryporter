package com.e101.carryporter.domain.user.controller;

import com.e101.carryporter.domain.locker.entity.UserLockerStatus;
import com.e101.carryporter.domain.locker.service.dto.response.UserLockerServiceResponseDto;
import com.e101.carryporter.domain.locker.service.dto.response.UserLockersServiceResponseDto;
import com.e101.carryporter.support.WebMvcTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserControllerTest extends WebMvcTestSupport {

    @DisplayName("사용자의 사물함 이용 내역을 조회한다")
    @Test
    void getUserLockers() throws Exception {
        // given
        Long userId = 1L;
        LocalDateTime now = LocalDateTime.now();

        List<UserLockerServiceResponseDto> lockerList = List.of(
                new UserLockerServiceResponseDto(1L, UserLockerStatus.OCCUPIED, now),
                new UserLockerServiceResponseDto(2L, UserLockerStatus.COMPLETED, now.minusHours(1))
        );
        UserLockersServiceResponseDto mockResponse = new UserLockersServiceResponseDto(lockerList);

        given(lockerService.getUserLockers(userId)).willReturn(mockResponse);

        // when & then
        mockMvc.perform(get("/me/lockers")
                        .requestAttr("userId", userId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userLockerServiceResponseDtos").isArray())
                .andExpect(jsonPath("$.userLockerServiceResponseDtos.length()").value(2))
                .andExpect(jsonPath("$.userLockerServiceResponseDtos[0].lockerId").value(1))
                .andExpect(jsonPath("$.userLockerServiceResponseDtos[0].userLockerStatus").value("OCCUPIED"))
                .andExpect(jsonPath("$.userLockerServiceResponseDtos[0].updatedAt").exists())
                .andExpect(jsonPath("$.userLockerServiceResponseDtos[1].lockerId").value(2))
                .andExpect(jsonPath("$.userLockerServiceResponseDtos[1].userLockerStatus").value("COMPLETED"))
                .andExpect(jsonPath("$.userLockerServiceResponseDtos[1].updatedAt").exists());

        verify(lockerService).getUserLockers(userId);
    }

    @DisplayName("사용자의 사물함 이용 내역이 없을 때 빈 배열을 반환한다")
    @Test
    void getUserLockersWithEmptyList() throws Exception {
        // given
        Long userId = 1L;
        UserLockersServiceResponseDto mockResponse = new UserLockersServiceResponseDto(List.of());
        given(lockerService.getUserLockers(userId)).willReturn(mockResponse);

        // when & then
        mockMvc.perform(get("/me/lockers")
                        .requestAttr("userId", userId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userLockerServiceResponseDtos").isArray())
                .andExpect(jsonPath("$.userLockerServiceResponseDtos.length()").value(0));

        verify(lockerService).getUserLockers(userId);
    }

    @DisplayName("사용자의 다양한 상태의 사물함 이용 내역을 조회한다")
    @Test
    void getUserLockersWithDifferentStatuses() throws Exception {
        // given
        Long userId = 1L;
        LocalDateTime now = LocalDateTime.now();

        List<UserLockerServiceResponseDto> lockerList = List.of(
                new UserLockerServiceResponseDto(1L, UserLockerStatus.READY, now),
                new UserLockerServiceResponseDto(2L, UserLockerStatus.OCCUPIED, now.minusHours(1)),
                new UserLockerServiceResponseDto(3L, UserLockerStatus.COMPLETED, now.minusHours(2))
        );
        UserLockersServiceResponseDto mockResponse = new UserLockersServiceResponseDto(lockerList);

        given(lockerService.getUserLockers(userId)).willReturn(mockResponse);

        // when & then
        mockMvc.perform(get("/me/lockers")
                        .requestAttr("userId", userId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userLockerServiceResponseDtos").isArray())
                .andExpect(jsonPath("$.userLockerServiceResponseDtos.length()").value(3))
                .andExpect(jsonPath("$.userLockerServiceResponseDtos[0].userLockerStatus").value("READY"))
                .andExpect(jsonPath("$.userLockerServiceResponseDtos[1].userLockerStatus").value("OCCUPIED"))
                .andExpect(jsonPath("$.userLockerServiceResponseDtos[2].userLockerStatus").value("COMPLETED"));

        verify(lockerService).getUserLockers(userId);
    }

    @DisplayName("userId가 RequestAttribute로 전달되어야 한다")
    @Test
    void getUserLockersRequiresUserIdAttribute() throws Exception {
        // given
        Long userId = 123L;
        UserLockersServiceResponseDto mockResponse = new UserLockersServiceResponseDto(List.of());
        given(lockerService.getUserLockers(userId)).willReturn(mockResponse);

        // when & then
        mockMvc.perform(get("/me/lockers")
                        .requestAttr("userId", userId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk());

        verify(lockerService).getUserLockers(userId);
    }

    @DisplayName("여러 사물함 정보를 정확하게 반환한다")
    @Test
    void getUserLockersReturnsMultipleLockers() throws Exception {
        // given
        Long userId = 1L;
        LocalDateTime time1 = LocalDateTime.of(2024, 1, 1, 10, 0);
        LocalDateTime time2 = LocalDateTime.of(2024, 1, 2, 11, 0);
        LocalDateTime time3 = LocalDateTime.of(2024, 1, 3, 12, 0);

        List<UserLockerServiceResponseDto> lockerList = List.of(
                new UserLockerServiceResponseDto(10L, UserLockerStatus.READY, time1),
                new UserLockerServiceResponseDto(20L, UserLockerStatus.OCCUPIED, time2),
                new UserLockerServiceResponseDto(30L, UserLockerStatus.COMPLETED, time3)
        );
        UserLockersServiceResponseDto mockResponse = new UserLockersServiceResponseDto(lockerList);

        given(lockerService.getUserLockers(userId)).willReturn(mockResponse);

        // when & then
        mockMvc.perform(get("/me/lockers")
                        .requestAttr("userId", userId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userLockerServiceResponseDtos[0].lockerId").value(10))
                .andExpect(jsonPath("$.userLockerServiceResponseDtos[0].userLockerStatus").value("READY"))
                .andExpect(jsonPath("$.userLockerServiceResponseDtos[1].lockerId").value(20))
                .andExpect(jsonPath("$.userLockerServiceResponseDtos[1].userLockerStatus").value("OCCUPIED"))
                .andExpect(jsonPath("$.userLockerServiceResponseDtos[2].lockerId").value(30))
                .andExpect(jsonPath("$.userLockerServiceResponseDtos[2].userLockerStatus").value("COMPLETED"));

        verify(lockerService).getUserLockers(userId);
    }
}
