package com.e101.carryporter.domain.location.repository;

import com.e101.carryporter.domain.location.entity.Location;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class LocationRepository {

    private final EntityManager em;

    public Long save(Location location) {
        em.persist(location);
        return location.getId();
    }

    public Optional<Location> findById(Long locationId) {
        return Optional.ofNullable(em.find(Location.class, locationId));
    }
}
