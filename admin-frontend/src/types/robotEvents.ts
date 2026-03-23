// types/robotEvents.ts
export interface RobotAssignedEvent {
  userId: number;
  missionId: number;
  robotCode: string;
  callLocationName: string; // 호출지 (예: 1층 로비)
  locker_code: string | null; // FIRST일 때 null, RECALL일 때 값 있음
  requestType: 'FIRST' | 'RECALL'; // 대문자로 온다고 가정
}


export interface RobotReturnedAdminEvent {
  userId: number;
  robotCode: string;
  missionId: number;
  lockerId: number; // 사물함 ID
  lockerCode: string; // 관리자가 넣어야 할 사물함
  message: string;
}

// API 요청 타입
export type AdminActionType = 'STORAGE' | 'RETURN';