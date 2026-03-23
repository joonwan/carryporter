package com.e101.carryporter.global.utils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class MattermostClient {

    @Value("${mattermost.webhook-url}") // application.properties에 설정 필요
    private String webhookUrl;

    public void sendMessage(String mmEmail, String text) {
        // WebClient 생성
        WebClient client = WebClient.create();

        // 보낼 메시지 구성 (JSON)
        Map<String, Object> payload = new HashMap<>();
        // 멘션(@)을 걸어서 알림이 가게 함
        payload.put("text", "@" + mmEmail.split("@")[0] + " " + text);
        // 참고: Mattermost 설정에 따라 username으로 멘션이 안 될 수도 있습니다.
        // 그럴 땐 채널 전체 알림이나 다이렉트 메시지 API를 써야 할 수도 있습니다.
        // 일단은 심플하게 텍스트 전송으로 구현합니다.

        try {
            client.post()
                    .uri(webhookUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(); // 동기식 처리 (메시지 갈 때까지 대기)
            log.info("Mattermost Message Sent to {}", mmEmail);
        } catch (Exception e) {
            log.error("Failed to send Mattermost message", e);
            throw new RuntimeException("메시지 발송 실패");
        }
    }
}