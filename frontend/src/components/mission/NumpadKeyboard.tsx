interface NumpadKeyboardProps {
    /** 3x3 그리드에 표시할 숫자 9개 */
    gridNumbers: number[];
    /** 마지막 행 중앙에 표시할 숫자 1개 */
    lastNumber: number;
    /** 숫자 클릭 핸들러 */
    onNumberClick: (num: number) => void;
    /** 백스페이스 핸들러 */
    onBackspace: () => void;
    /** 비활성화 여부 */
    disabled?: boolean;
}

/**
 * 랜덤 배치 숫자 키패드 — VerificationModal에서 분리
 */
const NumpadKeyboard = ({
    gridNumbers,
    lastNumber,
    onNumberClick,
    onBackspace,
    disabled = false,
}: NumpadKeyboardProps) => {
    const buttonClass =
        "h-16 rounded-2xl text-3xl font-normal transition-all duration-150 active:scale-95 disabled:opacity-50 bg-transparent text-[#0064FF] hover:bg-[#0064FF]/10 active:bg-[#0064FF]/20";

    return (
        <div className="px-6 pb-10">
            <div className="grid grid-cols-3 gap-4 max-w-xs mx-auto">
                {gridNumbers.map((num, index) => (
                    <button
                        key={`num-${index}`}
                        type="button"
                        disabled={disabled}
                        onClick={() => onNumberClick(num)}
                        className={buttonClass}
                    >
                        {num}
                    </button>
                ))}

                {/* 네 번째 줄: 빈 공간, 마지막 숫자, 백스페이스 */}
                <div className="h-16" />
                <button
                    type="button"
                    disabled={disabled}
                    onClick={() => onNumberClick(lastNumber)}
                    className={buttonClass}
                >
                    {lastNumber}
                </button>
                <button
                    type="button"
                    disabled={disabled}
                    onClick={onBackspace}
                    className="h-16 rounded-2xl text-2xl font-normal transition-all duration-150 active:scale-95 disabled:opacity-50 bg-transparent text-[#0064FF] hover:bg-[#0064FF]/10 active:bg-[#0064FF]/20"
                >
                    ←
                </button>
            </div>
        </div>
    );
};

export default NumpadKeyboard;
