import { useNavigate } from 'react-router-dom';
import { Logo } from '@/components/common/Logo';
import { cn } from '@/lib/utils';

interface AppHeaderProps {
  rightElement?: React.ReactNode;
  showBackButton?: boolean;
  showCloseButton?: boolean;
  onBack?: () => void;
  onClose?: () => void;
  className?: string;
}

export const AppHeader = ({
  rightElement,
  showBackButton = false,
  showCloseButton = false,
  onBack,
  onClose,
  className,
}: AppHeaderProps) => {
  const navigate = useNavigate();

  const handleBack = () => {
    if (onBack) {
      onBack();
    } else {
      navigate(-1);
    }
  };

  const handleClose = () => {
    if (onClose) {
      onClose();
    } else {
      navigate('/home');
    }
  };

  return (
    <header className={cn('bg-gray-50 pt-safe', className)}>
      <div className="max-w-md mx-auto px-6 py-4">
        <div className="flex items-center justify-between">
          {/* 뒤로가기 버튼 */}
          {showBackButton && (
            <button
              onClick={handleBack}
              className="p-2 hover:bg-gray-200 rounded-lg transition-colors"
              aria-label="뒤로가기"
            >
              <svg className="w-6 h-6 text-gray-700" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7" />
              </svg>
            </button>
          )}

          {/* 로고 + 타이틀 */}
          <div className="flex items-center gap-3">
            <Logo size="md" variant="default" />
            <h1 className="text-heading-3 font-beckman">CARRY PORTER</h1>
          </div>

          {/* 우측 요소 (커스텀) */}
          {rightElement && <div>{rightElement}</div>}

          {/* 닫기 버튼 */}
          {showCloseButton && (
            <button
              onClick={handleClose}
              className="p-2 hover:bg-gray-200 rounded-lg transition-colors"
              aria-label="닫기"
            >
              <svg className="w-6 h-6 text-gray-700" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
              </svg>
            </button>
          )}

          {/* 버튼이 없고 rightElement도 없으면 빈 공간 */}
          {!showBackButton && !showCloseButton && !rightElement && <div className="w-10" />}
        </div>
      </div>
    </header>
  );
};
