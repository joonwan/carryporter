import { create } from 'zustand'
import { persist } from 'zustand/middleware'

/**
 * 테마 타입 정의
 */
type Theme = 'light' | 'dark'

/**
 * 테마 스토어 인터페이스
 */
interface ThemeState {
    /** 현재 테마 */
    theme: Theme
    /** 테마 토글 */
    toggleTheme: () => void
    /** 특정 테마 설정 */
    setTheme: (theme: Theme) => void
}

/**
 * 테마 관리 Store
 * - Light/Dark 모드 전환
 * - localStorage에 영구 저장
 */
export const useThemeStore = create<ThemeState>()(
    persist(
        (set) => ({
            theme: 'dark',

            toggleTheme: () =>
                set((state) => ({
                    theme: state.theme === 'dark' ? 'light' : 'dark',
                })),

            setTheme: (theme) => set({ theme }),
        }),
        {
            name: 'admin-theme', // localStorage 키
        }
    )
)
