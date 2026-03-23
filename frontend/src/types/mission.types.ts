export interface Location {
    id: number;
    name: string; // "STOP 1", "STOP 2", "GATE 1"
    code: string; // "2", "3", "4"
    type?: "pickup"; // 픽업 장소 (단일 타입으로 통일)
    description?: string;
    icon?: string; // SVG 파일 경로 (예: "/images/STOP1.svg")
}

// 미션 타입 (보관 또는 반납)
export type MissionType = "STORAGE" | "RETURN";

// 미션 상태
export type MissionStatus =
    | "REQUESTED" // 요청됨
    | "ASSIGNED" // 로봇 배정
    | "MOVING" // 이동 중 (사용자에게)
    | "ARRIVED" // 도착
    | "UNLOCKED" // 잠금 해제
    | "LOCKED" // 잠금 (짐 넣기 완료)
    | "RETURNING" // 복귀 중 (로커로)
    | "RETURNED" // 복귀 완료
    | "FINISHED" // 완료
    | "ABORTED"; // 중단됨

// 미션 생성 요청
export interface CreateMissionRequest {
    callLocationId: number; // 호출 위치 아이디 (픽업 위치)
}

export interface CreateMissionResponse {
    missionId: number;
}

// 미션 엔티티
export interface Mission {
    id: string;
    userId: number;
    startLocation: number; // 키 이름 변경 (startLocationId → startLocation)
    endLocation: number; // 키 이름 변경 (endLocationId → endLocation)
    status: MissionStatus;
    missionType?: MissionType; // 보관 또는 반납
    robotCode?: string;
    destination?: string; // 목적지 이름 (예: "1번 정류장", "탑승구 1")
    lockerInfo?: {
        lockerId: string; // "A-127"
        lockerName: string; // "Locker A-127"
    };
    // 반납 시 참조하는 보관 정보
    storedLuggageId?: string;
    createdAt: string;
    updatedAt: string;
}

// SSE 이벤트
export interface MissionStatusEvent {
    missionId: string;
    status: MissionStatus;
    robotCode?: string;
    timestamp: string;
    message?: string; // SSE의 msg 필드 저장 (선택사항)
}

// SSE 이벤트 타입 (9가지)
export type SSEEventType =
    | "Connect"
    | "RobotAssignedEvent"
    | "MissionStartedEvent"
    | "RobotArrivalEvent"
    | "UserAuthSuccessEvent"
    | "MissionUnlockedEvent"
    | "MissionAbortedEvent"
    | "MissionLockedEvent"
    | "RobotReturnedEvent";

// SSE 이벤트 데이터 구조 (공통)
export interface SSEEventData {
    msg: string;
    timestamp: string;
    robotCode?: string; // 로봇 관련 이벤트만 포함
}

// 비밀번호 인증
export interface VerifyMissionRequest {
    password: number; // 4자리 (예: 1234)
}

// 통합 플로우 단계 (UnifiedFlowModal용)
export type UnifiedFlowStep =
    | "LOCK_REQUESTED" // 잠금 요청
    | "CHECKLIST_CONFIRM" // 체크리스트 확인
    | "RETURN_REQUESTED" // 복귀 요청
    | "RETURN_COMPLETE"; // 복귀 완료

// 체크리스트 항목
export interface ChecklistItem {
    id: string;
    label: string;
    checked: boolean;
}
