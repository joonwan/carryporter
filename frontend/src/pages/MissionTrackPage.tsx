import { useMemo } from "react";
import { useNavigate } from "react-router-dom";
import { useMissionStore } from "../store/missionStore";
import { useSSEStore } from "../store/sseStore";
import { Button } from "@/components/ui/button";
import { AppHeader } from "@/components/layouts/AppHeader";
import { useMissionFlow } from "@/hooks/useMissionFlow";
import { ConnectionStatusBadge } from "@/components/features/mission/ConnectionStatusBadge";
import { MissionTimeline } from "@/components/features/mission/MissionTimeline";
import { RobotInfoCard } from "@/components/features/mission/RobotInfoCard";
import { MissionModals } from "@/components/features/mission/MissionModals";
import {
    calculateProgressStep,
    getStatusMessage,
} from "@/domain/mission/stateMachine";

const MissionTrackPage = () => {
    const navigate = useNavigate();
    const { currentMission, clearMission } = useMissionStore();

    const { isConnected, connectionQuality, reconnectAttempts } = useSSEStore();

    const { flowStep, handleVerificationSuccess, handleReturnSuccess } =
        useMissionFlow(currentMission);

    // 미션 완료
    const handleComplete = () => {
        clearMission();
        navigate("/home");
    };

    // ✅ 진행률 계산 (도메인 로직 사용)
    const progressStep = useMemo(() => {
        if (!currentMission) return 0;
        return calculateProgressStep(currentMission.status);
    }, [currentMission]);

    // 미션 정보가 없으면 홈으로
    if (!currentMission) {
        return (
            <div className="min-h-screen bg-gray-50 p-6 flex flex-col items-center justify-center">
                <div className="text-center">
                    <div className="w-20 h-20 mx-auto mb-4 bg-gray-100 rounded-full flex items-center justify-center">
                        <svg
                            className="w-10 h-10 text-gray-400"
                            fill="none"
                            viewBox="0 0 24 24"
                            stroke="currentColor"
                        >
                            <path
                                strokeLinecap="round"
                                strokeLinejoin="round"
                                strokeWidth={2}
                                d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"
                            />
                        </svg>
                    </div>
                    <p className="text-gray-600 mb-6">미션 정보가 없습니다.</p>
                    <Button
                        onClick={() => navigate("/home")}
                        className="bg-toss-blue-500 text-white"
                    >
                        홈으로 돌아가기
                    </Button>
                </div>
            </div>
        );
    }

    const status = currentMission.status;

    return (
        <div className="min-h-screen bg-gray-50">
            {/* 헤더 */}
            <AppHeader
                rightElement={
                    <ConnectionStatusBadge
                        isConnected={isConnected}
                        connectionQuality={connectionQuality}
                        reconnectAttempts={reconnectAttempts}
                    />
                }
            />

            {/* 메인 컨텐츠 */}
            <main className="max-w-md mx-auto px-6 py-6">
                {/* 상태 메시지 */}
                <div className="mb-6 animate-fade-in-up">
                    <h2 className="text-heading-1 mb-1">미션 진행중 🚀</h2>
                    <p className="text-body-small">
                        {getStatusMessage(status)}
                    </p>
                </div>

                {/* 타임라인 카드 */}
                <MissionTimeline status={status} progressStep={progressStep} />

                {/* 로봇 정보 카드 */}
                {currentMission?.robotCode && (
                    <div className="mb-6">
                        <RobotInfoCard robotCode={currentMission.robotCode} />
                    </div>
                )}

                {/* 완료 버튼 (FINISHED 상태) */}
                {status === "FINISHED" && (
                    <Button
                        onClick={handleComplete}
                        className="w-full h-14 text-lg font-semibold bg-toss-blue-500 hover:bg-toss-blue-600 text-white"
                    >
                        완료
                    </Button>
                )}
            </main>

            {/* 모달들 - 배경이 흐릿하게 보임 */}
            <MissionModals
                currentMission={currentMission}
                flowStep={flowStep}
                onVerificationSuccess={handleVerificationSuccess}
                onReturnSuccess={handleReturnSuccess}
            />
        </div>
    );
};

export default MissionTrackPage;
