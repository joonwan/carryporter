import { useNavigate } from "react-router-dom";
import { useAuthStore } from "../store/authStore";
import { useSSEStore } from "../store/sseStore";
import { useTicketData } from "@/hooks/useTicketData";
import { useLockerData } from "@/hooks/useLockerData";
import { AppHeader } from "@/components/layouts/AppHeader";
import WelcomeSection from "@/components/home/WelcomeSection";
import RobotCallCard from "@/components/home/RobotCallCard";
import RobotStatusCard from "@/components/home/RobotStatusCard";
import LockerStatusCard from "@/components/home/LockerStatusCard";
import TicketSection from "@/components/home/TicketSection";

const HomePage = () => {
    const navigate = useNavigate();
    const { user } = useAuthStore();
    const { isConnected } = useSSEStore();
    const { currentTicket, isLoading: isLoadingTicket } = useTicketData();
    const {
        currentLocker,
        isLoading: isLoadingLocker,
        error: lockerError,
    } = useLockerData();

    return (
        <div className="min-h-screen bg-gray-50">
            <AppHeader
                rightElement={
                    <div className="flex items-center gap-2">
                        <div
                            className={`w-2 h-2 rounded-full ${isConnected ? "bg-sub-cyan animate-pulse" : "bg-gray-400"}`}
                        />
                        <span className="text-caption">
                            {isConnected ? "실시간 연결" : "오프라인"}
                        </span>
                    </div>
                }
            />

            <main className="max-w-md mx-auto px-6 py-6">
                <WelcomeSection email={user?.email} />
                <RobotCallCard onClick={() => navigate("/mission/create")} />
                <RobotStatusCard />
                <LockerStatusCard
                    currentLocker={currentLocker}
                    isLoading={isLoadingLocker}
                    error={lockerError}
                />
                <TicketSection
                    currentTicket={currentTicket}
                    isLoading={isLoadingTicket}
                    onTicketClick={() => navigate("/ticket/detail")}
                    onScanClick={() => navigate("/ticket/scan")}
                />
            </main>
        </div>
    );
};

export default HomePage;
