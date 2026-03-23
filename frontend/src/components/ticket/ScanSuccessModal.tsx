import { useEffect, useState } from "react";
import { Button } from "@/components/ui/button";

interface ScanSuccessModalProps {
    isOpen: boolean;
    onConfirm: () => void; // "등록" 버튼 클릭 시 실행
}

const ScanSuccessModal = ({ isOpen, onConfirm }: ScanSuccessModalProps) => {
    const [animate, setAnimate] = useState(false);

    // 모달이 열릴 때 애니메이션 시작
    useEffect(() => {
        if (!isOpen) {
            setAnimate(false);
            return;
        }
        const timer = setTimeout(() => {
            setAnimate(true);
        }, 100);
        return () => {
            clearTimeout(timer);
        };
    }, [isOpen]);

    if (!isOpen) return null;

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center">
            {/* 배경 오버레이 (블러 효과) */}
            <div
                className="absolute inset-0 bg-black/40 backdrop-blur-sm animate-fade-in"
            />

            {/* 모달 컨텐츠 */}
            <div
                className={`relative bg-white rounded-3xl p-8 max-w-sm w-[90%] mx-4 shadow-2xl transition-all duration-500 ${
                    animate
                        ? "opacity-100 scale-100 translate-y-0"
                        : "opacity-0 scale-95 translate-y-4"
                }`}
            >
                {/* 체크마크 아이콘 (Toss 스타일) */}
                <div className="flex justify-center mb-8">
                    <div
                        className={`relative w-24 h-24 transition-all duration-500 delay-200 ${
                            animate
                                ? "scale-100 opacity-100"
                                : "scale-0 opacity-0"
                        }`}
                    >
                        {/* 외부 원형 테두리 */}
                        <div className="absolute inset-0 rounded-full border-4 border-toss-blue-500/20" />

                        {/* 내부 채워진 원 */}
                        <div className="absolute inset-2 rounded-full bg-gradient-to-br from-toss-blue-500 to-toss-blue-light flex items-center justify-center shadow-lg shadow-blue-500/30">
                            <svg
                                className={`w-10 h-10 text-white transition-all duration-500 delay-300 ${
                                    animate
                                        ? "scale-100 opacity-100"
                                        : "scale-0 opacity-0"
                                }`}
                                fill="none"
                                viewBox="0 0 24 24"
                                stroke="currentColor"
                            >
                                <path
                                    strokeLinecap="round"
                                    strokeLinejoin="round"
                                    strokeWidth={3}
                                    d="M5 13l4 4L19 7"
                                />
                            </svg>
                        </div>

                        {/* 반짝임 효과 */}
                        <div
                            className={`absolute -top-1 -right-1 w-4 h-4 transition-all duration-500 delay-500 ${
                                animate
                                    ? "scale-100 opacity-100"
                                    : "scale-0 opacity-0"
                            }`}
                        >
                            <svg
                                viewBox="0 0 24 24"
                                fill="none"
                                className="w-full h-full"
                            >
                                <path
                                    d="M12 2L14 10L22 12L14 14L12 22L10 14L2 12L10 10L12 2Z"
                                    fill="#FFD600"
                                />
                            </svg>
                        </div>
                    </div>
                </div>

                {/* 메시지 */}
                <div className="text-center mb-8">
                    <h2 className="text-2xl font-bold text-gray-900 mb-3">
                        스캔이 완료되었습니다
                    </h2>
                    <p className="text-gray-500 leading-relaxed">
                        티켓 정보가 성공적으로 등록되었습니다.
                        <br />
                        메인 화면에서 확인해보세요.
                    </p>
                </div>

                {/* 등록 버튼 (Toss 스타일) */}
                <Button
                    onClick={onConfirm}
                    className="w-full h-14 text-lg font-semibold bg-toss-blue-500 hover:bg-toss-blue-600 text-white rounded-xl shadow-lg shadow-blue-500/30 transition-all duration-200 active:scale-[0.98]"
                >
                    등록
                </Button>
            </div>
        </div>
    );
};

export default ScanSuccessModal;
