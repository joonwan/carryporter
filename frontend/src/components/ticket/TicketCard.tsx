import { formatCityName, formatTime } from "../../utils/imageUtils";
import type { TicketInfo, TicketCardVariant } from "../../types/ticket.types";
import { cn } from "@/lib/utils";
import { useTiltEffect } from "@/hooks/useTiltEffect";

interface TicketCardProps {
    ticket: TicketInfo;
    variant?: TicketCardVariant; // 'compact' (메인 화면) 또는 'detailed' (상세 화면)
    onClick?: () => void;
}

const TicketCard = ({
    ticket,
    variant = "compact",
    onClick,
}: TicketCardProps) => {
    const isCompact = variant === "compact";
    const { tiltStyle, handleMouseMove, handleMouseLeave } = useTiltEffect();

    return (
        <div
            className={cn(
                "relative overflow-visible rounded-2xl active:scale-[0.98]",
                onClick && "cursor-pointer",
            )}
            style={{
                ...tiltStyle,
                transition: "transform 0.3s ease-out, box-shadow 0.3s ease-out",
                boxShadow: tiltStyle.transform
                    ? "0 25px 50px -12px rgba(102, 126, 234, 0.5)"
                    : "0 10px 30px -5px rgba(0, 0, 0, 0.2)",
            }}
            onMouseMove={handleMouseMove}
            onMouseLeave={handleMouseLeave}
            onClick={onClick}
        >
            {/* 프리미엄 실버/화이트 배경 - 진주빛 메탈릭 */}
            <div
                className="absolute inset-0 rounded-2xl"
                style={{
                    background:
                        "linear-gradient(135deg, #9CA3AF 0%, #D1D5DB 25%, #F3F4F6 50%, #DBEAFE 75%, #BFDBFE 100%)",
                    animation: "holographic-shift 8s ease-in-out infinite",
                }}
            />

            {/* 장식용 원형 - 기존 유지 */}
            <div className="absolute -top-20 -right-20 w-48 h-48 bg-white/10 rounded-full blur-2xl" />
            <div className="absolute -bottom-16 -left-16 w-40 h-40 bg-white/10 rounded-full blur-xl" />

            {/* Shimmer 효과 - 반짝이는 빛 */}
            <div className="absolute inset-0 overflow-hidden pointer-events-none rounded-2xl">
                <div
                    style={{
                        position: "absolute",
                        inset: 0,
                        background:
                            "linear-gradient(90deg, transparent 0%, rgba(255, 255, 255, 0.4) 30%, rgba(255, 255, 255, 0.7) 50%, rgba(255, 255, 255, 0.4) 70%, transparent 100%)",
                        width: "60%",
                        height: "200%",
                        top: "-50%",
                        opacity: 0.3,
                        animation: "shimmer-hologram 3s ease-in-out infinite",
                    }}
                />
            </div>

            {/* Float 애니메이션 */}
            <div
                className="absolute inset-0 pointer-events-none"
                style={{ animation: "subtle-float 4s ease-in-out infinite" }}
            />

            {/* 메탈릭 오버레이 */}
            <div className="absolute inset-0 bg-gradient-to-br from-white/30 via-transparent to-white/20 pointer-events-none rounded-2xl" />

            {/* Edge highlight - 테두리 빛 */}
            <div className="absolute inset-0 border-2 border-white/40 rounded-2xl pointer-events-none" />

            {/* 카드 컨텐츠 */}
            <div className="relative px-4 py-3 sm:p-6 text-gray-800">
                {/* 상단: 항공사 로고 + 편명/게이트 */}
                <div className="flex items-center justify-between mb-3">
                    <div className="flex items-center gap-2">
                        {/* 항공사 로고 */}
                        <img
                            src="/images/korean_air.svg"
                            alt="Korean Air"
                            className="h-12 sm:h-16 md:h-20 w-auto object-contain drop-shadow-lg scale-125 sm:scale-150 md:scale-[175] origin-left"
                        />
                    </div>

                    <div className="text-right">
                        <div className="flex items-center gap-4 text-sm text-gray-700">
                            <div>
                                <span className="text-xs opacity-70">
                                    Flight
                                </span>
                                <span className="ml-2 font-semibold text-gray-800">
                                    {ticket.flight || "-"}
                                </span>
                            </div>
                            <div>
                                <span className="text-xs opacity-70">Gate</span>
                                <span className="ml-2 font-semibold text-gray-800">
                                    {ticket.gate || "-"}
                                </span>
                            </div>
                        </div>
                    </div>
                </div>

                {/* 중앙: 출발지 → 도착지 */}
                <div className="flex items-center justify-between mb-3 gap-1">
                    <div className="text-center flex-1 min-w-0">
                        <div className="text-lg sm:text-xl md:text-2xl font-bold tracking-tighter mb-0">
                            {formatCityName(ticket.origin)}
                        </div>
                        <div className="text-xs sm:text-sm text-gray-600">
                            출발
                        </div>
                    </div>

                    {/* 비행기 아이콘과 점선 - 고정 너비 */}
                    <div
                        className="flex-shrink-0 px-1 sm:px-2 flex items-center justify-center"
                        style={{ width: "80px" }}
                    >
                        <div className="flex items-center w-full">
                            <div className="flex-1 border-t-2 border-dashed border-gray-400" />
                            <div className="mx-1 sm:mx-2 relative">
                                {/* 비행기 아이콘 */}
                                <svg
                                    className="w-5 h-5 sm:w-6 sm:h-6 text-gray-700 transform rotate-90"
                                    fill="currentColor"
                                    viewBox="0 0 24 24"
                                >
                                    <path d="M21 16v-2l-8-5V3.5c0-.83-.67-1.5-1.5-1.5S10 2.67 10 3.5V9l-8 5v2l8-2.5V19l-2 1.5V22l3.5-1 3.5 1v-1.5L13 19v-5.5l8 2.5z" />
                                </svg>
                            </div>
                            <div className="flex-1 border-t-2 border-dashed border-gray-400" />
                        </div>
                    </div>

                    <div className="text-center flex-1 min-w-0">
                        <div className="text-lg sm:text-xl md:text-2xl font-bold tracking-tighter mb-0">
                            {formatCityName(ticket.destination)}
                        </div>
                        <div className="text-xs sm:text-sm text-gray-600">
                            도착
                        </div>
                    </div>
                </div>

                {/* 구분선 */}
                <div className="border-t border-gray-300 my-2" />

                {/* 하단: 시간 정보 */}
                <div className="flex justify-between items-start">
                    <div>
                        <div className="text-xs text-gray-500 mb-1">
                            Boarding
                        </div>
                        <div className="text-base font-semibold">
                            {formatTime(ticket.boardingTime)}
                        </div>
                    </div>

                    <div>
                        <div className="text-xs text-gray-500 mb-1">
                            Departs
                        </div>
                        <div className="text-base font-semibold">
                            {formatTime(ticket.departureTime)}
                        </div>
                    </div>

                    {!isCompact && (
                        <div className="text-right">
                            <div className="text-xs text-gray-500 mb-1">
                                Seat
                            </div>
                            <div className="text-base font-semibold">
                                {ticket.seat || "-"}
                            </div>
                        </div>
                    )}
                </div>
            </div>
        </div>
    );
};

export default TicketCard;
