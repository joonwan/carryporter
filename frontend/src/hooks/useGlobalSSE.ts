import { useEffect, useRef } from "react";
import { useAuthStore } from "../store/authStore";
import { useSSEStore } from "../store/sseStore";
import { useMissionStore } from "../store/missionStore";
import { subscribeMissionUpdates } from "../api/mission.api";
import type { SSEEventData } from "../types/mission.types";

/**
 * 전역 SSE 관리 훅
 *
 * 로그인 후 SSE 연결을 시작하고, 로그아웃할 때까지 연결을 유지합니다.
 * 미션 생성/종료와 무관하게 연결을 지속하여 이벤트 손실을 방지합니다.
 */
export const useGlobalSSE = () => {
    const { setConnected, setConnectionError, resetReconnectAttempts } =
        useSSEStore();

    const { updateMissionStatus } = useMissionStore();

    const eventSourceRef = useRef<(() => void) | null>(null);

    // 컴포넌트 마운트 시 한 번만 실행.
    useEffect(() => {
        const token = useAuthStore.getState().accessToken;

        if (!token) {
            console.error("[SSE] 토큰이 없습니다. 로그인해주세요.");
            return;
        }

        console.log("[SSE] 구독 시작");

        const unsubscribe = subscribeMissionUpdates({
            onConnect: () => {
                console.log("[SSE] 연결 성공");
                setConnected(true);
                setConnectionError(null);
                resetReconnectAttempts();
            },

            onHeartbeat: () => {
                if (import.meta.env.DEV) console.debug("[SSE] Heartbeat 수신");
            },

            onRobotAssigned: (data: SSEEventData) => {
                console.log("[SSE] 로봇 배정:", data);
                updateMissionStatus({
                    status: "ASSIGNED",
                    robotCode: data.robotCode,
                });
            },

            onMissionStarted: (data: SSEEventData) => {
                console.log("[SSE] 미션 시작:", data);
                updateMissionStatus({
                    status: "MOVING",
                });
            },

            onRobotArrival: (data: SSEEventData) => {
                console.log("[SSE] 로봇 도착:", data);
                updateMissionStatus({
                    status: "ARRIVED",
                });
            },

            onAuthSuccess: (data: SSEEventData) => {
                console.log("[SSE] 사용자 인증 성공:", data);
                updateMissionStatus({
                    status: "UNLOCKED",
                });
            },

            onUnlocked: (data: SSEEventData) => {
                console.log("[SSE] 미션 잠금 해제:", data);
                updateMissionStatus({
                    status: "UNLOCKED",
                });
            },

            onAborted: (data: SSEEventData) => {
                console.log("[SSE] 미션 중단:", data);
                updateMissionStatus({
                    status: "ABORTED",
                });
            },

            onLocked: (data: SSEEventData) => {
                console.log("[SSE] 미션 잠금:", data);
                updateMissionStatus({
                    status: "LOCKED",
                });
            },

            onReturned: (data: SSEEventData) => {
                console.log("[SSE] 로봇 복귀:", data);
                updateMissionStatus({
                    status: "RETURNED",
                });
            },

            onError: (error: Error) => {
                console.error("[SSE] 연결 에러:", error);
                setConnected(false);
                setConnectionError(error);
                // fetchEventSource가 자동으로 재연결 시도
            },
        });

        eventSourceRef.current = unsubscribe;

        return () => {
            console.log("[SSE] 기존 연결 종료");
            if (eventSourceRef.current) {
                eventSourceRef.current();
                eventSourceRef.current = null;
            }
            setConnected(false);
        };
    }, []); // 빈 배열: 컴포넌트 마운트 시 한 번만 실행.

    return {
        isConnected: useSSEStore((state) => state.isConnected),
        reconnectAttempts: useSSEStore((state) => state.reconnectAttempts),
    };
};
