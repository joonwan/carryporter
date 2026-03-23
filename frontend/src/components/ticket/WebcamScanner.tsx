import { useRef, useState, useCallback } from "react";
import { useNavigate } from "react-router-dom";
import Webcam from "react-webcam";
import { Button } from "@/components/ui/button";
import CameraErrorView from "@/components/ticket/CameraErrorView";

interface WebcamScannerProps {
    onCapture: (imageFile: File) => void; // 캡처된 이미지를 전달하는 콜백
    isScanning?: boolean; // 스캔 진행 중 여부
}

const WebcamScanner = ({
    onCapture,
    isScanning = false,
}: WebcamScannerProps) => {
    const webcamRef = useRef<Webcam>(null);
    const navigate = useNavigate();
    const [hasError, setHasError] = useState(false);

    // 웹캠 설정 (PC와 모바일 모두 호환)
    const videoConstraints = {
        facingMode: { ideal: "environment" }, // 후면 카메라 선호, 없으면 전면 카메라
        width: { min: 640, ideal: 1920, max: 1920 }, // 유연한 해상도
        height: { min: 480, ideal: 1080, max: 1080 },
    };

    // 웹캠 에러 핸들러
    const handleUserMediaError = useCallback((error: string | DOMException) => {
        console.error("웹캠 접근 에러:", error);
        setHasError(true);
    }, []);

    // 스캔하기 버튼 클릭 핸들러
    const handleScan = useCallback(() => {
        if (webcamRef.current) {
            // 스크린샷 캡처 (base64 형식)
            const imageSrc = webcamRef.current.getScreenshot();

            if (imageSrc) {
                // base64를 File 객체로 변환
                const base64Data = imageSrc.split(",")[1];
                const binaryString = atob(base64Data);
                const bytes = new Uint8Array(binaryString.length);

                for (let i = 0; i < binaryString.length; i++) {
                    bytes[i] = binaryString.charCodeAt(i);
                }

                const blob = new Blob([bytes], { type: "image/jpeg" });
                const file = new File([blob], "ticket.jpg", {
                    type: "image/jpeg",
                });

                // 부모 컴포넌트로 전달
                onCapture(file);
            }
        }
    }, [onCapture]);

    // 닫기 버튼 핸들러 (홈 화면으로 직접 이동)
    const handleClose = () => {
        navigate("/home");
    };

    // 에러 상태 UI
    if (hasError) {
        return (
            <CameraErrorView
                onRetry={() => setHasError(false)}
                onClose={handleClose}
            />
        );
    }

    return (
        <div className="fixed inset-0 bg-black overflow-hidden">
            {/* 웹캠 스트림 (전체 화면) */}
            <Webcam
                ref={webcamRef}
                audio={false}
                screenshotFormat="image/jpeg"
                screenshotQuality={0.92}
                videoConstraints={videoConstraints}
                onUserMediaError={handleUserMediaError}
                className="absolute inset-0 w-full h-full object-cover"
            />

            {/* 상단 헤더 영역 */}
            <div className="absolute top-0 left-0 right-0 z-20 pt-safe">
                <div className="flex items-center justify-end px-6 py-6">
                    {/* 닫기 버튼 */}
                    <button
                        onClick={handleClose}
                        className="w-10 h-10 flex items-center justify-center rounded-full bg-black/30 backdrop-blur-sm transition-all hover:bg-black/50"
                    >
                        <svg
                            className="w-6 h-6 text-white"
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
                </div>
            </div>

            {/* 안내 타이틀 */}
            <div className="absolute top-24 left-0 right-0 z-20 text-center px-6 animate-fade-in-up">
                <h1 className="text-white text-2xl font-bold mb-2 drop-shadow-lg">
                    탑승권을 촬영해주세요
                </h1>
                <p className="text-white/80 text-sm">
                    탑승권이 화면에 잘 보이도록 조정해주세요
                </p>
            </div>

            {/* 하단 버튼 영역 */}
            <div className="absolute bottom-0 left-0 right-0 z-20 pb-safe">
                <div className="px-6 pb-8 pt-6">
                    {/* 보딩패스 가이드 텍스트 */}
                    <p className="text-white/60 text-xs text-center mb-4">
                        💡 탑승권 전체가 화면에 선명하게 보이도록 해주세요
                    </p>

                    <Button
                        onClick={handleScan}
                        disabled={isScanning}
                        className="w-full h-12 text-base font-semibold bg-toss-blue-500 hover:bg-toss-blue-600 text-white disabled:bg-toss-blue-500/50 rounded-xl shadow-lg shadow-blue-500/20 transition-all duration-200 active:scale-[0.98]"
                    >
                        {isScanning ? (
                            <div className="flex items-center gap-3">
                                <div className="w-5 h-5 border-2 border-white/30 border-t-white rounded-full animate-spin" />
                                스캔 중...
                            </div>
                        ) : (
                            "촬영하기"
                        )}
                    </Button>
                </div>
            </div>
        </div>
    );
};

export default WebcamScanner;
