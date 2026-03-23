package com.e101.carryporter.domain.sse.controller;

import com.e101.carryporter.domain.user.entity.Role;
import com.e101.carryporter.support.WebMvcTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class SseControllerTest extends WebMvcTestSupport {

    @DisplayName("인증된 사용자가 SSE 구독을 요청하면 성공한다.")
    @Test
    void subscribe() throws Exception {
        // given
        Long userId = 1L;
        String role = "BASIC";

        // sseService.subscribe(userId, role) 호출 시 SseEmitter 반환하도록 설정
        given(sseService.subscribe(anyLong(), anyString()))
                .willReturn(new SseEmitter());

        // when & then
        mockMvc.perform(get("/sse/subscribe")
                        // ★ 필터가 userId와 userRole을 세팅해준 상황을 시뮬레이션합니다.
                        .requestAttr("userId", userId)
                        .requestAttr("role", Role.BASIC)
                        .accept(MediaType.TEXT_EVENT_STREAM))
                .andDo(print())
                .andExpect(status().isOk())
                // 비동기 요청이 시작되었는지 확인
                .andExpect(request().asyncStarted());
    }
}