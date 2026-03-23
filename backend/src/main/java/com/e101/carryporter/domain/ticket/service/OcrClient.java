package com.e101.carryporter.domain.ticket.service;

import com.e101.carryporter.domain.ticket.service.dto.OcrResultDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Component
public class OcrClient {

    private final WebClient webClient;

    public OcrClient(@Value("${ocr.server.url:http://localhost:8020}") String ocrServerUrl) {
        this.webClient = WebClient.builder()
                .baseUrl(ocrServerUrl)
                .build();
    }

    public OcrResultDto sendToOcr(MultipartFile file) {
        log.info("Sending image to OCR server - filename: {}, size: {} bytes",
                file.getOriginalFilename(), file.getSize());

        try {
            MultipartBodyBuilder builder = new MultipartBodyBuilder();
            builder.part("file", new ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return file.getOriginalFilename();
                }
            }).contentType(MediaType.parseMediaType(file.getContentType()));

            OcrResultDto result = webClient.post()
                    .uri("/ocr")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(BodyInserters.fromMultipartData(builder.build()))
                    .retrieve()
                    .bodyToMono(OcrResultDto.class)
                    .block();

            log.info("OCR response received - success: {}", result != null && result.isSuccess());
            return result;

        } catch (Exception e) {
            log.error("OCR request failed", e);
            throw new RuntimeException("OCR 서버 호출 실패: " + e.getMessage(), e);
        }
    }
}
