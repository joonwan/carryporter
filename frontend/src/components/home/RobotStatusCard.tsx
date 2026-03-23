const RobotStatusCard = () => (
    <div
        className="bg-white rounded-2xl p-4 shadow-sm mb-4 animate-fade-in-up"
        style={{ animationDelay: "200ms" }}
    >
        <div className="flex items-center justify-between">
            <div className="flex items-center gap-2">
                <svg
                    className="w-5 h-5 text-sub-orange"
                    fill="none"
                    viewBox="0 0 24 24"
                    stroke="currentColor"
                >
                    <path
                        strokeLinecap="round"
                        strokeLinejoin="round"
                        strokeWidth={2}
                        d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z"
                    />
                </svg>
                <h3 className="text-gray-900 font-bold text-base">로봇 현황</h3>
            </div>
            <div className="flex items-center gap-1">
                <span className="text-lg font-bold text-sub-orange">12</span>
                <span className="text-sm text-gray-600">대 가용</span>
            </div>
        </div>
    </div>
);

export default RobotStatusCard;
