
/**
 * 회원가입 요청 데이터
 * Backend: JoinRequestDto
 */
export interface JoinRequest {
  mmEmail: string; // 🚨 주의: Backend DTO와 동일하게 'mmEmail'로 맞춰야 함
  name: string;
  password: string;
}

/**
 * 로그인 요청 데이터
 * Backend: LoginRequestDto
 */
export interface LoginRequest {
  mmEmail: string; // 🚨 주의: Backend DTO와 동일하게 'mmEmail'로 맞춰야 함
  password: string;
}

/**
 * 로그인 성공 응답 데이터
 * Backend: TokenResponseDto
 */
export interface TokenResponse {
  accessToken: string;
  refreshToken: string | null; // 백엔드에서 body에는 null로 보내므로 null 허용
  grantType: string;
  expiresIn: number;
}