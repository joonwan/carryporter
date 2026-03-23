import { useEffect, useRef } from 'react';
import { useAuthStore, getHasLoggedInBefore } from '../store/authStore';
import { reissue } from '../api/auth.api';

/**
 * 앱 시작 시 세션 복원을 처리하는 훅
 *
 * 동작 원리:
 * 1. /api/auth/reissue 호출 (refreshToken은 httpOnly 쿠키로 자동 전송)
 * 2. 성공: 새 accessToken 저장 → isAuthenticated = true
 * 3. 실패: 인증 상태 초기화 → isAuthenticated = false
 */
export const useSessionRestore = () => {
    const {
        isAuthenticated,
        isInitialized,
        setAccessToken,
        setUser,
        setAuthenticated,
        setInitialized,
        clearAuth,
    } = useAuthStore();

    // 세션 복원 시도 여부 추적 (무한 루프 방지)
    const isRestoringRef = useRef(false);

    useEffect(() => {
        // 이미 초기화된 경우 스킵
        if (isInitialized) {
            return;
        }

        // 이미 인증된 상태면 초기화만 완료
        if (isAuthenticated) {
            setInitialized(true);
            return;
        }

        // 이미 복원 시도 중이면 스킵 (중복 호출 방지)
        if (isRestoringRef.current) {
            return;
        }

        // 복원 시도 시작
        isRestoringRef.current = true;

        // refreshToken으로 세션 복원 시도
        const restoreSession = async () => {
            // 한 번도 로그인한 적 없으면 세션 복원 스킵
            if (!getHasLoggedInBefore()) {
                if (import.meta.env.DEV) console.log('첫 방문 사용자 - 세션 복원 스킵');
                setInitialized(true);
                isRestoringRef.current = false;
                return;
            }

            try {
                const response = await reissue();
                setAccessToken(response.accessToken);
                setUser(response.user);
                setAuthenticated(true);
                if (import.meta.env.DEV) {
                    console.log('세션 복원 성공', {
                        email: response.user.email,
                        role: response.user.role,
                    });
                }
            } catch (error) {
                // 로그 레벨 낮춤 (에러가 아닌 정상 동작)
                if (import.meta.env.DEV) console.log('세션 복원 실패 (refreshToken 만료):', error);
                // refreshToken이 만료된 경우 정리
                clearAuth();
            } finally {
                setInitialized(true);
                isRestoringRef.current = false;
            }
        };

        restoreSession();
        // isAuthenticated를 의존성에서 제거하여 무한 루프 방지
        // refreshToken은 httpOnly 쿠키로 관리되므로 의존성에서 제거
    }, [isInitialized, setAccessToken, setUser, setAuthenticated, setInitialized, clearAuth]);

    return { isInitialized };
};
