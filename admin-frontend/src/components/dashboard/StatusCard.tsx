import { cn } from '@/lib/utils'

interface StatusCardProps {
    /** 아이콘 (이모지 또는 컴포넌트) */
    icon: React.ReactNode
    /** 표시할 숫자 값 */
    value: number
    /** 카드 라벨 */
    label: string
    /** 상태 변형 */
    variant?: 'default' | 'success' | 'warning' | 'error'
}

/**
 * 로봇 상태를 표시하는 카드 컴포넌트
 */
export default function StatusCard({
    icon,
    value,
    label,
    variant = 'default'
}: StatusCardProps) {
    // 변형별 스타일 매핑
    const variantStyles = {
        default: 'border-border-default',
        success: 'border-status-active/50',
        warning: 'border-status-warning/50',
        error: 'border-status-error/50',
    }

    const valueStyles = {
        default: 'text-text-primary',
        success: 'text-status-active',
        warning: 'text-status-warning',
        error: 'text-status-error',
    }

    return (
        <div
            className={cn(
                "card p-4 flex items-center gap-4",
                variantStyles[variant]
            )}
        >
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
