import { create } from 'zustand';
import type { TicketInfo } from '../types/ticket.types';
import { ticketStorage } from '@/services/storage/ticketStorage';

interface TicketState {
  // 상태
  currentTicket: TicketInfo | null; // 현재 등록된 티켓 정보

  // 액션
  setTicket: (ticket: TicketInfo) => void; // 티켓 정보 설정
  clearTicket: () => void; // 티켓 정보 초기화
}

export const useTicketStore = create<TicketState>((set) => ({
  // 초기 상태
  currentTicket: null,

  // 티켓 정보 설정 + ticketId localStorage 저장
  setTicket: (ticket: TicketInfo) => {
    // ticketId를 localStorage에 영구 저장 (서비스 레이어 사용)
    if (ticket.ticketId) {
      ticketStorage.saveTicketId(ticket.ticketId);
    }

    set({ currentTicket: ticket });
  },

  // 티켓 정보 초기화 + ticketId localStorage 제거
  clearTicket: () => {
    // localStorage에서 ticketId 제거 (서비스 레이어 사용)
    ticketStorage.clearTicketId();

    set({ currentTicket: null });
  },
}));
