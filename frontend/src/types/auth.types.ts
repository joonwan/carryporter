// 사용자 정보 타입
export interface User {
  id: string;
  email: string;
  name?: string;
  role: 'USER' | 'ADMIN';
}

// 1단계: 인증번호 발송 요청
export interface SendCodeRequest {
  email: string;              // Mattermost 이메일
  password: number;           // 4자리 비밀번호 (로봇 인증용)
}

// 1단계: 인증번호 발송 응답
export interface SendCodeResponse {
  status: string;             // "SUCCESS"
  message: string;            // "인증번호가 전송되었습니다."
  expiresIn: number;          // 만료 시간 (초)
  code: number;                // CODE 번호 (예: 35)
}

// 2단계: CODE 인증 요청
export interface LoginRequest {
  email: string;              // Mattermost 이메일
  code: number;                // 선택한 CODE 번호
}

// 2단계: CODE 인증 응답 (토큰 발급)
// refreshToken은 httpOnly 쿠키로 설정되며 body에서는 null로 반환됨
export interface LoginResponse {
  accessToken: string;        // JWT 액세스 토큰
  refreshToken: string | null; // httpOnly 쿠키로 관리, body에서는 null
  tokenType: string;          // "Bearer"
  expiresIn: number;          // 토큰 만료 시간 (초)
}

// 인증 성공 응답 (토큰 + 사용자 정보)
export interface AuthResponse {
  accessToken: string;
  refreshToken: string | null; // httpOnly 쿠키로 관리
  user: User;
}

// API 에러 응답
export interface ApiError {
  code: string;
  message: string;
  details?: Record<string, any>;
}
