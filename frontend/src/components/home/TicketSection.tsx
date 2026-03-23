import { Button } from "@/components/ui/button";
import TicketCard from "@/components/ticket/TicketCard";
import type { TicketInfo } from "@/types/ticket.types";

interface TicketSectionProps {
    currentTicket: TicketInfo | null;
    isLoading: boolean;
    onTicketClick: () => void;
    onScanClick: () => void;
}

const TicketSection = ({
    currentTicket,
    isLoading,
    onTicketClick,
    onScanClick,
}: TicketSectionProps) => (
    <div
        className="bg-white rounded-2xl p-4 shadow-sm mb-4 animate-fade-in-up"
        style={{ animationDelay: "400ms" }}
    >
        <h3 className="text-gray-900 font-bold text-base mb-4 flex items-center gap-2">
            <svg
                className="w-5 h-5 text-toss-blue-500"
                fill="none"
                viewBox="0 0 24 24"
                stroke="currentColor"
            >
                <path
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    strokeWidth={2}
                    d="M15 5v2m0 4v2m0 4v2M5 5a2 2 0 00-2 2v3a2 2 0 110 4v3a2 2 0 002 2h14a2 2 0 002-2v-3a2 2 0 110-4V7a2 2 0 00-2-2H5z"
                />
            </svg>
            항공권
        </h3>

        {isLoading ? (
            <div className="text-center py-4">
                <div className="animate-spin w-12 h-12 border-4 border-toss-blue-500 border-t-transparent rounded-full mx-auto mb-4" />
                <p className="text-gray-600 text-sm">
                    티켓 정보를 불러오는 중...
                </p>
            </div>
        ) : currentTicket ? (
            <div onClick={onTicketClick} className="cursor-pointer">
                <TicketCard ticket={currentTicket} variant="compact" />
            </div>
        ) : (
            <div className="text-center py-4">
                <div className="w-16 h-16 mx-auto mb-4 bg-gray-100 rounded-full flex items-center justify-center">
                    <svg
                        className="w-8 h-8 text-gray-400"
                        fill="none"
                        viewBox="0 0 24 24"
                        stroke="currentColor"
                    >
                        <path
                            strokeLinecap="round"
                            strokeLinejoin="round"
                            strokeWidth={1.5}
                            d="M15 5v2m0 4v2m0 4v2M5 5a2 2 0 00-2 2v3a2 2 0 110 4v3a2 2 0 002 2h14a2 2 0 002-2v-3a2 2 0 110-4V7a2 2 0 00-2-2H5z"
                        />
                    </svg>
                </div>
                <h4 className="text-gray-900 text-lg font-bold mb-2">
                    티켓을 등록해주세요
                </h4>
                <p className="text-gray-600 text-sm mb-6">
                    비행기 티켓을 스캔하여 등록하세요
                </p>
                <Button
                    onClick={onScanClick}
                    className="w-full h-12 text-base font-semibold bg-toss-blue-500 hover:bg-toss-blue-600 text-white rounded-xl"
                >
                    <svg
                        className="w-5 h-5 mr-2"
                        fill="none"
                        viewBox="0 0 24 24"
                        stroke="currentColor"
                    >
                        <path
                            strokeLinecap="round"
                            strokeLinejoin="round"
                            strokeWidth={2}
                            d="M12 4v1m6 11h2m-6 0h-2v4m0-11v3m0 0h.01M12 12h4.01M16 20h4M4 12h4m12 0h.01M5 8h2a1 1 0 001-1V5a1 1 0 00-1-1H5a1 1 0 00-1 1v2a1 1 0 001 1zm12 0h2a1 1 0 001-1V5a1 1 0 00-1-1h-2a1 1 0 00-1 1v2a1 1 0 001 1zM5 20h2a1 1 0 001-1v-2a1 1 0 00-1-1H5a1 1 0 00-1 1v2a1 1 0 001 1z"
                        />
                    </svg>
                    티켓 스캔하기
                </Button>
            </div>
        )}
    </div>
);

export default TicketSection;
