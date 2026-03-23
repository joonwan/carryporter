import React from 'react';
import { useGlobalSSE } from '@/hooks/useGlobalSSE';

/**
 * SSE Provider 컴포넌트
 *
 * ProtectedRoute에서 사용되어 인증된 사용자에게 전역 SSE 연결을 제공합니다.
 * useGlobalSSE 훅을 실행하여 SSE 구독을 시작하고, 페이지 이동과 무관하게 연결을 유지합니다.
 */
export const SSEProvider: React.FC<{ children?: React.ReactNode }> = ({ children }) => {
  useGlobalSSE(); // SSE 연결 시작

  return <>{children}</>;
};
