import type { CurrentLocker } from "@/types/locker.types";

interface LockerStatusCardProps {
    currentLocker: CurrentLocker | null;
    isLoading: boolean;
    error: string | null;
}

const LockerStatusCard = ({
    currentLocker,
    isLoading,
    error,
}: LockerStatusCardProps) => {
    if (isLoading) {
        return (
            <div
                className="bg-white rounded-2xl p-4 shadow-sm mb-4 animate-fade-in-up"
                style={{ animationDelay: "300ms" }}
            >
                <div className="flex items-center justify-center py-8">
                    <div className="animate-spin w-8 h-8 border-4 border-toss-blue-500 border-t-transparent rounded-full" />
                    <span className="ml-3 text-gray-600 text-sm">
                        사물함 정보를 불러오는 중...
                    </span>
                </div>
            </div>
        );
    }

    if (error) {
        return (
            <div
                className="bg-white rounded-2xl p-4 shadow-sm mb-4 animate-fade-in-up"
                style={{ animationDelay: "300ms" }}
            >
                <div className="text-center py-8">
                    <p className="text-red-500 text-sm">{error}</p>
                    <button
                        onClick={() => window.location.reload()}
                        className="mt-4 text-toss-blue-500 text-sm underline"
                    >
                        다시 시도
                    </button>
                </div>
            </div>
        );
    }

    if (!currentLocker) return null;

    return (
        <div
            className="bg-white rounded-2xl p-4 shadow-sm mb-4 animate-fade-in-up"
            style={{ animationDelay: "300ms" }}
        >
            <h3 className="text-gray-900 font-bold text-sm mb-3 flex items-center gap-2">
                <svg
                    className="w-5 h-5 text-toss-green"
                    fill="none"
                    viewBox="0 0 24 24"
                    stroke="currentColor"
                >
                    <path
                        strokeLinecap="round"
                        strokeLinejoin="round"
                        strokeWidth={2}
                        d="M20 7l-8-4-8 4m16 0l-8 4m8-4v10l-8 4m0-10L4 7m8 4v10M4 7v10l8 4"
                    />
                </svg>
                보관 중인 짐
                <span className="ml-auto bg-toss-green text-white px-2 py-0.5 rounded-full text-xs font-semibold">
                    보관 중
                </span>
            </h3>

            <div className="bg-gray-50 rounded-xl p-3 flex items-center gap-3">
                <div className="w-10 h-10 bg-toss-green rounded-xl flex items-center justify-center flex-shrink-0">
                    <svg
                        className="w-5 h-5 text-white"
                        fill="none"
                        viewBox="0 0 24 24"
                        stroke="currentColor"
                    >
                        <path
                            strokeLinecap="round"
                            strokeLinejoin="round"
                            strokeWidth={2}
                            d="M5 8h14M5 8a2 2 0 110-4h14a2 2 0 110 4M5 8v10a2 2 0 002 2h10a2 2 0 002-2V8m-9 4h4"
                        />
                    </svg>
                </div>
                <div className="flex-1">
                    <p className="text-gray-900 font-semibold text-sm">
                        {currentLocker.lockerCode}
                    </p>
                    <p className="text-gray-600 text-xs">
                        {new Date(currentLocker.updatedAt).toLocaleTimeString(
                            "ko-KR",
                            {
                                hour: "2-digit",
                                minute: "2-digit",
                            },
                        )}
                    </p>
                </div>
            </div>
        </div>
    );
};

export default LockerStatusCard;
