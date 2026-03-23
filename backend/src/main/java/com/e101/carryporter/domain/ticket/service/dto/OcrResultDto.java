package com.e101.carryporter.domain.ticket.service.dto;

import com.e101.carryporter.domain.ticket.controller.dto.response.TicketOcrResponseDto;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class OcrResultDto {

    private boolean success;
    private TicketOcrResponseDto data;
    private String filename;
}
