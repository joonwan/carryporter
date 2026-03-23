import { create } from 'zustand'

/**
 * 사물함 상태 타입
 */
export type LockerStatus = 'available' | 'occupied' | 'reserved'

/**
 * 사물함 인터페이스
 */
export interface Locker {
    id: string
    name: string
    status: LockerStatus
    userId?: string
    userName?: string
    robotId?: string
    luggageWeight?: number
}

/**
 * 사물함 스토어 인터페이스
 */
interface LockerState {
    /** 사물함 목록 */
    lockers: Locker[]
    /** 선택된 사물함 ID */
    selectedLockerId: string | null

    /** 사물함 선택 */
    selectLocker: (id: string | null) => void
    /** 사물함 할당 */
    assignLocker: (lockerId: string, userId: string, userName: string, robotId: string) => void
    /** 사물함 해제 */
    releaseLocker: (lockerId: string) => void
    /** 사물함 상태 업데이트 */
    updateLockerStatus: (lockerId: string, status: LockerStatus) => void
    /** 짐 무게 업데이트 */
    updateLuggageWeight: (lockerId: string, weight: number) => void
}

// 더미 사물함 데이터 (24개)
const initialLockers: Locker[] = Array.from({ length: 24 }, (_, i) => {
    const id = `L${String(i + 1).padStart(3, '0')}`
    const isOccupied = [2, 5, 8, 12, 15, 20].includes(i)
    return {
        id,
        name: id,
        status: isOccupied ? 'occupied' : 'available',
        userId: isOccupied ? `user-${i}` : undefined,
        userName: isOccupied ? ['John Smith', 'Sarah Johnson', 'Ahmed Al-Rashid', 'Maria Garcia', 'Kim Minsu', 'Park Jihye'][i % 6] : undefined,
        robotId: isOccupied ? `r${(i % 6) + 1}` : undefined,
        luggageWeight: isOccupied ? Math.floor(Math.random() * 20) + 5 : undefined,
    }
})

/**
 * 사물함 관리 Store
 * - 사물함 상태 관리
 * - 할당/해제
 * - 짐 무게 추적
 */
export const useLockerStore = create<LockerState>((set) => ({
    lockers: initialLockers,
    selectedLockerId: null,

    selectLocker: (id) => set({ selectedLockerId: id }),

    assignLocker: (lockerId, userId, userName, robotId) =>
        set((state) => ({
            lockers: state.lockers.map((locker) =>
                locker.id === lockerId
                    ? { ...locker, status: 'occupied', userId, userName, robotId }
                    : locker
            ),
        })),

    releaseLocker: (lockerId) =>
        set((state) => ({
            lockers: state.lockers.map((locker) =>
                locker.id === lockerId
                    ? { ...locker, status: 'available', userId: undefined, userName: undefined, robotId: undefined, luggageWeight: undefined }
                    : locker
            ),
        })),

    updateLockerStatus: (lockerId, status) =>
        set((state) => ({
            lockers: state.lockers.map((locker) =>
                locker.id === lockerId ? { ...locker, status } : locker
            ),
        })),

    updateLuggageWeight: (lockerId, weight) =>
        set((state) => ({
            lockers: state.lockers.map((locker) =>
                locker.id === lockerId ? { ...locker, luggageWeight: weight } : locker
            ),
        })),
}))
