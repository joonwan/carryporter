import { useState } from 'react'
import { cn } from '@/lib/utils'
import { X, Lock, Unlock } from 'lucide-react'

/** 락커 상태 타입 */
type LockerStatus = 'available' | 'inUse'

/** 락커 데이터 인터페이스 */
interface Locker {
    id: string
    status: LockerStatus
    userId?: string
    robotId?: string
}

interface LockerManagementModalProps {
    /** 모달 열림 상태 */
    isOpen: boolean
    /** 모달 닫기 핸들러 */
    onClose: () => void
    /** 락커 선택 시 호출되는 콜백 */
    onLockerSelect: (lockerId: string) => void
}

// 더미 락커 데이터 (24개)
const generateLockers = (): Locker[] => {
    return Array.from({ length: 24 }, (_, i) => ({
        id: `L${String(i + 1).padStart(3, '0')}`,
        status: [0, 2, 5, 7, 12, 15, 18, 22].includes(i) ? 'inUse' : 'available',
        userId: [0, 2, 5, 7, 12, 15, 18, 22].includes(i) ? `user-${i}` : undefined,
        robotId: [0, 2, 5, 7, 12, 15, 18, 22].includes(i) ? `R00${(i % 6) + 1}` : undefined,
    }))
}

/**
 * 락커 관리 모달 컴포넌트
 * Glassmorphism 스타일과 Neon 아이콘을 활용한 미래지향적 디자인
 */
export default function LockerManagementModal({
    isOpen,
    onClose,
    onLockerSelect,
}: LockerManagementModalProps) {
    const [lockers] = useState<Locker[]>(generateLockers())
    const [selectedLocker, setSelectedLocker] = useState<string | null>(null)

    // 모달이 닫혀있으면 렌더링하지 않음
    if (!isOpen) return null

    // 락커 클릭 핸들러
    const handleLockerClick = (locker: Locker) => {
        if (locker.status === 'available') {
            setSelectedLocker(locker.id)
            onLockerSelect(locker.id)
        }
    }

    // 상단 12개, 하단 12개로 분리
    const topRow = lockers.slice(0, 12)
    const bottomRow = lockers.slice(12, 24)

    return (
        <div className="modal-overlay" onClick={onClose}>
            <div
                className="glass-modal w-[700px] max-w-[90vw] animate-slideUp"
                onClick={(e) => e.stopPropagation()}
            >
                {/* 헤더 */}
                <div className="flex items-center justify-between p-5 border-b border-border-default/50">
                    <div>
                        <h2 className="gradient-title text-xl font-bold">
                            Locker Management
                        </h2>
                        <p className="text-text-secondary text-sm mt-1">
                            Click on an available locker to assign user and robot
                        </p>
                    </div>
                    <button
                        onClick={onClose}
                        className="p-2 rounded-lg hover:bg-bg-secondary transition-colors"
                    >
                        <X className="w-5 h-5 text-text-secondary neon-icon-cyan" />
                    </button>
                </div>

                {/* 범례 */}
                <div className="flex items-center justify-end gap-6 px-5 py-3">
                    <div className="flex items-center gap-2">
                        <div className="w-3 h-3 rounded-sm bg-status-active" />
                        <span className="text-text-secondary text-xs">Available</span>
                    </div>
                    <div className="flex items-center gap-2">
                        <div className="w-3 h-3 rounded-sm bg-status-warning" />
                        <span className="text-text-secondary text-xs">In Use</span>
                    </div>
                </div>

                {/* 락커 그리드 */}
                <div className="p-5 space-y-3">
                    {/* 상단 행 */}
                    <div className="grid grid-cols-12 gap-2">
                        {topRow.map((locker) => (
                            <LockerCard
                                key={locker.id}
                                locker={locker}
                                isSelected={selectedLocker === locker.id}
                                onClick={() => handleLockerClick(locker)}
                            />
                        ))}
                    </div>

                    {/* 하단 행 */}
                    <div className="grid grid-cols-12 gap-2">
                        {bottomRow.map((locker) => (
                            <LockerCard
                                key={locker.id}
                                locker={locker}
                                isSelected={selectedLocker === locker.id}
                                onClick={() => handleLockerClick(locker)}
                            />
                        ))}
                    </div>
                </div>
            </div>
        </div>
    )
}

/** 개별 락커 카드 컴포넌트 */
function LockerCard({
    locker,
    isSelected,
    onClick,
}: {
    locker: Locker
    isSelected: boolean
    onClick: () => void
}) {
    const isAvailable = locker.status === 'available'

    return (
        <button
            onClick={onClick}
            disabled={!isAvailable}
            className={cn(
                "glass-card p-2 flex flex-col items-center justify-center gap-1",
                "min-h-[60px] transition-all duration-300",
                isAvailable && "cursor-pointer hover:scale-105",
                !isAvailable && "cursor-not-allowed opacity-70",
                isSelected && "neon-border-cyan scale-105",
                isAvailable && !isSelected && "neon-border-green",
                !isAvailable && "neon-border-orange"
            )}
        >
            {/* 아이콘 */}
            {isAvailable ? (
                <Unlock className={cn(
                    "w-4 h-4",
                    isSelected ? "neon-icon-cyan" : "neon-icon-green"
                )} />
            ) : (
                <Lock className="w-4 h-4 neon-icon-orange" />
            )}

            {/* 락커 ID */}
            <span className={cn(
                "text-[10px] font-bold",
                isSelected && "text-accent-cyan",
                isAvailable && !isSelected && "text-status-active",
                !isAvailable && "text-status-warning"
            )}>
                {locker.id}
            </span>
        </button>
    )
}
