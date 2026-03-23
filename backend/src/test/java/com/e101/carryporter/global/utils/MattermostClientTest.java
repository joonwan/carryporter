package com.e101.carryporter.global.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import com.e101.carryporter.support.IntegrationTestSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import static org.assertj.core.api.Assertions.assertThat;

class MattermostClientTest extends IntegrationTestSupport {

    @Autowired
    private MattermostClient mattermostClient;

    // [포인트 2] test.yml에 정의된 값을 @Value로 직접 가져옵니다.
    @Value("${mattermost.enabled}")
    private boolean enabled;

    @Value("${mattermost.webhook-url}")
    private String webhookUrl;

    @Test
    @DisplayName("[환경 설정 테스트] test.yml의 매터모스트 설정값 로드 확인")
    void testConfigLoad() {
        // then
        // test.yml에 적힌 대로 값이 잘 들어왔는지 확인
        assertThat(enabled).isTrue(); // 또는 yml 설정값에 따라 확인
        assertThat(webhookUrl).contains("meeting.ssafy.com");
    }

    @Test
    @DisplayName("매터모스트 메시지 발송 로직 테스트")
    void sendMessageTest() {
        String email = "test@ssafy.com";
        String text = "테스트 메시지입니다.";

        // 실제 발송 로직 실행 (enabled가 true면 실제 발송되니 주의!)
        // 팀원들 채널에 알람이 가는게 걱정된다면
        // test.yml에서 enabled를 false로 바꾸고 테스트하세요.
        mattermostClient.sendMessage(email, text);
    }
}