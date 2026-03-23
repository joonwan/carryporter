package com.e101.carryporter.domain.user.controller;

import com.e101.carryporter.domain.locker.service.LockerService;
import com.e101.carryporter.domain.locker.service.dto.response.UserLockersServiceResponseDto;
import com.e101.carryporter.domain.locker.service.dto.response.UserStoringLockerServiceResponseDto;
import com.e101.carryporter.domain.mission.entity.MissionStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
public class UserController {

    private final LockerService lockerService;

    @GetMapping("/me/lockers")
    public ResponseEntity<UserLockersServiceResponseDto> getUserLockers(@RequestAttribute("userId") Long userId) {
        log.debug("사용자 사물함 이용 내역 조회 userId = {}", userId);
        return ResponseEntity.ok(lockerService.getUserLockers(userId));
    }

    // todo 쿼리 파라미터 쓰도록 리펙토링 필요
    @GetMapping("/me/lockers/storing")
    public ResponseEntity<UserStoringLockerServiceResponseDto> getUserStoringLocker(@RequestAttribute("userId") Long userId) {
        log.debug("사용자 사용중인 사물함 단건조회 userId = {}", userId);
        return ResponseEntity.ok(lockerService.getStoringLocker(userId));
    }
}
