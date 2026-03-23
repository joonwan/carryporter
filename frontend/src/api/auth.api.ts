import { jwtDecode } from 'jwt-decode';
import apiClient from './axios';
import type {
  SendCodeRequest,
  SendCodeResponse,
  LoginRequest,
  LoginResponse,
  User,
} from '../types/auth.types';

// JWT Payload 타입 정의
interface JwtPayload {
  sub: string;      // 이메일 (표준 JWT claim)
  userId: number;   // 사용자 ID
  exp: number;      // 만료 시간
  iat: number;      // 발급 시간
}

// 1단계: 인증번호 발송 (이메일 + 비밀번호)
export const sendCode = async (data: SendCodeRequest): Promise<SendCodeResponse> => {
  const response = await apiClient.post<SendCodeResponse>('/api/auth/request', data);
  return response.data;
};

// 2단계: CODE 인증 (이메일 + 선택한 CODE)
export const login = async (data: LoginRequest): Promise<LoginResponse> => {
  const response = await apiClient.post<LoginResponse>('/api/auth/verify', data);
  return response.data;
};

// 토큰 재발급
// refreshToken은 httpOnly 쿠키로 관리되며 withCredentials: true로 자동 전송됨
export const reissue = async (): Promise<{ accessToken: string; user: User }> => {
  const response = await apiClient.post<{ accessToken: string; tokenType: string; expiresIn: number }>(
    '/api/auth/reissue',
    null
  );

  // JWT 디코딩으로 user 정보 추출
  const decoded = jwtDecode<JwtPayload>(response.data.accessToken);

  return {
    accessToken: response.data.accessToken,
    user: {
      id: decoded.userId.toString(),
      email: decoded.sub,  // sub 필드에서 이메일 추출
      role: 'USER',        // 기본값 (JWT에 role 정보 없음)
    },
  };
};
