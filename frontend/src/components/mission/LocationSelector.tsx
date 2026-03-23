import { cn } from '@/lib/utils';
import type { Location } from '../../types/mission.types';

interface LocationSelectorProps {
  /**
   * 위치 목록 (픽업 장소)
   */
  locations: Location[];
  /**
   * 현재 선택된 위치 ID
   */
  selectedLocationId: number | null;
  /**
   * 위치 선택 핸들러
   */
  onSelect: (locationId: number) => void;
  /**
   * 비활성화 여부
   */
  disabled?: boolean;
}

/**
 * LocationSelector 컴포넌트
 *
 * 픽업 장소 선택 UI를 제공하는 재사용 가능한 컴포넌트
 * 1x4 세로 배치 레이아웃으로 직사각형 카드를 표시하며, SVG 아이콘을 활용합니다.
 */
export const LocationSelector = ({
  locations,
  selectedLocationId,
  onSelect,
  disabled = false,
}: LocationSelectorProps) => {
  return (
    <div className="space-y-1.5">
      {locations.map((location) => (
        <button
          key={location.id}
          type="button"
          onClick={() => onSelect(location.id)}
          disabled={disabled}
          className={cn(
            // 레이아웃
            'w-full p-2.5 rounded-md flex flex-row items-center gap-2.5',

            // 브라우저 기본 outline 제거
            'outline-none focus:outline-none',

            // 애니메이션
            'transition-all duration-200',

            // 선택 상태
            selectedLocationId === location.id
              ? 'bg-white border border-transparent shadow-lg shadow-toss-blue-500/20 scale-[1.02] animate-pulse-glow'
              : 'bg-gray-50 border border-gray-200 hover:bg-white hover:shadow-md hover:border-toss-blue-200',

            // 클릭 피드백
            'active:scale-[0.98]',

            // 비활성화
            disabled && 'opacity-50 cursor-not-allowed'
          )}
          aria-label={`${location.name} 선택`}
        >
          {/* SVG 아이콘 (왼쪽) */}
          {location.icon && (
            <img
              src={location.icon}
              alt={location.name}
              className={cn(
                'w-8 h-8 object-contain transition-all duration-200 flex-shrink-0',
                selectedLocationId === location.id && 'brightness-110 drop-shadow-md'
              )}
              onError={(e) => {
                // SVG 로딩 실패 시 fallback
                e.currentTarget.style.display = 'none';
              }}
            />
          )}

          {/* 장소명 (오른쪽) */}
          <p
            className={cn(
              'text-sm font-semibold flex-1 text-left',
              selectedLocationId === location.id
                ? 'text-toss-blue-500'
                : 'text-gray-700'
            )}
          >
            {location.name}
          </p>
        </button>
      ))}
    </div>
  );
};
