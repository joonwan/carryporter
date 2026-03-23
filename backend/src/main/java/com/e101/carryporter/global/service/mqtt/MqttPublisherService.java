package com.e101.carryporter.global.service.mqtt;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.integration.mqtt.outbound.MqttPahoMessageHandler;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class MqttPublisherService {

    private final MqttPahoMessageHandler mqttOutbound;

    /**
     * MQTT 메시지 발행 (기본)
     */
    public void publish(String topic, String payload) {
        Message<String> message = MessageBuilder.withPayload(payload)
                .setHeader(MqttHeaders.TOPIC, topic)
                .build();
        mqttOutbound.handleMessage(message);
        log.info("MQTT 메시지 발행 - topic: {}, payload: {}", topic, payload);
    }

    /**
     * 로봇에게 명령 전송 (공통)
     * 사용법: sendCommand("AA:BB:CC...", "deliver", "{\"destX\":10, \"destY\":20}");
     */
    public void sendCommand(String mac, String action, String jsonPayload) {
        String topic = String.format("robot/%s/command/%s", mac, action);
        publish(topic, jsonPayload);
    }

    /**
     * 디스패치 명령 전송 (관리자가 로봇에게 이동 명령)
     *
     * @param mac 로봇 MAC 주소
     */
    public void sendDispatchCommand(String mac, String destination) {
        String payload = String.format("{\"destination\":\"%s\"}", destination);
        sendCommand(mac, "dispatch", payload);
        log.info("디스패치 명령 전송 - MAC: {}, 목적지: {}", mac, destination);
    }

    /**
     * 복귀 명령 전송
     *
     * @param mac 로봇 MAC 주소
     */
    public void sendReturnCommand(String mac) {
        sendCommand(mac, "return", "{}");
        log.info("복귀 명령 전송 - MAC: {}", mac);
    }

    /**
     * 긴급 정지 명령 전송
     *
     * @param mac 로봇 MAC 주소
     */
    public void sendStopCommand(String mac) {
        sendCommand(mac, "stop", "{}");
        log.info("긴급 정지 명령 전송 - MAC: {}", mac);
    }
}
