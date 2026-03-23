package com.e101.carryporter.global.utils;

import com.e101.carryporter.domain.user.entity.Role;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Slf4j
@Component
public class JwtUtils {

    private final Key key;
    private final long accessTokenExpTime;
    private final long refreshTokenExpTime; // [1. 추가] Refresh Token 만료 시간 변수

    // application.properties에서 값 가져오기
    public JwtUtils(
            @Value("${jwt.secret}") String secretKey,
            @Value("${jwt.expiration_time:86400000}") long accessTokenExpTime, // 기본값 24시간
            @Value("${jwt.refresh_expiration_time:604800000}") long refreshTokenExpTime // [추가] 7일 (기본값)

    ) {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        this.key = Keys.hmacShaKeyFor(keyBytes);
        this.accessTokenExpTime = accessTokenExpTime;
        this.refreshTokenExpTime = refreshTokenExpTime;
    }

    /**
     * Access Token 생성
     * @param mmEmail 사용자 이메일 (Subject로 사용)
     * @param userId 사용자 DB ID (Claim으로 추가 정보 저장)
     * @param role 사용자 권한 (Claim으로 추가 정보 저장)
     * @return 생성된 JWT 토큰 문자열
     */
    public String createAccessToken(String mmEmail, Long userId, Role role) {
        return Jwts.builder()
                .setSubject(mmEmail) // 토큰 제목(주인) = 이메일
                .claim("userId", userId) // 추가 정보 = userId
                .claim("role", role.name()) // 추가 정보 = role (BASIC 또는 ADMIN)
                .setIssuedAt(new Date(System.currentTimeMillis())) // 발행 시간
                .setExpiration(new Date(System.currentTimeMillis() + accessTokenExpTime)) // 만료 시간
                .signWith(key, SignatureAlgorithm.HS256) // 암호화 알고리즘
                .compact();
    }

    /**
     * [3. 추가] Refresh Token 생성
     * 보통 Refresh Token에는 많은 정보를 담지 않고, 만료 기간만 길게 잡습니다.
     */
    public String createRefreshToken(Long userId) {
        return Jwts.builder()
                .setSubject(String.valueOf(userId)) // 주인을 userId로 설정
                .claim("userId", userId)      // 혹시 모르니 claim에도 넣음
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + refreshTokenExpTime)) // ★ 긴 시간 적용
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 토큰에서 이메일(Subject) 추출
     */
    public String getMmEmailFromToken(String token) {
        return parseClaims(token).getSubject();
    }

    /**
     * 토큰에서 UserId 추출
     */
    public Long getUserIdFromToken(String token) {
        return parseClaims(token).get("userId", Long.class);
    }

    /**
     * 토큰에서 Role 추출
     */
    public Role getRoleFromToken(String token) {
        String roleString = parseClaims(token).get("role", String.class);
        return Role.valueOf(roleString);
    }

    /**
     * 토큰 유효성 검증
     * @param token 검사할 토큰
     * @return 유효하면 true, 아니면 false
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (io.jsonwebtoken.security.SecurityException | MalformedJwtException e) {
            log.info("Invalid JWT Token", e);
        } catch (ExpiredJwtException e) {
            log.info("Expired JWT Token", e);
        } catch (UnsupportedJwtException e) {
            log.info("Unsupported JWT Token", e);
        } catch (IllegalArgumentException e) {
            log.info("JWT claims string is empty.", e);
        }
        return false;
    }

    // 내부적으로 토큰을 파싱(해석)하는 메서드
    private Claims parseClaims(String accessToken) {
        try {
            return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(accessToken).getBody();
        } catch (ExpiredJwtException e) {
            return e.getClaims();
        }
    }

    /**
     * [추가] 외부에서 만료 시간을 조회할 수 있게 해주는 메서드
     * 프론트엔드에는 보통 '초(Second)' 단위로 주니까 / 1000 해서 반환
     */
    public long getAccessTokenValidityInSeconds() {
        return accessTokenExpTime / 1000;
    }
}