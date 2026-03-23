import { Outlet } from 'react-router-dom'
import { useThemeStore } from '@/store/themeStore'
import { cn } from '@/lib/utils'
import { useEffect } from 'react'
import { useRobotSSE } from '@/hooks/UserRobotSSE'

/**
 * 메인 레이아웃 컴포넌트
 * - 사이드바 + 메인 컨텐츠 영역
 * - 테마 적용
 * - 전역 SSE 연결 (로그인 후 페이지 이동해도 유지)
 */
export default function MainLayout() {
    const { theme } = useThemeStore()

    // 전역 SSE 연결 (토큰이 있을 때만 연결)
    useRobotSSE()

    // document에 light-mode 클래스 적용
    useEffect(() => {
        if (theme === 'light') {
            document.documentElement.classList.add('light-mode')
        } else {
            document.documentElement.classList.remove('light-mode')
        }
    }, [theme])

    return (
        <div className={cn(
            "min-h-screen transition-colors duration-300",
        )}>
            {/* 사이드바 */}
            {/* <Sidebar /> */}

            {/* 메인 컨텐츠 */}
            <main className="min-h-screen">
                <Outlet />
            </main>
        </div>
    )
}
