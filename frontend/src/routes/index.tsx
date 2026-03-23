import { Routes, Route, Navigate } from 'react-router-dom';
import ProtectedRoute from './ProtectedRoute';
import SplashPage from '../pages/SplashPage';
import LoginPage from '../pages/LoginPage';
import CodeVerificationPage from '../pages/CodeVerificationPage';
import HomePage from '../pages/HomePage';
import TicketScanPage from '../pages/TicketScanPage';
import TicketDetailPage from '../pages/TicketDetailPage';
import MissionCreatePage from '../pages/MissionCreatePage';
import MissionTrackPage from '../pages/MissionTrackPage';

const AppRoutes = () => {
  return (
    <Routes>
      {/* 스플래시 화면 - 첫 진입점 */}
      <Route path="/" element={<SplashPage />} />

      {/* 공개 라우트 (로그인 전) */}
      <Route path="/login" element={<LoginPage />} />
      <Route path="/login/verify" element={<CodeVerificationPage />} />

      {/* 보호된 라우트 (로그인 필요) */}
      <Route element={<ProtectedRoute />}>
        <Route path="/home" element={<HomePage />} />
        <Route path="/ticket/scan" element={<TicketScanPage />} />
        <Route path="/ticket/detail" element={<TicketDetailPage />} />

        {/* 미션 관련 라우트 */}
        <Route path="/mission/create" element={<MissionCreatePage />} />
        <Route path="/mission/track" element={<MissionTrackPage />} />
      </Route>

      {/* 알 수 없는 경로는 스플래시로 */}
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
};

export default AppRoutes;
