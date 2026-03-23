import { useState } from 'react'
import { cn } from '@/lib/utils'
import { X, User, MapPin, BatteryLow, BatteryMedium, BatteryFull, Bot } from 'lucide-react'

/** 사용자 데이터 인터페이스 */
interface UserData {
    id: string
    name: string
    flight: string
    gate: string
    seat: string
}

/** 로봇 데이터 인터페이스 */
interface RobotData {
    id: string
    name: string
    battery: number
    status: 'available' | 'busy'
}

/** 목적지(게이트) 데이터 */
interface Destination {
    id: string
    name: string
}

interface AssignToLockerModalProps {
    /** 모달 열림 상태 */
    isOpen: boolean
    /** 모달 닫기 핸들러 */
    onClose: () => void
    /** 선택된 락커 ID */
    lockerId: string | null
    /** 할당 완료 시 콜백 */
    onAssign: (data: { lockerId: string; userId: string; robotId: string; destination: string }) => void
}

// 더미 사용자 데이터
const users: UserData[] = [
    { id: 'u1', name: 'John Smith', flight: 'SQ421', gate: 'A12', seat: '12A' },
    { id: 'u2', name: 'Sarah Johnson', flight: 'GA001', gate: 'B5', seat: '8B' },
    { id: 'u3', name: 'Ahmed Al-Rashid', flight: 'GA001', gate: 'C8', seat: '15C' },
    { id: 'u4', name: 'Maria Garcia', flight: 'KE932', gate: 'D3', seat: '22A' },
]

// 더미 로봇 데이터
const robots: RobotData[] = [
    { id: 'r1', name: 'R001', battery: 85, status: 'available' },
    { id: 'r2', name: 'R002', battery: 72, status: 'busy' },
    { id: 'r3', name: 'R004', battery: 100, status: 'available' },
    { id: 'r4', name: 'R006', battery: 31, status: 'available' },
]

// 목적지 데이터
const destinations: Destination[] = [
    { id: 'd1', name: 'Gate A' },
    { id: 'd2', name: 'Gate B' },
    { id: 'd3', name: 'Gate C' },
    { id: 'd4', name: 'Gate D' },
]

/**
 * 락커 할당 모달 컴포넌트
 * 사용자, 로봇, 목적지를 선택하여 락커에 할당
 */
export default function AssignToLockerModal({
    isOpen,
    onClose,
    lockerId,
    onAssign,
}: AssignToLockerModalProps) {
    const [selectedUser, setSelectedUser] = useState<string | null>(null)
    const [selectedRobot, setSelectedRobot] = useState<string | null>(null)
    const [selectedDestination, setSelectedDestination] = useState<string | null>(null)

    // 모달이 닫혀있거나 락커 ID가 없으면 렌더링하지 않음
    if (!isOpen || !lockerId) return null

    // 할당 버튼 활성화 조건
    const canAssign = selectedUser && selectedRobot && selectedDestination

    // 할당 처리
    const handleAssign = () => {
        if (canAssign) {
            onAssign({
                lockerId,
                userId: selectedUser,
                robotId: selectedRobot,
                destination: selectedDestination,
            })
            onClose()
        }
    }

    return (
        <div className="modal-overlay" onClick={onClose}>
            <div
                className="glass-modal w-[600px] max-w-[90vw] animate-slideUp"
                onClick={(e) => e.stopPropagation()}
            >
                {/* 헤더 */}
                <div className="flex items-center justify-between p-5 border-b border-border-default/50">
                    <div>
                        <h2 className="gradient-title text-xl font-bold">
                            Assign to Locker
                        </h2>
                        <p className="text-accent-cyan text-sm mt-1 font-mono">
                            {lockerId} - Select user, robot, and destination
                        </p>
                    </div>
                    <button
                        onClick={onClose}
                        className="p-2 rounded-lg hover:bg-bg-secondary transition-colors"
                    >
                        <X className="w-5 h-5 text-text-secondary neon-icon-cyan" />
                    </button>
                </div>

                {/* 콘텐츠 */}
                <div className="p-5 grid grid-cols-2 gap-5">
                    {/* 왼쪽: 사용자 선택 */}
                    <div>
                        <div className="flex items-center gap-2 mb-3">
                            <User className="w-4 h-4 neon-icon-cyan" />
                            <span className="text-text-secondary text-sm">Select User</span>
                        </div>
                        <div className="space-y-2 max-h-[200px] overflow-y-auto">
                            {users.map((user) => (
                                <button
                                    key={user.id}
                                    onClick={() => setSelectedUser(user.id)}
                                    className={cn(
                                        "glass-card w-full p-3 text-left transition-all",
                                        selectedUser === user.id && "neon-border-cyan"
                                    )}
                                >
                                    <div className="flex items-center gap-3">
                                        <div className="w-8 h-8 rounded-full bg-bg-secondary flex items-center justify-center">
                                            <User className="w-4 h-4 text-text-secondary" />
                                        </div>
                                        <div>
                                            <p className="text-text-primary text-sm font-medium">
                                                {user.name}
                                            </p>
                                            <p className="text-text-muted text-xs">
                                                Flight: {user.flight} • Gate: {user.gate}
                                            </p>
                                        </div>
                                    </div>
                                </button>
                            ))}
                        </div>
                    </div>

                    {/* 오른쪽: 로봇 및 목적지 선택 */}
                    <div className="space-y-5">
                        {/* 로봇 선택 */}
                        <div>
                            <div className="flex items-center gap-2 mb-3">
                                <Bot className="w-4 h-4 neon-icon-purple" />
                                <span className="text-text-secondary text-sm">Select Robot</span>
                            </div>
                            <div className="grid grid-cols-2 gap-2">
                                {robots.map((robot) => (
                                    <RobotCard
                                        key={robot.id}
                                        robot={robot}
                                        isSelected={selectedRobot === robot.id}
                                        onClick={() => robot.status === 'available' && setSelectedRobot(robot.id)}
                                    />
                                ))}
                            </div>
                        </div>

                        {/* 목적지 선택 */}
                        <div>
                            <div className="flex items-center gap-2 mb-3">
                                <MapPin className="w-4 h-4 neon-icon-green" />
                                <span className="text-text-secondary text-sm">Select Destination</span>
                            </div>
                            <div className="grid grid-cols-2 gap-2">
                                {destinations.map((dest) => (
                                    <button
                                        key={dest.id}
                                        onClick={() => setSelectedDestination(dest.id)}
                                        className={cn(
                                            "glass-card p-3 flex items-center gap-2 transition-all",
                                            selectedDestination === dest.id && "neon-border-cyan"
                                        )}
                                    >
                                        <MapPin className={cn(
                                            "w-4 h-4",
                                            selectedDestination === dest.id ? "neon-icon-cyan" : "text-text-muted"
                                        )} />
                                        <span className={cn(
                                            "text-sm",
                                            selectedDestination === dest.id ? "text-accent-cyan" : "text-text-secondary"
                                        )}>
                                            {dest.name}
                                        </span>
                                    </button>
                                ))}
                            </div>
                        </div>
                    </div>
                </div>

                {/* 하단 버튼 */}
                <div className="p-5 border-t border-border-default/50 flex justify-center">
                    <button
                        onClick={handleAssign}
                        disabled={!canAssign}
                        className={cn(
                            "gradient-button w-[250px]",
                            !canAssign && "opacity-50 cursor-not-allowed"
                        )}
                    >
                        Assign & Deploy Robot
                    </button>
                </div>
            </div>
        </div>
    )
}

/** 로봇 카드 컴포넌트 */
function RobotCard({
    robot,
    isSelected,
    onClick,
}: {
    robot: RobotData
    isSelected: boolean
    onClick: () => void
}) {
    const isAvailable = robot.status === 'available'

    // 배터리 아이콘 결정
    const BatteryIcon = robot.battery > 70 ? BatteryFull
        : robot.battery > 30 ? BatteryMedium
            : BatteryLow

    // 배터리 색상 결정
    const batteryColor = robot.battery > 70 ? 'text-status-active'
        : robot.battery > 30 ? 'text-status-warning'
            : 'text-status-error'

    return (
        <button
            onClick={onClick}
            disabled={!isAvailable}
            className={cn(
                "glass-card p-3 flex flex-col items-center gap-2 transition-all",
                isAvailable && "cursor-pointer",
                !isAvailable && "cursor-not-allowed opacity-50",
                isSelected && "neon-border-cyan"
            )}
        >
            {/* 로봇 아이콘 */}
            <Bot className={cn(
                "w-6 h-6",
                isSelected ? "neon-icon-cyan" : isAvailable ? "neon-icon-purple" : "text-text-muted"
            )} />

            {/* 로봇 이름 */}
            <span className={cn(
                "text-sm font-medium",
                isSelected ? "text-accent-cyan" : "text-text-primary"
            )}>
                {robot.name}
            </span>

            {/* 배터리 상태 */}
            <div className="flex items-center gap-1">
                <BatteryIcon className={cn("w-4 h-4", batteryColor)} />
                <span className={cn("text-xs", batteryColor)}>
                    {robot.battery}%
                </span>
            </div>
        </button>
    )
}
