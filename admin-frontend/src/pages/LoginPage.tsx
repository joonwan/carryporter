import { useState } from 'react'
import { NavLink, useNavigate } from 'react-router-dom' // 혹시 회원가입 링크 등에 필요할 수 있음
import { cn } from '@/lib/utils' // 경로에 맞게 수정해주세요
import { Sun, Moon, Mail, Lock } from 'lucide-react'
import { useThemeStore } from '@/store/themeStore' // 사이드바와 동일한 테마 스토어 사용
import { loginApi } from '@/api/authApi' // Add the loginApi import

export default function LoginPage() {
    // 사이드바와 동일한 테마 스토어 사용
    const { theme, toggleTheme } = useThemeStore()
    const navigate = useNavigate()
    const [email, setEmail] = useState('')
    const [password, setPassword] = useState('')
    const [, setIsLoading] = useState(false)
    const [, setErrorMsg] = useState('')

    // LoginPage.tsx 내부 handleSubmit 함수 수정

const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsLoading(true);
    setErrorMsg('');

    try {
        // 🚨 수정 포인트: 백엔드가 원하는 필드명(mmEmail)으로 변경하여 전달
        const requestData = {
            mmEmail: email, // 화면의 state는 email이지만, 보낼 땐 mmEmail로
            password: password
        };

        // API 요청
        const response = await loginApi(requestData);

        // ... 나머지 로직 동일 (토큰 저장 등)
        localStorage.setItem('accessToken', response.accessToken);
        console.log("이동 시도!"); 
        navigate('/robots'); // 메인 페이지 경로로 강제 이동

    } catch (error: any) {
        // ... 에러 처리
    } finally {
        setIsLoading(false);
    }
}

   return (
        <div className="min-h-screen w-full flex items-center justify-center bg-bg-primary transition-colors duration-300 relative overflow-hidden">
            
            {/* 배경 장식 */}
            <div className="absolute top-[-20%] left-[-20%] w-[500px] h-[500px] bg-cyan-500/20 blur-[120px] rounded-full pointer-events-none mix-blend-screen" />
            <div className="absolute bottom-[-20%] right-[-20%] w-[500px] h-[500px] bg-purple-500/20 blur-[120px] rounded-full pointer-events-none mix-blend-screen" />

            {/* 우측 상단 테마 토글 버튼 */}
            <div className="absolute top-6 right-6">
                <button
                    onClick={toggleTheme}
                    className={cn(
                        "flex items-center gap-2 px-4 py-3",
                        "glass-card rounded-lg transition-all",
                        "hover:neon-border-cyan text-text-primary"
                    )}
                >
                    {theme === 'dark' ? (
                        <>
                            <Sun className="w-5 h-5 neon-icon-orange" />
                            <span className="text-sm font-medium">Light Mode</span>
                        </>
                    ) : (
                        <>
                            <Moon className="w-5 h-5 neon-icon-purple" />
                            <span className="text-sm font-medium">Dark Mode</span>
                        </>
                    )}
                </button>
            </div>

            {/* 로그인 카드 */}
            <main className="w-full max-w-md p-8 glass-card rounded-2xl border border-border-default/50 shadow-2xl z-10 relative">
                {/* 로고 및 헤더 */}
                <div className="text-center mb-8">
                    <div className="flex justify-center mb-4">
                        {/* ✅ 수정됨: public 폴더 경로 사용 */}
                        <img 
                            src={theme === 'dark' 
                                ? "./assets/images/robot-white.png"       // 다크모드일 때 (기존 로고 파일명)
                                : "./assets/images/robot-white.png" // 라이트모드일 때 (새 파일명)
                            } 
                            alt="Carry Porter Logo" 
                            className="w-24 h-24 object-contain drop-shadow-[0_0_15px_rgba(0,255,255,0.3)]"
                        />
                    </div>
                    <h1 className="gradient-title text-3xl font-bold mb-2">Welcome Back</h1>
                    <p className="text-text-secondary text-sm">
                        CARRY PORTER Admin Dashboard에 접속하세요.
                    </p>
                </div>
                <div className="text-center mt-6">
    <span className="text-text-secondary text-sm">Don't have an account? </span>
    <NavLink 
        to="/join" 
        className="text-cyan-400 hover:text-cyan-300 font-semibold text-sm hover:underline transition-colors"
    >
        Sign Up
    </NavLink>
</div>

                {/* 로그인 폼 */}
                <form onSubmit={handleSubmit} className="space-y-6">
                    <div className="space-y-2">
                        <label htmlFor="email" className="text-sm font-medium text-text-primary ml-1">
                            Email
                        </label>
                        <div className="relative group">
                            <Mail className="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-text-secondary group-focus-within:text-cyan-400 transition-colors" />
                            <input
                                id="email"
                                type="email"
                                value={email}
                                onChange={(e) => setEmail(e.target.value)}
                                placeholder="admin@carryporter.com"
                                className={cn(
                                    "w-full pl-12 pr-4 py-3 rounded-lg outline-none transition-all",
                                    "bg-bg-secondary/50 border border-border-default",
                                    "text-text-primary placeholder:text-text-secondary/50",
                                    "focus:neon-border-cyan focus:bg-bg-secondary"
                                )}
                                required
                            />
                        </div>
                    </div>

                    <div className="space-y-2">
                        <div className="flex justify-between items-center ml-1">
                            <label htmlFor="password" className="text-sm font-medium text-text-primary">
                                Password
                            </label>
                        </div>
                        <div className="relative group">
                            <Lock className="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-text-secondary group-focus-within:text-cyan-400 transition-colors" />
                            <input
                                id="password"
                                type="password"
                                value={password}
                                onChange={(e) => setPassword(e.target.value)}
                                placeholder="••••••••"
                                className={cn(
                                    "w-full pl-12 pr-4 py-3 rounded-lg outline-none transition-all",
                                    "bg-bg-secondary/50 border border-border-default",
                                    "text-text-primary placeholder:text-text-secondary/50",
                                    "focus:neon-border-cyan focus:bg-bg-secondary"
                                )}
                                required
                            />
                        </div>
                    </div>

                    <button
                        type="submit"
                        className={cn(
                            "w-full py-3 px-4 rounded-lg font-bold text-white transition-all mt-8",
                            "bg-gradient-to-r from-cyan-500 to-blue-600 hover:from-cyan-400 hover:to-blue-500",
                            "shadow-[0_0_20px_rgba(0,255,255,0.3)] hover:shadow-[0_0_30px_rgba(0,255,255,0.5)]",
                            "active:scale-[0.98]"
                        )}
                    >
                        Login to Dashboard
                    </button>
                </form>
            </main>
        </div>
    )
}