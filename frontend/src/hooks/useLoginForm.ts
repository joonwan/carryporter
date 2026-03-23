import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { useNavigate } from "react-router-dom";
import { sendCode } from "@/api/auth.api";
import { sendCodeSchema, type SendCodeFormData } from "@/utils/validation";

export const useLoginForm = () => {
    const navigate = useNavigate();
    const [isLoading, setIsLoading] = useState(false);
    const [apiError, setApiError] = useState("");

    const form = useForm<SendCodeFormData>({
        resolver: zodResolver(sendCodeSchema),
        mode: "onChange",
    });

    const onSubmit = async (data: SendCodeFormData) => {
        try {
            setIsLoading(true);
            setApiError("");

            // 인증번호 발송 API 호출
            const response = await sendCode({
                email: data.email,
                password: parseInt(data.password, 10),
            });

            if (import.meta.env.DEV)
                console.log("=== 1단계 인증번호 발송 성공 ===");
            if (import.meta.env.DEV) console.log("응답 데이터:", response);

            // CODE 선택 페이지로 이동
            navigate("/login/verify", {
                state: {
                    email: data.email,
                    code: response.code,
                },
            });
        } catch (error: unknown) {
            console.error("Send code error:", error);
            const axiosErr = error as {
                response?: { data?: { message?: string } };
            };
            setApiError(
                axiosErr.response?.data?.message ||
                    "인증번호 발송에 실패했습니다. 다시 시도해주세요.",
            );
        } finally {
            setIsLoading(false);
        }
    };

    return { form, onSubmit, isLoading, apiError };
};
