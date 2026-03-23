// 티켓 정보 타입 (camelCase 사용)
export interface TicketInfo {
  ticketId: number; // 티켓 고유 ID (백엔드에서 생성)
  flight?: string | null; // 항공편명 (예: "KE932") - OCR 실패 시 null 가능
  gate?: string | null; // 탑승구 (예: "E23") - OCR 실패 시 null 가능
  seat?: string | null; // 좌석 번호 (예: "40B") - OCR 실패 시 null 가능
  boardingTime?: string | null; // 탑승 시간 (예: "21:20") - OCR 실패 시 null 가능
  departureTime?: string | null; // 출발 시간 (예: "22:00") - OCR 실패 시 null 가능
  origin?: string | null; // 출발지 (예: "ROME") - OCR 실패 시 null 가능
  destination?: string | null; // 도착지 (예: "INCHEON") - OCR 실패 시 null 가능
}

// 티켓 스캔 응답 타입 (TicketInfo와 동일)
export type TicketScanResponse = TicketInfo;

// 티켓 카드 variant 타입
export type TicketCardVariant = 'compact' | 'detailed';
