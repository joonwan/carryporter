import { BrowserRouter, Routes, Route } from 'react-router-dom'
import MainLayout from './components/layout/MainLayout'
import RobotsPage from './pages/RobotsPage'
import LockersPage from './pages/LockersPage'
import LoginPage from './pages/LoginPage'
import JoinPage from './pages/JoinPage'
import AlertPage from './pages/AlertPage'

/**
 * 메인 앱 컴포넌트
 * - 라우팅 설정
 * - MainLayout으로 페이지 감싸기
 */
function App() {
    return (
        <BrowserRouter basename="/admin">
            <Routes>
                <Route element={<MainLayout />}>
                    <Route path="/" element={<LoginPage />} />
                    <Route path="/robots" element={<RobotsPage />} />
                    <Route path="/lockers" element={<LockersPage />} />
                    <Route path="/join" element={<JoinPage />} /> 
                    <Route path="/alert" element={<AlertPage />} />
                </Route>
            </Routes>
        </BrowserRouter>
    )
}

export default App
