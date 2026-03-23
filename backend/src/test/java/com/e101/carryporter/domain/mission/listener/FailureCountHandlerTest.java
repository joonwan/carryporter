package com.e101.carryporter.domain.mission.listener;

import com.e101.carryporter.domain.mission.event.MissionAbortedEvent;
import com.e101.carryporter.domain.user.event.UserAuthFailedEvent;
import com.e101.carryporter.support.IntegrationTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;

import static org.assertj.core.api.Assertions.assertThat;

class FailureCountHandlerIntegrationTest extends IntegrationTestSupport {


}