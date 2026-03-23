import apiClient from './axios';
import type { TicketInfo } from '../types/ticket.types';

/**
 * 티켓 스캔 API
 * 이미지 파일을 multipart/form-data로 전송하여 OCR 스캔 수행
 *
 * @param imageFile - 티켓 이미지 파일
 * @returns 스캔된 티켓 정보 (ticketId 포함)
 */
export const scanTicket = async (imageFile: File): Promise<TicketInfo> => {
  const formData = new FormData();
  formData.append('file', imageFile);

  // axios가 FormData를 자동으로 감지하고 올바른 Content-Type 설정
  // (multipart/form-data; boundary=----WebKitFormBoundary...)
  // 수동으로 헤더를 설정하면 boundary 정보가 누락되어 405 에러 발생
  const { data } = await apiClient.post<any>(
    '/api/tickets/scan',
    formData
  );

  // 백엔드 응답이 snake_case일 경우를 대비하여 camelCase로 변환
  // OCR 실패 시 null인 필드는 더미 데이터로 교체 (시연용)
  return {
    ticketId: data.ticket_id ?? data.ticketId,
    flight: data.flight || "KE932",
    gate: data.gate || "E23",
    seat: data.seat || "40B",
    boardingTime: data.boarding_time ?? data.boardingTime ?? "21:20",
    departureTime: data.departure_time ?? data.departureTime ?? "22:00",
    origin: data.origin || "ROME",
    destination: data.destination || "INCHEON",
  };
};

/**
 * 최신 티켓 정보 조회 API
 * localStorage에서 ticketId를 읽어 백엔드에서 티켓 정보를 조회
 *
 * @returns 최신 티켓 정보
 * @throws {Error} ticketId가 없을 경우 에러 발생
 */
export const getLatestTicket = async (): Promise<TicketInfo> => {
  // localStorage에서 ticketId 읽기
  const ticketId = localStorage.getItem('ticketId');

  if (!ticketId) {
    throw new Error('티켓 ID가 없습니다. 먼저 티켓을 스캔해주세요.');
  }

  // GET 요청에 body 포함 (백엔드 요구사항)
  // 주의: HTTP 표준과 맞지 않지만, 백엔드 스펙에 따름
  const { data } = await apiClient.get<any>(
    '/api/me/tickets/latest',
    {
      data: { ticketId: Number(ticketId) }
    }
  );

  // 백엔드 응답이 snake_case일 경우를 대비하여 camelCase로 변환
  return {
    ticketId: data.ticket_id ?? data.ticketId,
    flight: data.flight,
    gate: data.gate,
    seat: data.seat,
    boardingTime: data.boarding_time ?? data.boardingTime,
    departureTime: data.departure_time ?? data.departureTime,
    origin: data.origin,
    destination: data.destination,
  };
};
