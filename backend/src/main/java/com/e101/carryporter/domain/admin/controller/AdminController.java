package com.e101.carryporter.domain.admin.controller;

import com.e101.carryporter.domain.admin.controller.dto.request.*;
import com.e101.carryporter.domain.admin.controller.dto.response.LockerResponseDto;
import com.e101.carryporter.domain.admin.controller.dto.response.MissionResponseDto;
import com.e101.carryporter.domain.admin.controller.dto.response.RobotResponseDto;
import com.e101.carryporter.domain.admin.service.AdminLockerService;
import com.e101.carryporter.domain.admin.service.AdminService;
import com.e101.carryporter.domain.auth.controller.dto.response.TokenResponseDto;
import com.e101.carryporter.domain.locker.entity.LockerStatus;
import com.e101.carryporter.domain.mission.service.MissionService;
import com.e101.carryporter.domain.robot.entity.RobotStatus;
import com.e101.carryporter.domain.robot.service.RobotService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final RobotService robotService;
    private final AdminService adminService;
    private final AdminLockerService adminLockerService;
    private final MissionService missionService;

    @PostMapping("/boom")
    public ResponseEntity<Void> boom() {
        log.debug("FINISHED 제외한 모든 미션 Failed");
        missionService.failAllExceptFinished();

        log.debug("[DB] 모든 로봇 IDLE");
        log.debug("[Redis] 모든 로봇 IDLE 및 가용 큐 복귀");
        robotService.changeStatusAll(RobotStatus.IDLE);

        log.debug("[BD] 모든 Locker AVAILABLE");
        adminLockerService.changeStatusAll(LockerStatus.AVAILABLE);

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/join")
    public ResponseEntity<Void> join(@RequestBody @Valid JoinRequestDto requestDto) {
        log.debug("관리자 계정 생성 요청, mmEmail = {}", requestDto.getMmEmail());
        adminService.join(requestDto.getMmEmail(), requestDto.getName(), requestDto.getPassword());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponseDto> login(@RequestBody @Valid LoginRequestDto requestDto) {
        log.debug("관리자 login 요청, mmEmail = {}", requestDto.getMmEmail());

        TokenResponseDto tokens = adminService.login(requestDto.getMmEmail(), requestDto.getPassword());

        // create refresh token cookie
        ResponseCookie refreshCookie = createRefreshTokenCookie(tokens.getRefreshToken());

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(TokenResponseDto.builder()
                        .accessToken(tokens.getAccessToken())
                        .refreshToken(null) // 보안 이유로 refresh token 을 header 로
                        .grantType("Bearer")
                        .expiresIn(tokens.getExpiresIn())
                        .build());
    }


    @PostMapping("/missions/{missionId}/unlock")
    public ResponseEntity<Void> unlockRobot(@PathVariable Long missionId) {
        log.debug("관리자 권한 잠금 해제 요청 mission id = {}", missionId);

        robotService.unlockByAdmin(missionId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/missions/{missionId}/lock")
    public ResponseEntity<Void> lockRobot(@PathVariable Long missionId) {

        log.debug("관리자 권한 잠금 요청 mission id = {}", missionId);

        robotService.lockByAdmin(missionId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/missions/{missionId}/dispatch")
    public ResponseEntity<Void> dispatch(@PathVariable Long missionId) {
        log.debug("관리자 권한 이동 요청 mission id = {}", missionId);

        robotService.dispatch(missionId);
        return ResponseEntity.noContent().build();
    }
    // 락커 할당 해제
    @PostMapping("/missions/{missionId}/finalize")
    public ResponseEntity<Void> finalize(@PathVariable Long missionId) {
        log.debug("관리자 최종 점검 완료 - missionId: {}", missionId);

        robotService.finalizeMission(missionId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("missions/{missionId}/store")
    public ResponseEntity<Void> store(@PathVariable Long missionId){
        log.debug("관리자 보관 유지 완료 - missionId : {}", missionId);
        robotService.storeMission(missionId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/missions")
    public ResponseEntity<List<MissionResponseDto>> getAllMissions() {
        log.debug("관리자 전체 미션 조회 요청 (최대 15개)");
        List<MissionResponseDto> missions = adminService.getAllMissions(15);
        return ResponseEntity.ok(missions);
    }

    @GetMapping("/missions/{missionId}")
    public ResponseEntity<MissionResponseDto> getMission(@PathVariable Long missionId) {
        log.debug("관리자 미션 단건 조회 요청 - missionId: {}", missionId);
        MissionResponseDto mission = adminService.getMission(missionId);
        return ResponseEntity.ok(mission);
    }

    @GetMapping("/lockers")
    public ResponseEntity<List<LockerResponseDto>> getAllLockers() {
        log.debug("관리자 전체 사물함 조회 요청");

        List<LockerResponseDto> lockers = adminLockerService.getAllLockers();
        return ResponseEntity.ok(lockers);
    }

    @GetMapping("/lockers/{lockerId}")
    public ResponseEntity<LockerResponseDto> getLocker(@PathVariable Long lockerId) {
        log.debug("관리자 사물함 단건 조회 요청 - lockerId: {}", lockerId);

        LockerResponseDto locker = adminLockerService.getLocker(lockerId);
        return ResponseEntity.ok(locker);
    }
    // 락커 상태 변경
    @PostMapping("/missions/{missionId}/lockers/{lockerId}")
    public ResponseEntity<Void> assignLockerToMission(@PathVariable Long missionId, @PathVariable Long lockerId) {
        adminLockerService.assignLocker(missionId, lockerId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/lockers/{lockerId}/status")
    public ResponseEntity<LockerResponseDto> updateLockerStatus(
            @PathVariable Long lockerId,
            @RequestBody @Valid LockerStatusUpdateRequestDto requestDto) {
        log.debug("관리자 사물함 상태 변경 요청 - lockerId: {}, status: {}", lockerId, requestDto.getStatus());

        LockerResponseDto locker = adminLockerService.updateLockerStatus(lockerId, requestDto.getStatus());
        return ResponseEntity.ok(locker);
    }

    @GetMapping("/users/count")
    public ResponseEntity<Map<String, Long>> getUserCount() {
        log.debug("관리자 전체 사용자 수 조회 요청");
        long count = adminService.getUserCount();
        return ResponseEntity.ok(Map.of("count", count));
    }

    @GetMapping("/robots")
    public ResponseEntity<List<RobotResponseDto>> getAllRobots() {
        log.debug("관리자 전체 로봇 조회 요청");
        List<RobotResponseDto> robots = adminService.getAllRobots();
        return ResponseEntity.ok(robots);
    }

    @GetMapping("/robots/{robotId}")
    public ResponseEntity<RobotResponseDto> getRobot(@PathVariable Long robotId) {
        log.debug("관리자 로봇 단건 조회 요청 - robotId: {}", robotId);
        RobotResponseDto robot = adminService.getRobot(robotId);
        return ResponseEntity.ok(robot);
    }

    @GetMapping("/robots/available/count")
    public ResponseEntity<Map<String, Long>> getAvailableRobotCount() {
        log.debug("관리자 가용 로봇 수 조회 요청");
        Long count = adminService.getAvailableRobotCount();
        return ResponseEntity.ok(Map.of("count", count));
    }

    private ResponseCookie createRefreshTokenCookie(String refreshToken) {
        return ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(7 * 24 * 60 * 60) // 7 days
                .sameSite("None")
                .build();
    }



}
