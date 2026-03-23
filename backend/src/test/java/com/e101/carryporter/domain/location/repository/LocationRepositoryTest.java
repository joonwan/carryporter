package com.e101.carryporter.domain.location.repository;

import com.e101.carryporter.domain.location.entity.Location;
import com.e101.carryporter.support.IntegrationTestSupport;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class LocationRepositoryTest extends IntegrationTestSupport {

    @Autowired
    LocationRepository locationRepository;

    @Autowired
    EntityManager em;

    @DisplayName("위치를 저장할 수 있다.")
    @Test
    void saveLocation() {
        // given
        Location location = Location.createLocation("Gate A12", "탑승구 A12");

        // when
        Long savedId = locationRepository.save(location);
        flushAndClear();

        Location findLocation = locationRepository.findById(savedId)
                .orElseThrow(() -> new EntityNotFoundException("Location not found"));

        // then
        assertThat(findLocation.getLocationName()).isEqualTo(location.getLocationName());
        assertThat(findLocation.getDescription()).isEqualTo(location.getDescription());
    }

    @DisplayName("존재하지 않는 위치의 pk 로 조회시 빈 옵셔널이 반환된다")
    @Test
    void findByNotExistId() {
        // given
        Long notExistLocationId = 99999L;

        // when
        Optional<Location> locationOpt = locationRepository.findById(notExistLocationId);

        // then
        assertThat(locationOpt).isEmpty();
    }

    private void flushAndClear() {
        em.flush();
        em.clear();
    }

}
