import { NavLink } from 'react-router-dom'
import { cn } from '@/lib/utils'
import { LayoutDashboard, Bot, Archive, Sun, Moon } from 'lucide-react'
import { useThemeStore } from '@/store/themeStore'

/**
 * 사이드바 네비게이션 아이템
 */
const navItems = [
    { path: '/', label: 'Dashboard', icon: LayoutDashboard },
    { path: '/robots', label: 'Robots', icon: Bot },
    { path: '/lockers', label: 'Lockers', icon: Archive },
]

/**
 * 사이드바 컴포넌트
 * - 페이지 네비게이션
 * - Light/Dark 모드 토글
 */
export default function Sidebar() {
    const { theme, toggleTheme } = useThemeStore()

    return (
        <aside className="fixed left-0 top-0 h-screen w-64 glass-card border-r border-border-default flex flex-col">
            {/* 로고 */}
            <div className="p-6 border-b border-border-default/50">
                <h1 className="gradient-title text-xl font-bold">
                    CARRY PORTER
                </h1>
                <p className="text-text-secondary text-xs mt-1">
                    Admin Dashboard
                </p>
            </div>

            {/* 네비게이션 */}
            <nav className="flex-1 p-4">
                <ul className="space-y-2">
                    {navItems.map((item) => (
                        <li key={item.path}>
                            <NavLink
                                to={item.path}
                                className={({ isActive }) =>
                                    cn(
                                        "flex items-center gap-3 px-4 py-3 rounded-lg transition-all",
                                        "text-text-secondary hover:text-text-primary",
                                        "hover:bg-bg-secondary/50",
                                        isActive && "bg-bg-secondary text-text-primary neon-border-cyan"
                                    )
                                }
                            >
                                <item.icon className="w-5 h-5" />
                                <span className="text-sm font-medium">{item.label}</span>
                            </NavLink>
                        </li>
                    ))}
                </ul>
            </nav>

            {/* 테마 토글 */}
            <div className="p-4 border-t border-border-default/50">
                <button
                    onClick={toggleTheme}
                    className={cn(
                        "w-full flex items-center justify-center gap-2 px-4 py-3",
                        "glass-card rounded-lg transition-all",
                        "hover:neon-border-cyan"
                    )}
                >
                    {theme === 'dark' ? (
                        <>
                            <Sun className="w-5 h-5 neon-icon-orange" />
                            <span className="text-sm">Light Mode</span>
                        </>
                    ) : (
                        <>
                            <Moon className="w-5 h-5 neon-icon-purple" />
                            <span className="text-sm">Dark Mode</span>
                        </>
                    )}
                </button>
            </div>
        </aside>
    )
}
