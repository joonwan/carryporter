// src/api/axiosConfig.ts
import axios from 'axios';

// 1. Axios 인스턴스 생성
export const api = axios.create({
  // 개발 환경: Vite 프록시 사용 (빈 문자열)
  // 프로덕션 환경: 환경 변수의 API 서버 URL 사용
  baseURL: import.meta.env.DEV ? '' : (import.meta.env.VITE_API_URL || ''),

  timeout: 10000, // 10초 타임아웃
  withCredentials: true, // 🚨 중요: 쿠키(Refresh Token)를 주고받기 위한 설정
  headers: {
    'Content-Type': 'application/json',
  },
});

// 2. (선택) 요청 인터셉터: Access Token이 있다면 헤더에 끼워넣기
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('accessToken'); // 저장된 토큰 가져오기
    if (token) {
      config.headers['Authorization'] = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);
export default api;