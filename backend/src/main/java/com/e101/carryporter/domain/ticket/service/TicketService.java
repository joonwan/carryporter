package com.e101.carryporter.domain.ticket.service;

import com.e101.carryporter.domain.ticket.controller.dto.response.TicketOcrResponseDto;
import com.e101.carryporter.domain.ticket.controller.dto.response.TicketResponseDto;
import com.e101.carryporter.domain.ticket.repository.TicketRepository;
import com.e101.carryporter.domain.ticket.service.dto.OcrResultDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TicketService {

    private final OcrClient ocrClient;
    private final TicketRepository ticketRepository;

    public TicketOcrResponseDto scanTicket(MultipartFile file, Long userId) {
        log.info("Processing ticket scan for userId: {}", userId);

        OcrResultDto ocrResult = ocrClient.sendToOcr(file);
        return ocrResult.getData();
    }

    public TicketResponseDto getTicket(Long ticketId, Long userId) {
        return ticketRepository.findById(ticketId)
                .filter(ticket -> ticket.getUser().getId().equals(userId))
                .map(TicketResponseDto::from)
                .orElseThrow(() -> new IllegalArgumentException("티켓을 찾을 수 없습니다."));
    }
}
