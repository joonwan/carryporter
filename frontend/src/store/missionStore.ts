import { create } from "zustand";
import type {
    Mission,
    MissionStatus,
    MissionType,
} from "../types/mission.types";
import type { CurrentLocker } from "../types/locker.types";

interface MissionState {
    // 미션 정보
    currentMission: Mission | null;

    // 현재 사물함 (API에서 조회, localStorage 사용 안 함)
    currentLocker: CurrentLocker | null;

    // 액션
    setCurrentMission: (mission: Mission) => void;
    updateMissionStatus: (update: {
        status: MissionStatus;
        robotCode?: string;
    }) => void;
    clearMission: () => void;

    // 미션 타입 설정 (보관/반납)
    setMissionType: (missionType: MissionType) => void;

    // 현재 사물함 관리
    setCurrentLocker: (locker: CurrentLocker | null) => void;
}

export const useMissionStore = create<MissionState>((set) => ({
    currentMission: null,
    currentLocker: null,

    setCurrentMission: (mission) => set({ currentMission: mission }),

    updateMissionStatus: (status) =>
        set((state) => ({
            currentMission: state.currentMission
                ? {
                      ...state.currentMission,
                      status: status.status,
                      robotCode:
                          status.robotCode || state.currentMission.robotCode,
                  }
                : null,
        })),

    clearMission: () =>
        set({
            currentMission: null,
            currentLocker: null,
        }),

    /**
     * 미션 타입 설정 (보관/반납)
     */
    setMissionType: (missionType) =>
        set((state) => ({
            currentMission: state.currentMission
                ? { ...state.currentMission, missionType }
                : null,
        })),

    /**
     * 현재 사물함 설정 (API에서 조회한 값)
     */
    setCurrentLocker: (locker) => set({ currentLocker: locker }),
}));
