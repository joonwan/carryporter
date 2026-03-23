import { cn } from '@/lib/utils'
import { useLockerStore } from '@/store/lockerStore'
import { Archive, Lock, Unlock, User, Bot, Package } from 'lucide-react'

/**
 * 사물함 관리 페이지
 * - 모든 사물함 상태 표시
 * - 사물함 선택 → 상세 정보
 * - 할당/해제 기능
 */
export default function LockersPage() {
    const { lockers, selectedLockerId, selectLocker, assignLocker, releaseLocker } = useLockerStore()
    const selectedLocker = lockers.find((l) => l.id === selectedLockerId)

    // 사물함 할당 핸들러 (간단한 더미 데이터로 할당)
    const handleAssignLocker = () => {
        if (!selectedLocker) return
        // 더미 데이터로 할당 (추후 API 연동 시 실제 데이터로 변경)
        assignLocker(
            selectedLocker.id,
            `user-${Date.now()}`,
            'Test User',
            'r1'
        )
    }

    // 상태별 스타일
    const statusStyles = {
        available: { border: 'neon-border-green', text: 'text-status-active', label: 'Available' },
        occupied: { border: 'neon-border-orange', text: 'text-status-warning', label: 'Occupied' },
        reserved: { border: 'neon-border-cyan', text: 'text-accent-cyan', label: 'Reserved' },
    }

    // 상단 행, 하단 행 분리
    const topRow = lockers.slice(0, 12)
    const bottomRow = lockers.slice(12, 24)

    return (
        <div className="space-y-6">
            {/* 헤더 */}
            <div>
                <h1 className="gradient-title text-2xl font-bold">Locker Management</h1>
                <p className="text-text-secondary text-sm mt-1">
                    Manage storage lockers and assignments
                </p>
            </div>

            {/* 범례 */}
            <div className="flex items-center gap-6">
                <div className="flex items-center gap-2">
                    <div className="w-3 h-3 rounded-sm bg-status-active" />
                    <span className="text-text-secondary text-xs">Available</span>
                </div>
                <div className="flex items-center gap-2">
                    <div className="w-3 h-3 rounded-sm bg-status-warning" />
                    <span className="text-text-secondary text-xs">Occupied</span>
                </div>
                <div className="flex items-center gap-2">
                    <div className="w-3 h-3 rounded-sm bg-accent-cyan" />
                    <span className="text-text-secondary text-xs">Reserved</span>
                </div>
            </div>

            <div className="grid grid-cols-3 gap-6">
                {/* 사물함 그리드 */}
                <div className="col-span-2 space-y-4">
                    <h2 className="text-text-primary font-medium">All Lockers</h2>

                    {/* 상단 행 */}
                    <div className="grid grid-cols-12 gap-2">
                        {topRow.map((locker) => {
                            const style = statusStyles[locker.status]
                            return (
                                <button
                                    key={locker.id}
                                    onClick={() => selectLocker(locker.id)}
                                    className={cn(
                                        "glass-card p-2 flex flex-col items-center justify-center gap-1 min-h-[70px] transition-all",
                                        selectedLockerId === locker.id ? "neon-border-cyan scale-105" : style.border
                                    )}
                                >
                                    {locker.status === 'available' ? (
                                        <Unlock className={cn("w-4 h-4", style.text)} />
                                    ) : (
                                        <Lock className={cn("w-4 h-4", style.text)} />
                                    )}
                                    <span className={cn("text-[10px] font-bold", style.text)}>
                                        {locker.name}
                                    </span>
                                </button>
                            )
                        })}
                    </div>

                    {/* 하단 행 */}
                    <div className="grid grid-cols-12 gap-2">
                        {bottomRow.map((locker) => {
                            const style = statusStyles[locker.status]
                            return (
                                <button
                                    key={locker.id}
                                    onClick={() => selectLocker(locker.id)}
                                    className={cn(
                                        "glass-card p-2 flex flex-col items-center justify-center gap-1 min-h-[70px] transition-all",
                                        selectedLockerId === locker.id ? "neon-border-cyan scale-105" : style.border
                                    )}
                                >
                                    {locker.status === 'available' ? (
                                        <Unlock className={cn("w-4 h-4", style.text)} />
                                    ) : (
                                        <Lock className={cn("w-4 h-4", style.text)} />
                                    )}
                                    <span className={cn("text-[10px] font-bold", style.text)}>
                                        {locker.name}
                                    </span>
                                </button>
                            )
                        })}
                    </div>
                </div>

                {/* 상세 패널 */}
                <div className="glass-card p-6">
                    <h2 className="text-text-primary font-medium mb-4">Locker Details</h2>

                    {selectedLocker ? (
                        <div className="space-y-6">
                            {/* 사물함 정보 */}
                            <div className="text-center">
                                <Archive className="w-16 h-16 mx-auto neon-icon-cyan mb-4" />
                                <h3 className="text-xl font-bold text-text-primary">{selectedLocker.name}</h3>
                                <span className={cn(
                                    "inline-block px-3 py-1 rounded-full text-xs mt-2",
                                    statusStyles[selectedLocker.status].text,
                                    selectedLocker.status === 'available' ? 'bg-status-active/20' : 'bg-status-warning/20'
                                )}>
                                    {statusStyles[selectedLocker.status].label}
                                </span>
                            </div>

                            {/* 상세 정보 */}
                            {selectedLocker.status === 'occupied' && (
                                <div className="space-y-3 text-sm">
                                    <div className="flex justify-between">
                                        <span className="text-text-secondary flex items-center gap-2">
                                            <User className="w-4 h-4" /> User
                                        </span>
                                        <span className="text-text-primary">{selectedLocker.userName}</span>
                                    </div>
                                    <div className="flex justify-between">
                                        <span className="text-text-secondary flex items-center gap-2">
                                            <Bot className="w-4 h-4" /> Robot
                                        </span>
                                        <span className="text-accent-cyan">{selectedLocker.robotId}</span>
                                    </div>
                                    {selectedLocker.luggageWeight && (
                                        <div className="flex justify-between">
                                            <span className="text-text-secondary flex items-center gap-2">
                                                <Package className="w-4 h-4" /> Weight
                                            </span>
                                            <span className="text-text-primary">{selectedLocker.luggageWeight} kg</span>
                                        </div>
                                    )}
                                </div>
                            )}

                            {/* 액션 버튼 */}
                            <div className="space-y-3">
                                {selectedLocker.status === 'available' ? (
                                    <button
                                        onClick={handleAssignLocker}
                                        className="w-full gradient-button"
                                    >
                                        Assign Locker
                                    </button>
                                ) : (
                                    <button
                                        onClick={() => releaseLocker(selectedLocker.id)}
                                        className="w-full glass-card py-3 text-status-error hover:neon-border-cyan transition-all"
                                    >
                                        Release Locker
                                    </button>
                                )}
                            </div>
                        </div>
                    ) : (
                        <div className="text-center py-12 text-text-secondary">
                            <Archive className="w-12 h-12 mx-auto mb-4 opacity-50" />
                            <p>Select a locker to view details</p>
                        </div>
                    )}
                </div>
            </div>
        </div>
    )
}
