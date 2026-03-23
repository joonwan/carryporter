import { VerificationModal } from '@/components/mission/VerificationModal';
import { ChecklistModal } from '@/components/mission/ChecklistModal';
import { ReturningModal } from '@/components/mission/ReturningModal';
import { CompleteModal } from '@/components/mission/CompleteModal';
import type { Mission } from '@/types/mission.types';

type FlowStep = 'none' | 'checklist' | 'returning' | 'complete';

interface MissionModalsProps {
  currentMission: Mission;
  flowStep: FlowStep;
  onVerificationSuccess: () => void;
  onReturnSuccess: () => void;
}

export const MissionModals = ({
  currentMission,
  flowStep,
  onVerificationSuccess,
  onReturnSuccess,
}: MissionModalsProps) => {
  const status = currentMission.status;

  // 모달 표시 조건
  const showVerifyModal = status === 'ARRIVED' && flowStep === 'none';
  const showChecklistModal = flowStep === 'checklist';
  const showReturningModal = flowStep === 'returning';
  const showCompleteModal = flowStep === 'complete';

  return (
    <>
      {showVerifyModal && (
        <VerificationModal
          missionId={Number(currentMission.id)}
          onSuccess={onVerificationSuccess}
        />
      )}
      {showChecklistModal && (
        <ChecklistModal onReturnSuccess={onReturnSuccess} />
      )}
      {showReturningModal && <ReturningModal />}
      {showCompleteModal && <CompleteModal />}
    </>
  );
};
