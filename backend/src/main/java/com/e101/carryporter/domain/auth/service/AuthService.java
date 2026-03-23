package com.e101.carryporter.domain.auth.service;

import com.e101.carryporter.domain.auth.service.dto.request.AuthServiceReqeustDto;
import com.e101.carryporter.domain.auth.service.dto.request.LockServiceRequestDto;
import com.e101.carryporter.domain.auth.service.dto.request.VerifyCodeServiceRequestDto;
import com.e101.carryporter.domain.auth.repository.*;
import com.e101.carryporter.domain.auth.controller.dto.response.AuthResponseDto;
import com.e101.carryporter.domain.auth.controller.dto.response.TokenResponseDto;
import com.e101.carryporter.domain.auth.service.dto.request.VerifyPasswordServiceRequestDto;
import com.e101.carryporter.domain.mission.entity.Mission;
import com.e101.carryporter.domain.mission.entity.MissionStatus;
import com.e101.carryporter.domain.mission.event.MissionLockRequestEvent;
import com.e101.carryporter.domain.mission.repository.MissionRepository;
import com.e101.carryporter.domain.user.entity.User;
import com.e101.carryporter.domain.user.event.UserAuthFailedEvent;
import com.e101.carryporter.domain.user.event.UserAuthSuccessEvent;
import com.e101.carryporter.domain.user.repository.UserRepository;
import com.e101.carryporter.global.utils.JwtUtils;
import com.e101.carryporter.global.utils.MattermostClient;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private final JwtUtils jwtUtils;
    private final MattermostClient mattermostClient;
    private final ApplicationEventPublisher eventPublisher;

    private final UserRepository userRepository;

    private final EmailCodeRedisRepository emailCodeRepository;
    private final TempPasswordRedisRepository tempPasswordRepository;
    private final UserPasswordRedisRepository userPasswordRepository;
    private final RefreshTokenRedisRepository refreshTokenRepository;
    private final MissionRepository missionRepository;

    /**
     * 3-1. 인증번호 요청 (Command DTO 사용)
     */
    public AuthResponseDto requestAuth(AuthServiceReqeustDto command) {
        String email = command.email();
        Integer password = command.password();

        // 1. 인증번호 생성 (2자리)
        SecureRandom random = new SecureRandom();
        Integer authCode = random.nextInt(90) + 10;

        // 2. Redis 저장
        emailCodeRepository.save(email, authCode);
        tempPasswordRepository.save(email, password);

        // 3. Mattermost 발송
        mattermostClient.sendMessage(email, "CarryPorter 인증번호: [" + authCode + "]");

        long expiresIn = emailCodeRepository.getExpireSeconds();
        return new AuthResponseDto("SUCCESS", "인증번호가 전송되었습니다.", authCode, expiresIn);
    }

    /**
     * 3-2. 인증 및 토큰 발급 (Access + Refresh 동시 반환)
     */
    public TokenResponseDto verifyAuth(VerifyCodeServiceRequestDto command) {
        String email = command.email();
        Integer inputCode = command.code();

        // 1. 인증번호 검증
        Integer redisCode = emailCodeRepository.get(email)
                .orElseThrow(() -> new IllegalArgumentException("AUTH_001:인증번호가 만료되었습니다."));

        if (!redisCode.equals(inputCode)) {
            throw new IllegalArgumentException("AUTH_001:인증번호가 일치하지 않습니다.");
        }

        // 2. 임시 비밀번호 승격 준비
        Integer tempPassword = tempPasswordRepository.get(email)
                .orElseThrow(() -> new IllegalArgumentException("AUTH_002:인증 시간이 만료되었습니다."));

        //3. 유저가 있으면 가져오고, 없으면 생성해서 DB에 저장
        // findByMmEmail로 먼저 조회합니다.
        User user = userRepository.findByMmEmail(email)
                .orElseGet(() -> {
                    // 존재하지 않을 때만 새로 만들고 저장합니다.
                    User newUser = User.createUser(email);
                    userRepository.save(newUser);
                    return newUser;
                });

        Long savedId = user.getId();

        userPasswordRepository.save(savedId, tempPassword);

        // 4. 토큰 발급 (Access & Refresh 둘 다 생성)
        String accessToken = jwtUtils.createAccessToken(email, savedId, user.getRole());
        String refreshToken = jwtUtils.createRefreshToken(savedId);

        // 5. Refresh Token Redis 저장
        refreshTokenRepository.save(savedId, refreshToken);

        // 6. 사용 완료 데이터 삭제
        emailCodeRepository.delete(email);
        tempPasswordRepository.delete(email);

        return TokenResponseDto.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken) // 추가됨
                .grantType("Bearer")
                .expiresIn(jwtUtils.getAccessTokenValidityInSeconds())
                .build();
    }

    /**
     * 3-3. 토큰 재발급
     */
    /**
     * 3-3. 토큰 재발급 (수정됨)
     */
    public TokenResponseDto reissue(String refreshToken) {
        // 1. 유효성 검사
        if (!jwtUtils.validateToken(refreshToken)) {
            throw new IllegalArgumentException("AUTH_003:유효하지 않은 Refresh Token입니다.");
        }

        // 2. 유저 ID 추출 및 Redis 조회
        Long userId = jwtUtils.getUserIdFromToken(refreshToken);
        String savedToken = refreshTokenRepository.get(userId)
                .orElseThrow(() -> new IllegalArgumentException("AUTH_004:로그인 정보가 없거나 만료되었습니다."));

        // 3. 토큰 일치 여부 확인 (탈취 감지)
        if (!savedToken.equals(refreshToken)) {
            throw new IllegalArgumentException("AUTH_005:토큰 정보가 일치하지 않습니다.");
        }

        // 4. 유저 확인
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("AUTH_006:존재하지 않는 유저입니다."));

        // 5. 새 토큰 생성 (Rotation)
        String newAccessToken = jwtUtils.createAccessToken(user.getMmEmail(), user.getId(), user.getRole());
        String newRefreshToken = jwtUtils.createRefreshToken(user.getId()); // 여기서 새로 만듦

        // 6. Redis 업데이트 (기존 키에 덮어쓰기)
        // Tip: 저장할 때 TTL(만료시간)도 같이 설정해주는 것이 좋습니다.
        refreshTokenRepository.save(user.getId(), newRefreshToken);

        // 7. 응답 반환
        return TokenResponseDto.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken) // ★ 수정완료: 반드시 '새 토큰'을 내려줘야 함!
                .grantType("Bearer")
                .expiresIn(jwtUtils.getAccessTokenValidityInSeconds())
                .build();
    }

    /**
     * 모바일에서 입력한 비밀번호 검증 후 로봇 문열림
     */

    public void unlockRequest(VerifyPasswordServiceRequestDto command){
        //미션 조회
        Mission mission = missionRepository.findById(command.missionId())
                .orElseThrow(()-> new IllegalArgumentException("존재하지 않는 미션입니다."));

        //소유권 검증 (요청자 == 미션주인?)

        //미션 상태 검증(로봇 도착 여부)
        if(mission.getMissionStatus() != MissionStatus.ARRIVED){
            throw new IllegalStateException("인증 가능한 상태가 아닙니다.(로봇 미도착 또는 이미 완료)");
        }

        //redis 비밀번호 조회(userId 사용)
        Integer savedPassword = userPasswordRepository.get(command.userId())
                .orElseThrow(()-> new IllegalArgumentException("인증 시간이 만료되었습니다."));
        String robotMacAddress = mission.getRobot().getMacAddress();

        //비밀번호 일치 확인
        if(String.valueOf(savedPassword).equals(String.valueOf(command.password()))){
            log.warn("인증 성공: userId={}, missionId={}", command.userId(), command.userId());

            //성공 이벤트 발행->Mqtt로봇 문을 연다
            eventPublisher.publishEvent(new UserAuthSuccessEvent(
                    command.missionId(),
                    command.userId(),
                    robotMacAddress
            ));
            }else{
            log.warn("인증 실패(비밀번호 불일치): userId={}, missionId={}", command.userId(), command.userId());

            //실패 이벤트 발행 -> failureCountHandler 카운트 증가
            eventPublisher.publishEvent(new UserAuthFailedEvent(
                    command.missionId(),
                    command.userId(),
                    robotMacAddress
            ));

            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");

        }
    }
    @Transactional
    public void lockRequest(LockServiceRequestDto command){

        Mission mission = missionRepository.findById(command.missionId())
                .orElseThrow(()-> new IllegalArgumentException("존재하지 않는 미션입니다."));
        String robotMacAddress = mission.getRobot().getMacAddress();

        //성공 이벤트 발행->Mqtt로봇 문을 잠금 요청을 보낸다
        eventPublisher.publishEvent(new MissionLockRequestEvent(
                command.missionId(),
                command.userId(),
                robotMacAddress
        ));
        log.info("[LOCK-SERVICE] 미션 {}에 대한 로봇 {} 잠금 요청 발행", mission.getId(), robotMacAddress);
    }
}