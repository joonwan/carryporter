import { useState } from "react";
import { useMissionStore } from "@/store/missionStore";
import { lockMission, returnMission } from "@/api/mission.api";

interface ChecklistState {
    workCompleted: boolean;
    lockerChecked: boolean;
    confirmReturn: boolean;
}

/**
 * 체크리스트 흐름 관리 훅 — ChecklistModal에서 비즈니스 로직 분리
 */
export const useChecklistFlow = (onReturnSuccess: () => void) => {
    const { currentMission, updateMissionStatus } = useMissionStore();

    const [isReturning, setIsReturning] = useState(false);
    const [checklist, setChecklist] = useState<ChecklistState>({
        workCompleted: false,
        lockerChecked: false,
        confirmReturn: false,
    });

    const allChecked =
        checklist.workCompleted &&
        checklist.lockerChecked &&
        checklist.confirmReturn;

    const handleChecklistChange = (key: keyof ChecklistState) => {
        setChecklist((prev) => ({ ...prev, [key]: !prev[key] }));
    };

    const handleReturn = async () => {
        if (!currentMission?.id) return;

        setIsReturning(true);
        try {
            await lockMission(Number(currentMission.id));
            updateMissionStatus({ status: "LOCKED" });
            await returnMission(Number(currentMission.id));
            onReturnSuccess();
        } catch (error) {
            console.error("[ChecklistModal] 잠금/복귀 실패:", error);
            setIsReturning(false);
        }
    };

    return {
        checklist,
        allChecked,
        isReturning,
        handleChecklistChange,
        handleReturn,
    };
};
