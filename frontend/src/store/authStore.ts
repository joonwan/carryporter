import { create } from 'zustand';
import type { User } from '../types/auth.types';

// localStorage 키 상수
const HAS_LOGGED_IN_KEY = 'hasLoggedInBefore';

// 로그인 이력 플래그 저장
const setHasLoggedInBefore = () => {
  localStorage.setItem(HAS_LOGGED_IN_KEY, 'true');
};

// 로그인 이력 플래그 조회
export const getHasLoggedInBefore = (): boolean => {
  return localStorage.getItem(HAS_LOGGED_IN_KEY) === 'true';
};

interface AuthState {
  // 상태
  accessToken: string | null;
  user: User | null;
  isAuthenticated: boolean;
  isInitialized: boolean; // 세션 복원 완료 여부

  // 액션
  login: (accessToken: string, user: User) => void;
  clearAuth: () => void; // 토큰 만료 시 내부 사용
  setAccessToken: (token: string) => void;
  setUser: (user: User) => void; // User 정보만 업데이트
  setAuthenticated: (value: boolean) => void;
  setInitialized: (value: boolean) => void;
}

export const useAuthStore = create<AuthState>((set) => ({
  // 초기 상태 (refreshToken은 httpOnly 쿠키로 관리되므로 상태에서 제거)
  accessToken: null,
  user: null,
  isAuthenticated: false,
  isInitialized: false,

  // 로그인 성공 시 호출 (refreshToken은 백엔드가 httpOnly 쿠키로 설정)
  login: (accessToken: string, user: User) => {
    setHasLoggedInBefore(); // 로그인 이력 저장
    set({
      accessToken,
      user,
      isAuthenticated: true,
      isInitialized: true,
    });
  },

  // 인증 정보 초기화 (토큰 만료 시 내부 사용)
  // refreshToken 쿠키는 브라우저에서 자동 만료됨 (httpOnly라 JS에서 삭제 불가)
  clearAuth: () => {
    set({
      accessToken: null,
      user: null,
      isAuthenticated: false,
    });
  },

  // Access Token 갱신 시 호출
  setAccessToken: (token: string) => {
    set({ accessToken: token });
  },

  // User 정보만 업데이트
  setUser: (user: User) => {
    set({ user });
  },

  // 인증 상태 설정
  setAuthenticated: (value: boolean) => {
    set({ isAuthenticated: value });
  },

  // 초기화 상태 설정
  setInitialized: (value: boolean) => {
    set({ isInitialized: value });
  },
}));
