const TICKET_ID_KEY = 'ticketId';

export const ticketStorage = {
  /**
   * 티켓 ID 저장
   */
  saveTicketId: (ticketId: number): void => {
    localStorage.setItem(TICKET_ID_KEY, String(ticketId));
  },

  /**
   * 티켓 ID 조회
   */
  getTicketId: (): number | null => {
    const id = localStorage.getItem(TICKET_ID_KEY);
    return id ? parseInt(id, 10) : null;
  },

  /**
   * 티켓 ID 삭제
   */
  clearTicketId: (): void => {
    localStorage.removeItem(TICKET_ID_KEY);
  },
};
