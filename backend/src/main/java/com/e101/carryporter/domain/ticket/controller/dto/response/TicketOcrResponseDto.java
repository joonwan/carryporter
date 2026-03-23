package com.e101.carryporter.domain.ticket.controller.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class TicketOcrResponseDto {

    private String flight;
    private String gate;
    private String seat;

    @JsonProperty("boarding_time")
    private String boardingTime;

    @JsonProperty("departure_time")
    private String departureTime;

    private String origin;
    private String destination;
}
