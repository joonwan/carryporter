package com.e101.carryporter.global.config.mqtt;

import com.e101.carryporter.global.service.mqtt.MqttSubscriberService;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;
import org.springframework.integration.mqtt.outbound.MqttPahoMessageHandler;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.UUID;

@Slf4j
@Configuration
public class MqttConfig {

    @Value("${mqtt.broker.url}")
    private String brokerUrl;

    @Value("${mqtt.client.id}")
    private String clientId;

    @Value("${mqtt.broker.username:}")
    private String brokerUsername;

    @Value("${mqtt.broker.password:}")
    private String brokerPassword;

    @Autowired
    private Environment environment;

    // 서버가 구독할 토픽 패턴들 (Upstream: 로봇 → 서버)
    private static final String[] SUBSCRIBE_TOPICS = {
            "robot/+/register",   // 기기 등록
            "robot/+/arrived",    // 사용자 위치 도착
            "robot/+/locked",     // 잠금 완료
            "robot/+/unlocked",   // 잠금 해제 완료
            "robot/+/returned",   // 스테이션 복귀 완료
            "robot/+/IDLE",       // 스테이션 복귀 완료 (IDLE 상태)
            "robot/+/error"       // 에러 발생
    };

    @Bean
    public MqttConnectOptions mqttConnectOptions() {
        MqttConnectOptions options = new MqttConnectOptions();
        options.setServerURIs(new String[]{brokerUrl});
        options.setCleanSession(true);
        options.setAutomaticReconnect(true);
        options.setConnectionTimeout(10);
        options.setKeepAliveInterval(60);

        if (StringUtils.hasText(brokerUsername)) {
            options.setUserName(brokerUsername);
        }
        if (StringUtils.hasText(brokerPassword)) {
            options.setPassword(brokerPassword.toCharArray());
        }

        return options;
    }

    @Bean
    public MqttPahoClientFactory mqttClientFactory(MqttConnectOptions mqttConnectOptions) {
        DefaultMqttPahoClientFactory factory = new DefaultMqttPahoClientFactory();
        factory.setConnectionOptions(mqttConnectOptions);
        return factory;
    }

    // ==================== Outbound (메시지 발행: 서버 → 로봇) ====================

    @Bean
    public MqttPahoMessageHandler mqttOutbound(MqttPahoClientFactory mqttClientFactory) {
        MqttPahoMessageHandler handler = new MqttPahoMessageHandler(
                clientId + "-publisher", mqttClientFactory);
        handler.setAsync(true);
        handler.setDefaultTopic("default");
        return handler;
    }

    // ==================== Inbound (메시지 구독: 로봇 → 서버) - 직접 Paho 사용 ====================

    @Bean
    public MqttClient mqttSubscriberClient(MqttConnectOptions mqttConnectOptions,
                                           MqttSubscriberService mqttSubscriberService) throws MqttException {
        log.info("MQTT 브로커 연결: {}", brokerUrl);
        log.info("MQTT 구독 토픽 목록:");
        for (String topic : SUBSCRIBE_TOPICS) {
            log.info("  - {}", topic);
        }

        String subscriberClientId = clientId + "-subscriber";
        // 테스트 환경에서는 Client ID가 중복되지 않도록 UUID 추가
        if (Arrays.asList(environment.getActiveProfiles()).contains("test")) {
            subscriberClientId += "-" + UUID.randomUUID();
        }

        MqttClient client = new MqttClient(brokerUrl, subscriberClientId, new MemoryPersistence());

        client.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {
                log.error("MQTT 연결 끊김: {}", cause.getMessage());
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) {
                String payload = new String(message.getPayload());
                // Paho 콜백 스레드는 Spring의 트랜잭션 관리 밖에 있으므로,
                // 서비스 레이어에서 TransactionTemplate 등을 사용하여 트랜잭션을 수동으로 관리해야 함.
                mqttSubscriberService.handleMqttMessage(topic, payload);
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                // 발행 완료 시 호출 (구독자에서는 사용 안 함)
            }
        });

        client.connect(mqttConnectOptions);
        log.info("MQTT 구독자 연결 성공 - Client ID: {}", subscriberClientId);

        // 토픽 구독
        for (String topic : SUBSCRIBE_TOPICS) {
            client.subscribe(topic, 1);
            log.info("MQTT 토픽 구독: {}", topic);
        }

        return client;
    }
}
