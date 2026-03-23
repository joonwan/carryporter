/**
 * 사물함(Locker) 관련 타입 정의
 */

/**
 * 백엔드 응답 타입 (GET /me/lockers/storing)
 */
export interface UserStoringLockerResponse {
  lockerCode: string | null; // 사물함 코드 (예: "abc") 또는 null (사물함 없음)
  updatedAt: string | null;  // 업데이트 시간 (예: "2026-01-29 10:30:00") 또는 null
}

/**
 * 프론트엔드에서 사용하는 현재 사물함 타입
 */
export interface CurrentLocker {
  lockerCode: string; // 사물함 코드 (백엔드 값 그대로 사용)
  updatedAt: string;  // 업데이트 시간 (백엔드 형식 그대로 사용)
}
