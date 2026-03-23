import type { MissionStatus } from "@/types/mission.types";

/**
 * 미션 상태 전이 규칙
 */
export const canTransitionTo = (
    currentStatus: MissionStatus,
    nextStatus: MissionStatus,
): boolean => {
    const transitions: Record<MissionStatus, MissionStatus[]> = {
        REQUESTED: ["ASSIGNED", "ABORTED"],
        ASSIGNED: ["MOVING", "ABORTED"],
        MOVING: ["ARRIVED", "ABORTED"],
        ARRIVED: ["UNLOCKED", "ABORTED"],
        UNLOCKED: ["LOCKED", "ABORTED"],
        LOCKED: ["RETURNING", "ABORTED"],
        RETURNING: ["RETURNED", "ABORTED"],
        RETURNED: ["FINISHED"],
        FINISHED: [],
        ABORTED: [],
    };

    return transitions[currentStatus]?.includes(nextStatus) ?? false;
};

/**
 * 진행률 계산 (0-100)
 */
export const calculateProgress = (status: MissionStatus): number => {
    const progressMap: Record<MissionStatus, number> = {
        REQUESTED: 20,
        ASSIGNED: 40,
        MOVING: 60,
        ARRIVED: 70,
        UNLOCKED: 75,
        LOCKED: 80,
        RETURNING: 90,
        RETURNED: 95,
        FINISHED: 100,
        ABORTED: 0,
    };

    return progressMap[status] ?? 0;
};

/**
 * 진행 단계 계산 (1-5)
 */
export const calculateProgressStep = (status: MissionStatus): number => {
    if (["REQUESTED"].includes(status)) return 1;
    if (["ASSIGNED"].includes(status)) return 2;
    if (["MOVING"].includes(status)) return 3;
    if (["ARRIVED", "UNLOCKED", "LOCKED"].includes(status)) return 4;
    if (["RETURNING", "RETURNED", "FINISHED"].includes(status)) return 5;
    return 0;
};

/**
 * 상태 메시지 반환
 */
export const getStatusMessage = (status: MissionStatus): string => {
    const messages: Record<MissionStatus, string> = {
        REQUESTED: "요청됨",
        ASSIGNED: "로봇 배정됨",
        MOVING: "로봇이 이동 중입니다",
        ARRIVED: "로봇이 도착했습니다",
        UNLOCKED: "짐 확인 중",
        LOCKED: "수령 완료",
        RETURNING: "로봇이 복귀 중입니다",
        RETURNED: "보관 완료",
        FINISHED: "미션 완료!",
        ABORTED: "미션이 중단되었습니다",
    };

    return messages[status] ?? "작업 중입니다";
};

/**
 * 특정 단계(step)가 현재 상태 기준으로 완료되었는지 판별
 * 상태 순서: REQUESTED → ASSIGNED → MOVING → ARRIVED → UNLOCKED → LOCKED → RETURNING → RETURNED → FINISHED
 */
const STATUS_ORDER: MissionStatus[] = [
    "REQUESTED",
    "ASSIGNED",
    "MOVING",
    "ARRIVED",
    "UNLOCKED",
    "LOCKED",
    "RETURNING",
    "RETURNED",
    "FINISHED",
];

export const isStepCompleted = (
    currentStatus: MissionStatus,
    stepStatus: MissionStatus,
): boolean => {
    const currentIdx = STATUS_ORDER.indexOf(currentStatus);
    const stepIdx = STATUS_ORDER.indexOf(stepStatus);
    if (currentIdx === -1 || stepIdx === -1) return false;
    return currentIdx > stepIdx;
};
