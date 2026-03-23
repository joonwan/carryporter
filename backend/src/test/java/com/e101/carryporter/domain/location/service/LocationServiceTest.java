package com.e101.carryporter.domain.location.service;

import com.e101.carryporter.domain.location.entity.Location;
import com.e101.carryporter.domain.location.repository.LocationRepository;
import com.e101.carryporter.global.exception.BusinessException;
import com.e101.carryporter.support.IntegrationTestSupport;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocationServiceTest extends IntegrationTestSupport {

    @Autowired
    LocationService locationService;

    @Autowired
    LocationRepository locationRepository;

    @Autowired
    EntityManager em;

    @DisplayName("위치를 pk 기반으로 조회할 수 있다.")
    @Test
    void findById() {
        // given
        Location location = Location.builder()
                .description("test description")
                .locationName("test location name")
                .build();

        locationRepository.save(location);

        flushAndClear();

        // when
        Location findLocation = locationService.findById(location.getId());

        // then
        assertThat(findLocation.getDescription()).isEqualTo("test description");
        assertThat(findLocation.getLocationName()).isEqualTo("test location name");

    }
    @DisplayName("위치가 없을 경우 예외가 발생한다.")
    @Test
    void findByNotExistsUserId() {
        // given
        Long notExistsLocationId = 9999L;

        // when then
        assertThatThrownBy(() -> locationService.findById(notExistsLocationId))
                .isInstanceOf(BusinessException.class)
                .hasMessage("해당 위치를 찾을 수 없습니다.");

    }

    private void flushAndClear() {
        em.flush();
        em.clear();
    }
}