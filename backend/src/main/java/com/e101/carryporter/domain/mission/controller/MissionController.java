package com.e101.carryporter.domain.mission.controller;

import com.e101.carryporter.domain.mission.controller.dto.request.CreateMissionRequestDto;
import com.e101.carryporter.domain.mission.controller.dto.response.CreateMissionResponseDto;
import com.e101.carryporter.domain.mission.entity.Mission;
import com.e101.carryporter.domain.mission.service.MissionService;
import com.e101.carryporter.domain.user.entity.Role;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/missions")
@RequiredArgsConstructor
public class MissionController {

    private final MissionService missionService;

    @PostMapping
    public ResponseEntity<CreateMissionResponseDto> createMission(@RequestBody @Valid CreateMissionRequestDto requestDto, @RequestAttribute("userId") Long userId, @RequestAttribute("role")Role role) {

        log.debug("Create Mission Request: {}, userId = {}, role = {}", requestDto, userId, role);
        Long missionId = missionService.createMission(userId, requestDto.toServiceRequestDto());

        return ResponseEntity.ok(CreateMissionResponseDto.of(missionId));
    }

    @PostMapping("/{missionId}/return")
    public ResponseEntity<Void> returnRobot(@PathVariable Long missionId, @RequestAttribute("userId") Long userId) {
        log.debug("Return Mission Request: missionId = {}, userId = {}", missionId, userId);
        missionService.returnToMainStation(missionId, userId);
        return ResponseEntity.noContent().build();
    }
}
