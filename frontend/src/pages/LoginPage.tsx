import { useEffect } from "react";
import { useAuthStore } from "../store/authStore";
import { useLoginForm } from "@/hooks/useLoginForm";
import { AppHeader } from "@/components/layouts/AppHeader";
import LoginForm from "@/components/auth/LoginForm";

const LoginPage = () => {
    const { isAuthenticated, clearAuth } = useAuthStore();
    const { form, onSubmit, isLoading, apiError } = useLoginForm();

    // 로그인 페이지 진입 시 기존 인증 정보 클리어
    useEffect(() => {
        if (isAuthenticated) {
            clearAuth();
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, []);

    return (
        <div className="min-h-screen bg-gray-50">
            <AppHeader />

            <main className="max-w-md mx-auto px-6 py-6">
                <div className="mb-5 animate-fade-in-up">
                    <h2 className="text-heading-2 mb-1">환영합니다! 👋</h2>
                    <p className="text-body-small">
                        편리한 짐 운반 서비스를 시작하세요
                    </p>
                </div>

                <LoginForm
                    form={form}
                    onSubmit={onSubmit}
                    isLoading={isLoading}
                    apiError={apiError}
                />

                <p className="text-center text-sm text-gray-500 mt-6 animate-fade-in-up">
                    가장 낮은 눈높이에서, 가장 높은 서비스를
                </p>
            </main>
        </div>
    );
};

export default LoginPage;
