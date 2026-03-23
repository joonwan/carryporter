import { cn } from '@/lib/utils'

/** 로봇 상태 데이터 */
interface Robot {
    id: string
    name: string
    status: 'active' | 'moving' | 'idle' | 'maintenance'
    position: { x: number; y: number }
}

// 더미 로봇 데이터
const robots: Robot[] = [
    { id: '1', name: 'CP-01', status: 'active', position: { x: 25, y: 35 } },
    { id: '2', name: 'CP-02', status: 'moving', position: { x: 45, y: 55 } },
    { id: '3', name: 'CP-03', status: 'active', position: { x: 70, y: 40 } },
    { id: '4', name: 'CP-04', status: 'idle', position: { x: 35, y: 70 } },
    { id: '5', name: 'CP-05', status: 'moving', position: { x: 60, y: 65 } },
    { id: '6', name: 'CP-06', status: 'maintenance', position: { x: 80, y: 75 } },
]

// 경로 연결선 데이터
const pathConnections = [
    { from: { x: 25, y: 35 }, to: { x: 45, y: 55 } },
    { from: { x: 45, y: 55 }, to: { x: 70, y: 40 } },
    { from: { x: 45, y: 55 }, to: { x: 35, y: 70 } },
    { from: { x: 35, y: 70 }, to: { x: 60, y: 65 } },
    { from: { x: 60, y: 65 }, to: { x: 80, y: 75 } },
]

/**
 * 공항 지도 실시간 로봇 추적 뷰
 */
export default function MapView() {
    const statusColors: Record<Robot['status'], string> = {
        active: 'bg-status-active',
        moving: 'bg-accent-cyan',
        idle: 'bg-status-warning',
        maintenance: 'bg-status-error',
    }

    const statusLabels: Record<Robot['status'], string> = {
        active: 'Active',
        moving: 'Moving',
        idle: 'Idle',
        maintenance: 'Maint.',
    }

    return (
        <div className="card relative h-[400px] overflow-hidden">
            {/* 지도 배경 */}
            <div className="absolute inset-0 bg-gradient-to-b from-bg-primary/50 to-bg-secondary/50">
                {/* 그리드 패턴 */}
                <div
                    className="absolute inset-0 opacity-10"
                    style={{
                        backgroundImage: `
              linear-gradient(rgba(0, 211, 242, 0.3) 1px, transparent 1px),
              linear-gradient(90deg, rgba(0, 211, 242, 0.3) 1px, transparent 1px)
            `,
                        backgroundSize: '40px 40px',
                    }}
                />
            </div>

            {/* 경로 연결선 */}
            <svg className="absolute inset-0 w-full h-full">
                {pathConnections.map((path, idx) => (
                    <line
                        key={idx}
                        x1={`${path.from.x}%`}
                        y1={`${path.from.y}%`}
                        x2={`${path.to.x}%`}
                        y2={`${path.to.y}%`}
                        stroke="rgba(234, 179, 8, 0.6)"
                        strokeWidth="2"
                        strokeDasharray="5,5"
                    />
                ))}
            </svg>

            {/* 로봇 마커 */}
            {robots.map((robot) => (
                <div
                    key={robot.id}
                    className="absolute transform -translate-x-1/2 -translate-y-1/2 cursor-pointer group"
                    style={{ left: `${robot.position.x}%`, top: `${robot.position.y}%` }}
                >
                    {/* 로봇 아이콘 */}
                    <div className={cn(
                        "w-10 h-10 rounded-lg flex items-center justify-center text-lg",
                        "bg-gradient-to-br from-accent-cyan/80 to-accent-blue/80",
                        "border border-accent-cyan/50 shadow-lg shadow-accent-cyan/20",
                        "group-hover:scale-110 transition-transform"
                    )}>
                        🤖
                    </div>

                    {/* 상태 라벨 */}
                    <div className={cn(
                        "absolute -bottom-6 left-1/2 -translate-x-1/2",
                        "px-2 py-0.5 rounded text-xs whitespace-nowrap",
                        "bg-bg-card border border-border-default"
                    )}>
                        <span className={cn("inline-block w-2 h-2 rounded-full mr-1", statusColors[robot.status])} />
                        {statusLabels[robot.status]}
                    </div>
                </div>
            ))}

            {/* 범례 */}
            <div className="absolute bottom-4 left-4 flex gap-4">
                <div className="flex items-center gap-2 text-xs text-text-secondary">
                    <span className="status-dot status-dot--active" />
                    Available Robots
                </div>
                <div className="flex items-center gap-2 text-xs text-text-secondary">
                    <span className="w-4 border-t-2 border-dashed border-status-warning" />
                    Robot Path
                </div>
            </div>
        </div>
    )
}
