import { cn } from '@/lib/utils'

interface StatCardProps {
    /** 아이콘 (이모지 또는 컴포넌트) */
    icon: React.ReactNode
    /** 표시할 숫자 값 */
    value: number
    /** 카드 라벨 */
    label: string
    /** 상태 변형 */
    variant?: 'default' | 'error'
}

/**
 * 사용자 통계를 표시하는 카드 컴포넌트
 */
export default function StatCard({
    icon,
    value,
    label,
    variant = 'default'
}: StatCardProps) {
    const valueStyles = {
        default: 'text-text-primary',
        error: 'text-status-error',
    }

    return (
        <div className="card p-4 flex items-center gap-4 border-border-default">
            {/* 아이콘 영역 */}
            <div className="w-10 h-10 flex items-center justify-center bg-bg-secondary rounded-lg text-xl">
                {icon}
            </div>

            {/* 정보 영역 */}
            <div className="flex flex-col">
                <span className={cn("text-2xl font-bold", valueStyles[variant])}>
                    {value}
                </span>
                <span className="text-text-secondary text-xs">
                    {label}
                </span>
            </div>
        </div>
    )
}
