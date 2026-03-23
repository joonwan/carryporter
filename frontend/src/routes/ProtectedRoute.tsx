import React from 'react';
import { Navigate, Outlet } from 'react-router-dom';
import { useAuthStore } from '../store/authStore';
import { SSEProvider } from '../components/common/SSEProvider';

const ProtectedRoute: React.FC = () => {
  const { isAuthenticated } = useAuthStore();

  // 로그인 안 했으면 로그인 페이지로 리다이렉트
  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  // ✅ SSEProvider로 자식 컴포넌트를 래핑하여 전역 SSE 연결 제공
  return (
    <SSEProvider>
      <Outlet />
    </SSEProvider>
  );
};

export default ProtectedRoute;
