import { useMissionStore } from '../../store/missionStore';

/**
 * 복귀 중 모달
 * RETURNING 상태에서 표시
 */
export const ReturningModal = () => {
  const { currentMission } = useMissionStore();

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 backdrop-blur-sm">
      <div className="bg-white rounded-2xl p-6 mx-4 w-full max-w-sm shadow-xl animate-fade-in text-center">
        <div className="w-20 h-20 mx-auto mb-6 bg-toss-blue-500 rounded-2xl flex items-center justify-center">
          <svg className="w-10 h-10 text-white animate-pulse" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7" />
          </svg>
        </div>
        <h2 className="text-gray-900 text-xl font-bold mb-2">로봇 복귀 중...</h2>
        <p className="text-gray-500 text-sm mb-4">
          로봇이 스테이션으로 돌아가고 있습니다
        </p>
        
        {/* 로봇 정보 */}
        {currentMission?.robotCode && (
          <div className="bg-gray-50 rounded-xl p-4 text-left">
            <p className="text-xs text-gray-500 mb-1">배정 로봇</p>
            <p className="text-lg font-bold text-gray-900">{currentMission.robotCode}</p>
          </div>
        )}

        {/* 로딩 인디케이터 */}
        <div className="mt-6 flex justify-center gap-1">
          <div className="w-2 h-2 bg-toss-blue-500 rounded-full animate-bounce" style={{ animationDelay: '0ms' }} />
          <div className="w-2 h-2 bg-toss-blue-500 rounded-full animate-bounce" style={{ animationDelay: '150ms' }} />
          <div className="w-2 h-2 bg-toss-blue-500 rounded-full animate-bounce" style={{ animationDelay: '300ms' }} />
        </div>
      </div>
    </div>
  );
};
