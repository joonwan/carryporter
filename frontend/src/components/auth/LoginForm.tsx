import type { UseFormReturn } from "react-hook-form";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { PasswordInputField } from "@/components/auth/PasswordInputField";
import { TermsCheckbox } from "@/components/auth/TermsCheckbox";
import type { SendCodeFormData } from "@/utils/validation";

interface LoginFormProps {
    form: UseFormReturn<SendCodeFormData>;
    onSubmit: (data: SendCodeFormData) => void;
    isLoading: boolean;
    apiError: string;
}

const LoginForm = ({ form, onSubmit, isLoading, apiError }: LoginFormProps) => {
    const {
        register,
        handleSubmit,
        control,
        watch,
        formState: { errors, isValid },
    } = form;

    const password = watch("password");
    const passwordConfirm = watch("passwordConfirm");
    const agreeTerms = watch("agreeTerms");
    const agreePrivacy = watch("agreePrivacy");

    const isFormValid = isValid && agreeTerms && agreePrivacy;

    return (
        <div className="bg-white rounded-2xl shadow-sm p-6 animate-fade-in-up">
            <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
                {/* 폼 제목 */}
                <div className="text-center space-y-2">
                    <h2 className="text-xl font-bold text-gray-900">로그인</h2>
                    <p className="text-sm text-gray-600">
                        CARRY PORTER 이용을 위해 정보를 입력해주세요
                    </p>
                </div>

                {/* 입력 필드 */}
                <div className="space-y-4">
                    {/* 이메일 */}
                    <div className="space-y-2">
                        <label
                            htmlFor="email"
                            className="block text-sm font-medium text-gray-700"
                        >
                            Mattermost 이메일
                        </label>
                        <Input
                            id="email"
                            type="email"
                            placeholder="example@email.com"
                            {...register("email")}
                            className={errors.email ? "border-red-500" : ""}
                        />
                        {errors.email && (
                            <p className="text-sm text-red-500">
                                {errors.email.message}
                            </p>
                        )}
                    </div>

                    {/* 비밀번호 */}
                    <div className="space-y-2">
                        <label className="block text-sm font-medium text-gray-700">
                            비밀번호 (4자리 숫자)
                        </label>
                        <PasswordInputField
                            register={register}
                            errors={errors}
                            name="password"
                            label=""
                            placeholder="4자리 숫자"
                        />
                    </div>

                    {/* 비밀번호 확인 */}
                    <div className="space-y-2">
                        <label className="block text-sm font-medium text-gray-700">
                            비밀번호 확인
                        </label>
                        <PasswordInputField
                            register={register}
                            errors={errors}
                            name="passwordConfirm"
                            label=""
                            placeholder="4자리 숫자"
                        />
                        {passwordConfirm &&
                            password &&
                            passwordConfirm === password &&
                            !errors.passwordConfirm && (
                                <p className="text-sm text-green-600">
                                    ✓ 비밀번호가 일치합니다
                                </p>
                            )}
                    </div>

                    {/* 약관 동의 */}
                    <div className="space-y-3 p-3 bg-sub-orange/5 border border-sub-orange/15 rounded-xl">
                        <TermsCheckbox
                            control={control}
                            name="agreeTerms"
                            label="보관 정책에 동의합니다 (필수)"
                            errors={errors}
                        />
                        <TermsCheckbox
                            control={control}
                            name="agreePrivacy"
                            label="서비스 이용약관에 동의합니다 (필수)"
                            errors={errors}
                        />
                    </div>

                    {/* 약관 설명 */}
                    <div className="text-[10px] text-gray-500 space-y-0.5">
                        <p>· 보관 정책: 짐 보관 안전 및 책임 범위</p>
                        <p>· 이용약관: 로봇 호출 서비스 준수사항</p>
                    </div>
                </div>

                {/* API 에러 */}
                {apiError && (
                    <div className="bg-red-50 border border-red-200 rounded-xl p-4">
                        <div className="flex items-center gap-2">
                            <svg
                                className="w-5 h-5 text-red-500"
                                fill="none"
                                viewBox="0 0 24 24"
                                stroke="currentColor"
                            >
                                <path
                                    strokeLinecap="round"
                                    strokeLinejoin="round"
                                    strokeWidth={2}
                                    d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"
                                />
                            </svg>
                            <p className="text-sm text-red-600">{apiError}</p>
                        </div>
                    </div>
                )}

                {/* 제출 버튼 */}
                <Button
                    type="submit"
                    disabled={!isFormValid || isLoading}
                    className="w-full h-12 text-base font-semibold bg-toss-blue-500 hover:bg-toss-blue-600 text-white disabled:opacity-40 rounded-xl"
                >
                    {isLoading ? "처리 중..." : "로그인"}
                </Button>
            </form>
        </div>
    );
};

export default LoginForm;
