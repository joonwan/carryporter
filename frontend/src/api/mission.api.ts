import apiClient from './axios';
import { fetchEventSource } from '@microsoft/fetch-event-source';
import type {
  CreateMissionRequest,
  CreateMissionResponse,
  SSEEventData,
} from '../types/mission.types';
import { useAuthStore } from '../store/authStore';

/**
 * 미션 생성 API
 */
export const createMission = async (
  data: CreateMissionRequest
): Promise<CreateMissionResponse> => {
  const response = await apiClient.post<CreateMissionResponse>(
    '/api/missions',
    data
  );
  return response.data;
};

/**
 * SSE 구독 - 실시간 이벤트 수신
 * @microsoft/fetch-event-source 사용으로 탭 비활성화 시에도 연결 유지
 */
export const subscribeMissionUpdates = (
  callbacks: {
    onConnect?: () => void;
    onHeartbeat?: () => void;
    onRobotAssigned?: (data: SSEEventData) => void;
    onMissionStarted?: (data: SSEEventData) => void;
    onRobotArrival?: (data: SSEEventData) => void;
    onAuthSuccess?: (data: SSEEventData) => void;
    onUnlocked?: (data: SSEEventData) => void;
    onAborted?: (data: SSEEventData) => void;
    onLocked?: (data: SSEEventData) => void;
    onReturned?: (data: SSEEventData) => void; // 복귀 완료 이벤트 추가
    onError?: (error: Error) => void;
  }
): (() => void) => {
  const token = useAuthStore.getState().accessToken;
  if (!token) throw new Error('AccessToken이 없습니다.');

  // Nginx 프록시를 사용하므로 개발/프로덕션 모두 상대 경로 사용
  const sseUrl = '/api/sse/subscribe';

  const controller = new AbortController();

  // 이벤트 타입과 콜백 매핑
  const eventCallbacks: Record<string, (data: SSEEventData) => void> = {
    'RobotAssignedEvent': callbacks.onRobotAssigned || (() => {}),
    'MissionStartedEvent': callbacks.onMissionStarted || (() => {}),
    'RobotArrivalEvent': callbacks.onRobotArrival || (() => {}),
    'UserAuthSuccessEvent': callbacks.onAuthSuccess || (() => {}),
    'MissionUnlockedEvent': callbacks.onUnlocked || (() => {}),
    'MissionAbortedEvent': callbacks.onAborted || (() => {}),
    'MissionLockedEvent': callbacks.onLocked || (() => {}),
    'RobotReturnedEvent': callbacks.onReturned || (() => {}), // 복귀 완료 이벤트
  };

  // fetchEventSource 실행
  fetchEventSource(sseUrl, {
    method: 'GET',
    headers: {
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'text/event-stream',
    },
    signal: controller.signal,
    openWhenHidden: true, // 탭이 백그라운드에 있어도 연결 유지

    // 연결 성공
    async onopen(response: any) {
      if (response.ok) {
        if (import.meta.env.DEV) console.log('[SSE] Connected');
        callbacks.onConnect?.();
        return;
      } else {
        if (import.meta.env.DEV) console.error('[SSE] Connection failed:', response.status);
        throw new Error(`HTTP ${response.status}`);
      }
    },

    // 메시지 수신
    onmessage(msg: any) {
      // CONNECT 이벤트
      if (msg.event === 'CONNECT') {
        if (import.meta.env.DEV) console.log('[SSE] CONNECT event');
        callbacks.onConnect?.();
        return;
      }

      // HEARTBEAT 이벤트
      if (msg.event === 'HEARTBEAT' || msg.event === 'heartbeat') {
        if (import.meta.env.DEV) console.debug('[SSE] Heartbeat');
        callbacks.onHeartbeat?.();
        return;
      }

      // 데이터 이벤트 처리
      if (eventCallbacks[msg.event]) {
        try {
          const parsedData: SSEEventData = JSON.parse(msg.data);
          if (import.meta.env.DEV) console.log(`[SSE] ${msg.event}:`, parsedData);
          eventCallbacks[msg.event](parsedData);
        } catch (err) {
          console.error(`[SSE] ${msg.event} 파싱 실패:`, err);
        }
      }
    },

    // 에러 처리
    onerror(err: any) {
      if (import.meta.env.DEV) console.error('[SSE] Error:', err);

      // AbortError는 정상 종료 → 재연결하지 않음
      if (err instanceof Error && err.name === 'AbortError') {
        throw err;
      }

      callbacks.onError?.(new Error('SSE connection error'));
      // throw하지 않으면 fetch-event-source가 자동 재연결함
    },

    // 연결 종료
    onclose() {
      if (import.meta.env.DEV) console.log('[SSE] Connection closed');
    }
  });

  // cleanup 함수 반환
  return () => {
    if (import.meta.env.DEV) console.log('[SSE] Disconnecting');
    controller.abort();
  };
};

/**
 * 사용자 잠금 해제 (비밀번호 인증)
 */
export const verifyMission = async (
  missionId: number,
  password: number
): Promise<void> => {
  await apiClient.post(`/api/auth/unlock`, { missionId, password });
};

/**
 * 사물함 잠금
 */
export const lockMission = async (missionId: number): Promise<void> => {
  await apiClient.post(`/api/auth/lock`,{missionId});
};

/**
 * 로봇 복귀 요청
 */
export const returnMission = async (missionId: number): Promise<void> => {
  await apiClient.post(`/api/missions/${missionId}/return`);
};