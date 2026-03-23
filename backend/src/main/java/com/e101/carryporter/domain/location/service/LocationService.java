package com.e101.carryporter.domain.location.service;

import com.e101.carryporter.domain.location.entity.Location;
import com.e101.carryporter.domain.location.exception.LocationErrorCode;
import com.e101.carryporter.domain.location.repository.LocationRepository;
import com.e101.carryporter.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class LocationService {

    private final LocationRepository locationRepository;

    public Location findById(Long locationId) {
        return locationRepository.findById(locationId)
                .orElseThrow(() -> new BusinessException(LocationErrorCode.LOCATION_NOT_FOUND));
    }
}
