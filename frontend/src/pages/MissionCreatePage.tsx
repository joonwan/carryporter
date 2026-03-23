import { useNavigate } from "react-router-dom";
import { Button } from "@/components/ui/button";
import { LocationSelector } from "@/components/mission/LocationSelector";
import MissionSummaryCard from "@/components/mission/MissionSummaryCard";
import { AppHeader } from "@/components/layouts/AppHeader";
import { useMissionCreate } from "@/hooks/useMissionCreate";
import { PICKUP_LOCATIONS } from "../constants/locations";

const MissionCreatePage = () => {
    const navigate = useNavigate();
    const {
        locationId,
        selectedLocation,
        error,
        isCreating,
        handleLocationSelect,
        handleSubmit,
    } = useMissionCreate();

    return (
        <div className="min-h-screen bg-gray-50">
            <AppHeader showCloseButton onClose={() => navigate("/home")} />

            <main className="max-w-md mx-auto px-6 py-6">
                <div className="mb-6 animate-fade-in-up">
                    <h2 className="text-heading-1 mb-1">로봇 호출 🤖</h2>
                    <p className="text-body-small">
                        픽업 장소를 선택하세요
                    </p>
                </div>

                <form onSubmit={handleSubmit} className="space-y-6">
                    <div className="bg-white rounded-2xl p-5 shadow-sm animate-fade-in-up">
                        <h3 className="text-gray-900 font-bold text-base mb-4 flex items-center gap-2">
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
                                    d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z"
                                />
                                <path
                                    strokeLinecap="round"
                                    strokeLinejoin="round"
                                    strokeWidth={2}
                                    d="M15 11a3 3 0 11-6 0 3 3 0 016 0z"
                                />
                            </svg>
                            픽업 위치
                        </h3>

                        <LocationSelector
                            locations={PICKUP_LOCATIONS}
                            selectedLocationId={locationId}
                            onSelect={handleLocationSelect}
                        />
                    </div>

                    {error && (
                        <div className="bg-red-50 border border-red-200 rounded-xl p-4 animate-fade-in-up">
                            <p className="text-red-600 text-sm text-center">
                                {error}
                            </p>
                        </div>
                    )}

                    {selectedLocation && (
                        <MissionSummaryCard
                            locationName={selectedLocation.name}
                        />
                    )}

                    <Button
                        type="submit"
                        disabled={!locationId || isCreating}
                        className="w-full h-12 text-base font-semibold bg-toss-blue-500 hover:bg-toss-blue-600 text-white disabled:opacity-40 rounded-xl"
                    >
                        {isCreating ? (
                            <span className="flex items-center gap-2">
                                <div className="w-5 h-5 border-2 border-white border-t-transparent rounded-full animate-spin" />
                                호출 중...
                            </span>
                        ) : !locationId ? (
                            <span className="flex items-center gap-2">
                                <svg
                                    className="w-5 h-5"
                                    fill="none"
                                    viewBox="0 0 24 24"
                                    stroke="currentColor"
                                >
                                    <path
                                        strokeLinecap="round"
                                        strokeLinejoin="round"
                                        strokeWidth={2}
                                        d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"
                                    />
                                </svg>
                                위치를 선택해주세요
                            </span>
                        ) : (
                            <span className="flex items-center gap-2">
                                <svg
                                    className="w-5 h-5"
                                    fill="none"
                                    viewBox="0 0 24 24"
                                    stroke="currentColor"
                                >
                                    <path
                                        strokeLinecap="round"
                                        strokeLinejoin="round"
                                        strokeWidth={2}
                                        d="M13 10V3L4 14h7v7l9-11h-7z"
                                    />
                                </svg>
                                로봇 호출하기
                            </span>
                        )}
                    </Button>
                </form>
            </main>
        </div>
    );
};

export default MissionCreatePage;
