import { useState, useEffect } from 'react';
import { useMissionStore } from '@/store/missionStore';
import type { Mission } from '@/types/mission.types';

type FlowStep = 'none' | 'checklist' | 'returning' | 'complete';

export const useMissionFlow = (currentMission: Mission | null) => {
  const { updateMissionStatus } = useMissionStore();
  const [flowStep, setFlowStep] = useState<FlowStep>('none');

  // 인증 성공 → 체크리스트 모달
  const handleVerificationSuccess = () => {
    updateMissionStatus({ status: 'UNLOCKED' });
    setFlowStep('checklist');
  };

  // 복귀 API 호출 성공 → 복귀 중 모달
  const handleReturnSuccess = () => {
    updateMissionStatus({ status: 'RETURNING' });
    setFlowStep('returning');
  };

  // SSE에서 RETURNED 이벤트 수신 시 완료 모달로 전환
  useEffect(() => {
    if (currentMission?.status === 'RETURNED' && flowStep === 'returning') {
      setFlowStep('complete');
    }
  }, [currentMission?.status, flowStep]);

  return {
    flowStep,
    setFlowStep,
    handleVerificationSuccess,
    handleReturnSuccess,
  };
};
