package com.e101.carryporter.domain.ticket.controller.dto.response;

import com.e101.carryporter.domain.ticket.entity.Ticket;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class TicketResponseDto {

    private Long ticketId;
    private String flightNo;
    private String gate;
    private LocalDateTime boardingTime;
    private LocalDateTime createdAt;

    public static TicketResponseDto from(Ticket ticket) {
        return TicketResponseDto.builder()
                .ticketId(ticket.getId())
                .flightNo(ticket.getFlightNo())
                .gate(ticket.getGate())
                .boardingTime(ticket.getBoardingTime())
                .createdAt(ticket.getCreatedAt())
                .build();
    }
}
