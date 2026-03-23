interface ProgressBarProps {
    currentStep: number;
    totalSteps: number;
}

/**
 * Progress Bar 컴포넌트
 * 미션 플로우 진행 상황을 시각적으로 표시 (shimmer 흐르는 효과 포함)
 */
export function ProgressBar({ currentStep, totalSteps }: ProgressBarProps) {
    const progress = (currentStep / totalSteps) * 100;
    const isInProgress = progress > 0 && progress < 100;

    return (
        <div className="w-full">
            {/* Progress 텍스트 */}
            <div className="flex justify-between text-sm text-gray-600 mb-2">
                <span className="font-medium">진행 중</span>
                <span className="font-semibold">{Math.round(progress)}%</span>
            </div>

            {/* Progress bar */}
            <div className="relative w-full h-3 bg-gray-200 rounded-full overflow-hidden">
                <div
                    className="absolute top-0 left-0 h-full bg-toss-blue-500 transition-all duration-500 ease-out"
                    style={{ width: `${progress}%` }}
                >
                    {/* Shimmer 흐르는 효과 */}
                    {isInProgress && (
                        <div className="absolute inset-0 bg-gradient-to-r from-transparent via-white/30 to-transparent animate-shimmer-flow" />
                    )}
                </div>
            </div>
        </div>
    );
}
