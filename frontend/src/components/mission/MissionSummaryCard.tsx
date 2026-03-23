interface MissionSummaryCardProps {
    locationName: string;
}

const MissionSummaryCard = ({ locationName }: MissionSummaryCardProps) => (
    <div className="bg-white rounded-2xl p-5 shadow-sm animate-fade-in-up">
        <h3 className="text-gray-900 font-bold text-base mb-3 flex items-center gap-2">
            <svg
                className="w-5 h-5 text-sub-cyan"
                fill="none"
                viewBox="0 0 24 24"
                stroke="currentColor"
            >
                <path
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    strokeWidth={2}
                    d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2"
                />
            </svg>
            선택 요약
        </h3>
        <div className="space-y-2">
            <div className="flex items-center justify-between bg-gray-50 rounded-xl p-3">
                <span className="text-sm text-gray-600">픽업 위치</span>
                <span className="text-sm font-semibold text-gray-900">
                    {locationName}
                </span>
            </div>
            <div className="flex items-center justify-between bg-gray-50 rounded-xl p-3">
                <span className="text-sm text-gray-600">보관 위치</span>
                <span className="text-sm font-semibold text-gray-900">
                    중앙 사물함
                </span>
            </div>
        </div>
    </div>
);

export default MissionSummaryCard;
