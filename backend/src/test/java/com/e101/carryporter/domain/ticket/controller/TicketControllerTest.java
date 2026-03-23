package com.e101.carryporter.domain.ticket.controller;

import com.e101.carryporter.domain.ticket.controller.dto.response.TicketOcrResponseDto;
import com.e101.carryporter.domain.ticket.controller.dto.response.TicketResponseDto;
import com.e101.carryporter.support.WebMvcTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TicketControllerTest extends WebMvcTestSupport {

    @Test
    @DisplayName("티켓 스캔 성공")
    void scanTicket_success() throws Exception {
        // given
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "ticket.jpg",
                "image/jpeg",
                "fake image content".getBytes()
        );

        TicketOcrResponseDto mockResponse = createMockResponse();
        when(ticketService.scanTicket(any(), eq(1L))).thenReturn(mockResponse);

        // when & then
        mockMvc.perform(multipart("/tickets/scan")
                        .file(file)
                        .requestAttr("userId", 1L))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.flight").value("KE932"))
                .andExpect(jsonPath("$.gate").value("E23"))
                .andExpect(jsonPath("$.seat").value("40B"))
                .andExpect(jsonPath("$.boarding_time").value("21:20"))
                .andExpect(jsonPath("$.departure_time").value("22:00"))
                .andExpect(jsonPath("$.origin").value("ROME"))
                .andExpect(jsonPath("$.destination").value("INCHEON"));
    }

    private TicketOcrResponseDto createMockResponse() {
        try {
            TicketOcrResponseDto dto = new TicketOcrResponseDto();
            setField(dto, "flight", "KE932");
            setField(dto, "gate", "E23");
            setField(dto, "seat", "40B");
            setField(dto, "boardingTime", "21:20");
            setField(dto, "departureTime", "22:00");
            setField(dto, "origin", "ROME");
            setField(dto, "destination", "INCHEON");
            return dto;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("티켓 조회 성공")
    void getTicket_success() throws Exception {
        // given
        Long ticketId = 1L;
        Long userId = 1L;

        TicketResponseDto mockResponse = TicketResponseDto.builder()
                .ticketId(ticketId)
                .flightNo("KE932")
                .gate("E23")
                .boardingTime(LocalDateTime.of(2024, 1, 30, 21, 20))
                .createdAt(LocalDateTime.of(2024, 1, 30, 10, 0))
                .build();

        when(ticketService.getTicket(eq(ticketId), eq(userId))).thenReturn(mockResponse);

        // when & then
        mockMvc.perform(get("/me/tickets/{ticketId}", ticketId)
                        .requestAttr("userId", userId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ticketId").value(ticketId))
                .andExpect(jsonPath("$.flightNo").value("KE932"))
                .andExpect(jsonPath("$.gate").value("E23"));
    }

    private void setField(Object obj, String fieldName, Object value) throws Exception {
        var field = obj.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(obj, value);
    }
}
