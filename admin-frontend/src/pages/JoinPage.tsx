import { useState } from 'react'
import { useNavigate, NavLink } from 'react-router-dom'
import { useThemeStore } from '@/store/themeStore'
import { cn } from '@/lib/utils'
import { Sun, Moon, Mail, Lock, User, Loader2, ArrowLeft } from 'lucide-react'
import { joinApi } from '@/api/authApi' 

export default function JoinPage() {
    const navigate = useNavigate()
    const { theme, toggleTheme } = useThemeStore()
    
    // 상태 관리
    const [email, setEmail] = useState('')
    const [name, setName] = useState('') // 이름 추가
    const [password, setPassword] = useState('')
    const [isLoading, setIsLoading] = useState(false)
    const [errorMsg, setErrorMsg] = useState('')

    const handleJoin = async (e: React.FormEvent) => {
        e.preventDefault()
        setIsLoading(true)
        setErrorMsg('')

        try {
            // 백엔드 DTO에 맞춰서 데이터 전송 (mmEmail, name, password)
            await joinApi({
                mmEmail: email,
                name: name,
                password: password
            })

            // 성공 시 알림 후 로그인 페이지로 이동
            alert('회원가입이 완료되었습니다! 로그인해주세요.')
            navigate('/') // 로그인 페이지 경로로 이동

        } catch (error: any) {
            console.error("회원가입 실패:", error)
            // 백엔드에서 보낸 에러 메시지가 있다면 표시
            if (error.response?.data?.message) {
                setErrorMsg(error.response.data.message)
            } else {
                setErrorMsg("회원가입 중 오류가 발생했습니다.")
            }
        } finally {
            setIsLoading(false)
        }
    }

    return (
        <div className="min-h-screen w-full flex items-center justify-center bg-bg-primary transition-colors duration-300 relative overflow-hidden">
            
            {/* 배경 장식 (로그인 페이지와 동일) */}
            <div className="absolute top-[-20%] left-[-20%] w-[500px] h-[500px] bg-cyan-500/20 blur-[120px] rounded-full pointer-events-none mix-blend-screen" />
            <div className="absolute bottom-[-20%] right-[-20%] w-[500px] h-[500px] bg-purple-500/20 blur-[120px] rounded-full pointer-events-none mix-blend-screen" />

            {/* 테마 토글 */}
            <div className="absolute top-6 right-6">
                <button
                    onClick={toggleTheme}
                    className={cn(
                        "flex items-center gap-2 px-4 py-3",
                        "glass-card rounded-lg transition-all",
                        "hover:neon-border-cyan text-text-primary"
                    )}
                >
                    {theme === 'dark' ? <Sun className="w-5 h-5 neon-icon-orange" /> : <Moon className="w-5 h-5 neon-icon-purple" />}
                </button>
            </div>

            <main className="w-full max-w-md p-8 glass-card rounded-2xl border border-border-default/50 shadow-2xl z-10 relative">
                
                {/* 헤더 */}
                <div className="mb-8">
                    <button onClick={() => navigate(-1)} className="flex items-center text-text-secondary hover:text-cyan-400 transition-colors mb-4">
                        <ArrowLeft className="w-4 h-4 mr-1" /> Back
                    </button>
                    <h1 className="gradient-title text-3xl font-bold mb-2">Create Account</h1>
                    <p className="text-text-secondary text-sm">관리자 계정을 생성합니다.</p>
                </div>

                <form onSubmit={handleJoin} className="space-y-5">
                    {errorMsg && (
                        <div className="p-3 text-sm text-red-500 bg-red-500/10 border border-red-500/20 rounded-lg text-center">
                            {errorMsg}
                        </div>
                    )}

                    {/* 이름 입력 */}
                    <div className="space-y-2">
                        <label className="text-sm font-medium text-text-primary ml-1">Name</label>
                        <div className="relative group">
                            <User className="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-text-secondary group-focus-within:text-cyan-400 transition-colors" />
                            <input
                                type="text"
                                value={name}
                                onChange={(e) => setName(e.target.value)}
                                placeholder="관리자 이름"
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

                    {/* 이메일 입력 */}
                    <div className="space-y-2">
                        <label className="text-sm font-medium text-text-primary ml-1">Mattermost Email</label>
                        <div className="relative group">
                            <Mail className="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-text-secondary group-focus-within:text-cyan-400 transition-colors" />
                            <input
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

                    {/* 비밀번호 입력 */}
                    <div className="space-y-2">
                        <label className="text-sm font-medium text-text-primary ml-1">Password</label>
                        <div className="relative group">
                            <Lock className="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-text-secondary group-focus-within:text-cyan-400 transition-colors" />
                            <input
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

                    {/* 가입 버튼 */}
                    <button
                        type="submit"
                        disabled={isLoading}
                        className={cn(
                            "w-full py-3 px-4 rounded-lg font-bold text-white transition-all mt-8 flex justify-center items-center gap-2",
                            "bg-gradient-to-r from-purple-500 to-pink-600 hover:from-purple-400 hover:to-pink-500", // 로그인과 다른 색상 포인트
                            "shadow-[0_0_20px_rgba(168,85,247,0.3)] hover:shadow-[0_0_30px_rgba(168,85,247,0.5)]",
                            "active:scale-[0.98]",
                            isLoading && "opacity-70 cursor-not-allowed"
                        )}
                    >
                        {isLoading ? <Loader2 className="w-5 h-5 animate-spin" /> : "Sign Up"}
                    </button>
                    
                    {/* 로그인 페이지로 이동 링크 */}
                    <div className="text-center mt-4">
                        <span className="text-text-secondary text-sm">Already have an account? </span>
                        <NavLink to="/" className="text-cyan-400 hover:text-cyan-300 font-semibold text-sm hover:underline">
                            Log In
                        </NavLink>
                    </div>
                </form>
            </main>
        </div>
    )
}