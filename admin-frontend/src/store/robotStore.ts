import { create } from 'zustand'

/**
 * 로봇 상태 타입
 */
export type RobotStatus = 'available' | 'busy' | 'returning' | 'maintenance'

/**
 * 로봇 잠금 상태 타입
 */
export type LockStatus = 'locked' | 'unlocked'

/**
 * 로봇 인터페이스
 */
export interface Robot {
    id: string
    name: string
    status: RobotStatus
    lockStatus: LockStatus
    battery: number
    position: { x: number; y: number }
    currentMissionId?: string
    assignedLockerId?: string
}

/**
 * 로봇 스토어 인터페이스
 */
interface RobotState {
    /** 로봇 목록 */
    robots: Robot[]
    /** 선택된 로봇 ID */
    selectedRobotId: string | null

    /** 로봇 선택 */
    selectRobot: (id: string | null) => void
    /** 로봇 잠금 해제 (열기) */
    unlockRobot: (id: string) => void
    /** 로봇 잠금 (닫기) */
    lockRobot: (id: string) => void
    /** 로봇 상태 업데이트 */
    updateRobotStatus: (id: string, status: RobotStatus) => void
    /** 로봇에 사물함 할당 */
    assignLockerToRobot: (robotId: string, lockerId: string) => void
    /** 로봇 출발 */
    dispatchRobot: (id: string) => void
}

// 더미 로봇 데이터
const initialRobots: Robot[] = [
    { id: 'r1', name: 'R001', status: 'available', lockStatus: 'locked', battery: 85, position: { x: 25, y: 35 } },
    { id: 'r2', name: 'R002', status: 'busy', lockStatus: 'locked', battery: 72, position: { x: 45, y: 55 }, currentMissionId: 'm1' },
    { id: 'r3', name: 'R003', status: 'available', lockStatus: 'locked', battery: 100, position: { x: 70, y: 40 } },
    { id: 'r4', name: 'R004', status: 'returning', lockStatus: 'locked', battery: 45, position: { x: 35, y: 70 } },
    { id: 'r5', name: 'R005', status: 'busy', lockStatus: 'unlocked', battery: 60, position: { x: 60, y: 65 }, currentMissionId: 'm2' },
    { id: 'r6', name: 'R006', status: 'maintenance', lockStatus: 'locked', battery: 31, position: { x: 80, y: 75 } },
]

/**
 * 로봇 관리 Store
 * - 로봇 상태 관리
 * - 잠금/해제 제어
 * - 사물함 할당
 */
export const useRobotStore = create<RobotState>((set) => ({
    robots: initialRobots,
    selectedRobotId: null,

    selectRobot: (id) => set({ selectedRobotId: id }),

    unlockRobot: (id) =>
        set((state) => ({
            robots: state.robots.map((robot) =>
                robot.id === id ? { ...robot, lockStatus: 'unlocked' } : robot
            ),
        })),

    lockRobot: (id) =>
        set((state) => ({
            robots: state.robots.map((robot) =>
                robot.id === id ? { ...robot, lockStatus: 'locked' } : robot
            ),
        })),

    updateRobotStatus: (id, status) =>
        set((state) => ({
            robots: state.robots.map((robot) =>
                robot.id === id ? { ...robot, status } : robot
            ),
        })),

    assignLockerToRobot: (robotId, lockerId) =>
        set((state) => ({
            robots: state.robots.map((robot) =>
                robot.id === robotId ? { ...robot, assignedLockerId: lockerId } : robot
            ),
        })),

    dispatchRobot: (id) =>
        set((state) => ({
            robots: state.robots.map((robot) =>
                robot.id === id
                    ? { ...robot, status: 'busy', lockStatus: 'locked' }
                    : robot
            ),
        })),
}))
