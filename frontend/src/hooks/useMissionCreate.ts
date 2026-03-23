import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { createMission } from "@/api/mission.api";
import { useAuthStore } from "@/store/authStore";
import { useMissionStore } from "@/store/missionStore";
import { PICKUP_LOCATIONS } from "@/constants/locations";

export const useMissionCreate = () => {
    const navigate = useNavigate();
    const { user } = useAuthStore();
    const { setCurrentMission } = useMissionStore();

    const [locationId, setLocationId] = useState<number | null>(null);
    const [error, setError] = useState("");
    const [isCreating, setIsCreating] = useState(false);

    const selectedLocation = PICKUP_LOCATIONS.find((l) => l.id === locationId);

    const handleLocationSelect = (id: number) => {
        setLocationId(id);
        setError("");
    };

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();

        if (!user) {
            setError("사용자 정보가 없습니다.");
            return;
        }

        if (!locationId) {
            setError("픽업 장소를 선택해주세요.");
            return;
        }

        if (!selectedLocation) {
            setError("유효하지 않은 위치입니다.");
            return;
        }

        try {
            setIsCreating(true);
            setError("");

            const response = await createMission({
                callLocationId: locationId,
            });

            if (import.meta.env.DEV) {
                console.log("[MissionCreate] 백엔드 응답:", response);
                console.log("[MissionCreate] missionId:", response.missionId);
            }

            if (!response || !response.missionId) {
                throw new Error(
                    "백엔드 응답에 missionId가 없습니다. 응답: " +
                        JSON.stringify(response),
                );
            }

            setCurrentMission({
                id: response.missionId.toString(),
                userId: 0,
                startLocation: locationId,
                endLocation: 0,
                status: "REQUESTED",
                destination: selectedLocation.name,
                createdAt: new Date().toISOString(),
                updatedAt: new Date().toISOString(),
            });

            navigate("/mission/track");
        } catch (err: unknown) {
            if (import.meta.env.DEV) {
                console.error("미션 생성 실패:", err);
                const axiosErr = err as {
                    response?: { data?: unknown; status?: number };
                };
                console.error("에러 응답:", axiosErr.response?.data);
                console.error("에러 상태:", axiosErr.response?.status);
            }
            setError("미션 생성에 실패했습니다. 다시 시도해주세요.");
        } finally {
            setIsCreating(false);
        }
    };

    return {
        locationId,
        selectedLocation,
        error,
        isCreating,
        handleLocationSelect,
        handleSubmit,
    };
};
