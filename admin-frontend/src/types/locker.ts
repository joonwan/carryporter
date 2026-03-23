// types/locker.ts (또는 모달 파일 상단)
export interface LockerResponse {
  lockerId: number;
  lockerCode: string;
  status: 'AVAILABLE' | 'OCCUPIED'; // 백엔드 Enum 값
}
