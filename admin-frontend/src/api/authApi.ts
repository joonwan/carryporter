// src/api/authApi.ts
import api from './axiosConfig';
import type { JoinRequest, LoginRequest, TokenResponse } from '@/types/auth';

// URL Prefix (백엔드 컨트롤러의 @RequestMapping 확인 필요)
// 전체 경로 사용 (frontend 패턴과 동일)
const ADMIN_URL = '/api/admin'; 

/**
 * 관리자 회원가입
 * POST /api/admin/join
 */
export const joinApi = async (data: JoinRequest) => {
  // 백엔드 리턴 타입이 ResponseEntity<Void>이므로 제네릭을 void로 설정
  const response = await api.post<void>(`${ADMIN_URL}/join`, data);
  return response.data;
};

/**
 * 관리자 로그인
 * POST /api/admin/login
 */
export const loginApi = async (data: LoginRequest) => {
  // 백엔드 리턴 타입이 ResponseEntity<TokenResponseDto>
  const response = await api.post<TokenResponse>(`${ADMIN_URL}/login`, data);
  return response.data;
};