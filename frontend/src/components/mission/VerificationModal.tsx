import { useState, useEffect } from "react";
import { verifyMission } from "../../api/mission.api";
import { shuffleArray } from "@/utils/array";
import NumpadKeyboard from "@/components/mission/NumpadKeyboard";

interface VerificationModalProps {
    missionId: number;
    onSuccess: () => void;
    onClose?: () => void; // optional로 변경
}

export const VerificationModal = ({
    missionId,
    onSuccess,
    onClose,
}: VerificationModalProps) => {
    const [password, setPassword] = useState("");
    const [isVerifying, setIsVerifying] = useState(false);
    const [error, setError] = useState("");
    const [isVisible, setIsVisible] = useState(false);

    // 숫자 배치를 state로 관리 (틀렸을 때 재생성하기 위해)
    const [shuffledNumbers, setShuffledNumbers] = useState(() =>
        shuffleArray([0, 1, 2, 3, 4, 5, 6, 7, 8, 9]),
    );

    // 모달 진입 애니메이션
    useEffect(() => {
        requestAnimationFrame(() => {
            setIsVisible(true);
        });
    }, []);

    const handleNumberClick = (num: number) => {
        if (password.length < 4) {
            const newPassword = password + num.toString();
            setPassword(newPassword);
            setError("");

            // 4자리 입력 완료 시 자동 인증
            if (newPassword.length === 4) {
                handleVerify(newPassword);
            }
        }
    };

    const handleBackspace = () => {
        setPassword((prev) => prev.slice(0, -1));
        setError("");
    };

    const handleVerify = async (pwd: string) => {
        if (pwd.length !== 4) {
            setError("4자리 비밀번호를 입력해주세요.");
            return;
        }

        try {
            setIsVerifying(true);
            setError("");

            const result = await verifyMission(Number(missionId), Number(pwd));
            if (import.meta.env.DEV) {
                console.log("[VerificationModal] 인증 성공:", result);
            }

            // 인증 성공
            onSuccess();
            onClose?.(); // optional chaining으로 안전하게 호출
        } catch (err) {
            console.error("인증 실패:", err);
            // 비밀번호 불일치 시 에러 메시지와 함께 초기화
            setError("비밀번호가 맞지 않아요\n다시 눌러주세요");
            setPassword("");
            // 번호 배치 다시 섮기
            setShuffledNumbers(shuffleArray([0, 1, 2, 3, 4, 5, 6, 7, 8, 9]));
        } finally {
            setIsVerifying(false);
        }
    };

    const handleClose = () => {
        setIsVisible(false);
        setTimeout(() => onClose?.(), 300); // optional chaining으로 안전하게 호출
    };

    // 키패드 레이아웃: 3x3 그리드 + 마지막 행 (빈칸, 숫자, 백스페이스)
    // shuffledNumbers에서 9개는 3x3에, 마지막 1개는 하단 중앙에
    const gridNumbers = shuffledNumbers.slice(0, 9);
    const lastNumber = shuffledNumbers[9];

    return (
        <div className="fixed inset-0 z-50 flex flex-col">
            {/* 블러 배경 - 전체 화면, 뒷 화면 흐릿하게 보임 */}
            <div
                className={`absolute inset-0 bg-black/20 backdrop-blur-sm transition-opacity duration-300 ${
                    isVisible ? "opacity-100" : "opacity-0"
                }`}
                onClick={handleClose}
            />

            {/* 모달 컨텐츠 - 전체 화면 */}
            <div
                className={`relative z-10 flex-1 flex flex-col bg-white/70 backdrop-blur-sm transition-all duration-300 ${
                    isVisible
                        ? "opacity-100 translate-y-0"
                        : "opacity-0 translate-y-4"
                }`}
            >
                {/* 닫기 버튼 */}
                <button
                    onClick={handleClose}
                    className="absolute top-4 right-4 w-10 h-10 flex items-center justify-center text-gray-400 hover:text-gray-600 transition-colors z-20"
                >
                    <svg
                        className="w-6 h-6"
                        fill="none"
                        viewBox="0 0 24 24"
                        stroke="currentColor"
                    >
                        <path
                            strokeLinecap="round"
                            strokeLinejoin="round"
                            strokeWidth={2}
                            d="M6 18L18 6M6 6l12 12"
                        />
                    </svg>
                </button>

                {/* 상단 영역 - 타이틀과 비밀번호 표시 */}
                <div className="flex-1 flex flex-col items-center justify-center px-6">
                    {/* 타이틀 또는 에러 메시지 - 같은 위치에 표시 */}
                    <div className="h-16 flex items-center justify-center mb-8">
                        {error ? (
                            // 에러 메시지 - 그라디언트 애니메이션
                            <p className="text-center text-lg font-semibold whitespace-pre-line animate-shake animate-gradient-text">
                                {error}
                            </p>
                        ) : password.length === 0 ? (
                            // 초기 상태 - "비밀번호를 눌러주세요" (그라디언트 애니메이션)
                            <h2 className="text-2xl font-bold animate-gradient-text">
                                비밀번호를 눌러주세요
                            </h2>
                        ) : null}
                    </div>

                    {/* 비밀번호 표시 (4개 점) - 파란색 */}
                    <div className="flex justify-center gap-5 mb-6">
                        {[0, 1, 2, 3].map((idx) => (
                            <div
                                key={idx}
                                className={`w-4 h-4 rounded-full transition-all duration-200 ${
                                    password[idx]
                                        ? "bg-[#0064FF] scale-125"
                                        : "border-2 border-[#0064FF] bg-transparent"
                                }`}
                            />
                        ))}
                    </div>

                    {/* 로딩 표시 */}
                    {isVerifying && (
                        <div className="flex justify-center mb-4">
                            <div className="w-6 h-6 border-2 border-[#0064FF] border-t-transparent rounded-full animate-spin" />
                        </div>
                    )}
                </div>

                {/* 하단 영역 - 숫자 키패드 */}
                <NumpadKeyboard
                    gridNumbers={gridNumbers}
                    lastNumber={lastNumber}
                    onNumberClick={handleNumberClick}
                    onBackspace={handleBackspace}
                    disabled={isVerifying}
                />
            </div>
        </div>
    );
};
