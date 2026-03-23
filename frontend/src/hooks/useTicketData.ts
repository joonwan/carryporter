import { useState, useEffect } from 'react';
import { useTicketStore } from '@/store/ticketStore';
import { getLatestTicket } from '@/api/ticket.api';

export const useTicketData = () => {
  const { currentTicket, setTicket } = useTicketStore();
  const [isLoading, setIsLoading] = useState(false);

  useEffect(() => {
    const loadTicket = async () => {
      if (currentTicket) return;

      const ticketId = localStorage.getItem('ticketId');
      if (!ticketId) return;

      try {
        setIsLoading(true);
        const ticketData = await getLatestTicket();
        setTicket(ticketData);
      } catch (error) {
        console.error('티켓 정보 조회 실패:', error);
        localStorage.removeItem('ticketId');
      } finally {
        setIsLoading(false);
      }
    };

    loadTicket();
  }, [currentTicket, setTicket]);

  return { currentTicket, isLoading };
};
