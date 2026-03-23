import { useState, useMemo, useEffect } from "react";
import { useNavigate, useLocation } from "react-router-dom";
import { Button } from "@/components/ui/button";
import { login } from "../api/auth.api";
import { useAuthStore } from "../store/authStore";
import { AppHeader } from "@/components/layouts/AppHeader";
import { shuffleArray, generateFakeCodes } from "@/utils/array";

interface LocationState {
    email: string;
    code: number; // 실제 CODE (백엔드에서 전달받은 값)
}

const CodeVerificationPage = () => {
    const navigate = useNavigate();
    const location = useLocation();
    const { login: loginStore, isAuthenticated, clearAuth } = useAuthStore();

    const state = location.state as LocationState | null;
    const [selectedCode, setSelectedCode] = useState<number | null>(null);
    const [isLoading, setIsLoading] = useState(false);
    const [apiError, setApiError] = useState("");

    // state에서 값 추출 (없으면 기본값)
    const email = state?.email ?? "";
    const correctCode = state?.code ?? 0;
    const hasValidState = !!(state && state.email && state.code !== undefined);

    // CODE 옵션 생성 (모든 훅은 조건부 반환 전에 호출되어야 함)
    const codeOptions = useMemo(() => {
        if (!hasValidState) return [];
        const fakeCodes = generateFakeCodes(correctCode, 2);
        return shuffleArray([correctCode, ...fakeCodes]);
    }, [correctCode, hasValidState]);

    // 로그인 페이지 진입 시 기존 인증 정보 클리어
    // (뒤로가기로 왔을 때 처음부터 다시 시작하도록)
    useEffect(() => {
        if (isAuthenticated) {
            // 이미 로그인된 상태에서 이 페이지에 왔다면 로그아웃 후 로그인 페이지로
            clearAuth();
            navigate("/login", { replace: true });
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, []); // 마운트 시 1회만 실행

    // state가 없으면 로그인 페이지로 리다이렉트
    useEffect(() => {
        if (!hasValidState) {
            navigate("/login", { replace: true });
        }
    }, [hasValidState, navigate]);

    // state가 없으면 렌더링하지 않음
    if (!hasValidState) {
        return null;
    }

    const handleCodeSelect = (code: number) => {
        setSelectedCode(code);
        setApiError(""); // 에러 초기화
    };

    const handleSubmit = async () => {
        if (selectedCode === null) {
            setApiError("CODE 번호를 선택해주세요");
            return;
        }

        // 잘못된 CODE 선택 시 에러 표시 (프론트에서 바로 체크)
        if (selectedCode !== correctCode) {
            setApiError("잘못된 CODE입니다. mattermost 메시지를 확인해주세요.");
            return;
        }

        try {
            setIsLoading(true);
            setApiError("");

            // 디버깅: 전송할 데이터 확인
            const requestData = {
                email,
                code: selectedCode,
            };
            if (import.meta.env.DEV)
                console.log("=== CODE 인증 요청 데이터 ===");
            if (import.meta.env.DEV) console.log("Email:", email);
            if (import.meta.env.DEV)
                console.log(
                    "Selected Code:",
                    selectedCode,
                    "(type:",
                    typeof selectedCode,
                    ")",
                );
            if (import.meta.env.DEV)
                console.log(
                    "Correct Code:",
                    correctCode,
                    "(type:",
                    typeof correctCode,
                    ")",
                );
            if (import.meta.env.DEV) console.log("Request Data:", requestData);

            // CODE 인증 API 호출 (이메일 + 선택한 CODE)
            // refreshToken은 백엔드가 httpOnly 쿠키로 설정하므로 응답 body에서 처리 불필요
            const response = await login(requestData);

            // Zustand 스토어에 accessToken 저장
            loginStore(response.accessToken, {
                id: "", // 토큰에서 추출 또는 임시값
                email,
                role: "USER",
            });

            // 티켓 스캔 페이지로 이동
            navigate("/ticket/scan");
        } catch (error: unknown) {
            console.error("Login error:", error);
            const axiosErr = error as {
                response?: { data?: { message?: string } };
            };
            setApiError(
                axiosErr.response?.data?.message ||
                    "로그인에 실패했습니다. 다시 시도해주세요.",
            );
        } finally {
            setIsLoading(false);
        }
    };

    return (
        <div className="min-h-screen bg-gray-50">
            {/* 헤더 */}
            <AppHeader />

            {/* 메인 컨텐츠 */}
            <main className="max-w-md mx-auto px-6 py-6">
                {/* 제목 */}
                <div className="mb-5 animate-fade-in-up">
                    <h2 className="text-heading-2 mb-1">같은 번호 선택 🔢</h2>
                    <p className="text-body-small">
                        편리한 인증을 위해 번호를 선택해주세요
                    </p>
                </div>

                {/* CODE 선택 카드 */}
                <div className="bg-white rounded-2xl shadow-sm p-6 animate-fade-in-up">
                    <div className="text-center space-y-2 mb-4">
                        <h3 className="text-xl font-bold text-gray-900">
                            인증 코드
                        </h3>
                        <p className="text-sm text-gray-600">
                            Mattermost에서 받은 번호를 선택해주세요
                        </p>
                    </div>

                    {/* CODE 버튼들 */}
                    <div className="space-y-3 mb-5">
                        {codeOptions.map((code) => (
                            <button
                                key={code}
                                onClick={() => handleCodeSelect(code)}
                                className={`
                  w-full h-14 text-2xl font-bold rounded-xl border-2 transition-all
                  ${
                      selectedCode === code
                          ? "bg-gradient-to-r from-toss-blue-500 to-toss-blue-400 border-toss-blue-500 text-white shadow-md shadow-toss-blue-500/25"
                          : "bg-gray-50 border-gray-200 text-gray-900 hover:bg-gray-100 hover:border-gray-300"
                  }
                `}
                            >
                                {code}
                            </button>
                        ))}
                    </div>

                    {/* API 에러 메시지 */}
                    {apiError && (
                        <div className="bg-red-50 border border-red-200 rounded-xl p-4 mb-4">
                            <p className="text-sm text-red-600">{apiError}</p>
                        </div>
                    )}

                    {/* 로그인 버튼 */}
                    <Button
                        onClick={handleSubmit}
                        disabled={isLoading || selectedCode === null}
                        className="w-full h-12 text-base font-semibold bg-toss-blue-500 hover:bg-toss-blue-600 text-white disabled:opacity-40 rounded-xl"
                    >
                        {isLoading ? "인증 중..." : "로그인"}
                    </Button>
                </div>
            </main>
        </div>
    );
};

export default CodeVerificationPage;
