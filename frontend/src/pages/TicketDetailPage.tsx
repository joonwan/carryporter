import { useNavigate } from 'react-router-dom';
import { useTicketStore } from '../store/ticketStore';
import TicketCard from '../components/ticket/TicketCard';
import { Button } from '@/components/ui/button';
import { AppHeader } from '@/components/layouts/AppHeader';

const TicketDetailPage = () => {
  const navigate = useNavigate();
  const { currentTicket } = useTicketStore();

  // 티켓 정보가 없으면 홈으로 리다이렉트
  if (!currentTicket) {
    navigate('/home');
    return null;
  }

  return (
    <div className="min-h-screen bg-gray-50">
      {/* 헤더 */}
      <AppHeader showCloseButton onClose={() => navigate('/home')} />

      {/* 메인 컨텐츠 */}
      <main className="max-w-md mx-auto px-6 py-6">
        {/* 제목 */}
        <div className="mb-6 animate-fade-in-up">
          <h2 className="text-heading-1 mb-1">
            티켓 상세 ✈️
          </h2>
          <p className="text-body-small">
            등록된 항공권 정보를 확인하세요
          </p>
          
        </div>

        {/* 티켓 카드 */}
        <div className="animate-fade-in-up">
          <TicketCard ticket={currentTicket} variant="detailed" />
        </div>

        {/* 확인 버튼 */}
        <div className="mt-6 animate-fade-in-up">
          <Button
            className="w-full h-12 text-base font-semibold bg-toss-blue-500 hover:bg-toss-blue-600 text-white rounded-xl"
            onClick={() => navigate('/home')}
          >
            확인
          </Button>
        </div>
      </main>
    </div>
  );
};

export default TicketDetailPage;
