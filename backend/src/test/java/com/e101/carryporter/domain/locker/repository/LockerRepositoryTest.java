package com.e101.carryporter.domain.locker.repository;

import com.e101.carryporter.domain.locker.entity.Locker;
import com.e101.carryporter.domain.locker.entity.LockerStatus;
import com.e101.carryporter.support.IntegrationTestSupport;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class LockerRepositoryTest extends IntegrationTestSupport {

    @Autowired
    LockerRepository lockerRepository;

    @Autowired
    EntityManager em;

    @DisplayName("라커를 저장할 수 있다.")
    @Test
    void saveLocker() {
        // given
        Locker locker = Locker.createLocker("A-001");

        // when
        Long savedId = lockerRepository.save(locker);
        flushAndClear();

        Locker findLocker = lockerRepository.findById(savedId)
                .orElseThrow(() -> new EntityNotFoundException("Locker not found"));

        // then
        assertThat(findLocker.getLockerCode()).isEqualTo(locker.getLockerCode());
        assertThat(findLocker.getLockerStatus()).isEqualTo(LockerStatus.AVAILABLE);
    }

    @DisplayName("존재하지 않는 라커의 pk 로 조회시 빈 옵셔널이 반환된다")
    @Test
    void findByNotExistId() {
        // given
        Long notExistLockerId = 99999L;

        // when
        Optional<Locker> lockerOpt = lockerRepository.findById(notExistLockerId);

        // then
        assertThat(lockerOpt).isEmpty();
    }

    private void flushAndClear() {
        em.flush();
        em.clear();
    }

}
