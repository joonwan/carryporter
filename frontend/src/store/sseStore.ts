import { create } from "zustand";

interface SSEState {
    // 연결 상태
    isConnected: boolean;
    connectionError: Error | null;

    // 재연결 관련 상태
    reconnectAttempts: number;
    maxReconnectAttempts: number;
    lastConnectedAt: string | null;
    connectionQuality: "good" | "poor" | "disconnected";

    // 액션
    setConnected: (connected: boolean) => void;
    setConnectionError: (error: Error | null) => void;
    incrementReconnectAttempts: () => void;
    resetReconnectAttempts: () => void;
    setConnectionQuality: (quality: "good" | "poor" | "disconnected") => void;
}

export const useSSEStore = create<SSEState>((set) => ({
    isConnected: false,
    connectionError: null,
    reconnectAttempts: 0,
    maxReconnectAttempts: 10,
    lastConnectedAt: null,
    connectionQuality: "disconnected",

    setConnected: (connected) => set({ isConnected: connected }),
    setConnectionError: (error) => set({ connectionError: error }),

    incrementReconnectAttempts: () =>
        set((state) => {
            const newAttempts = state.reconnectAttempts + 1;
            return {
                reconnectAttempts: newAttempts,
                connectionQuality:
                    newAttempts > 3 ? "poor" : state.connectionQuality,
            };
        }),

    resetReconnectAttempts: () =>
        set({
            reconnectAttempts: 0,
            connectionQuality: "good",
            lastConnectedAt: new Date().toISOString(),
        }),

    setConnectionQuality: (quality) => set({ connectionQuality: quality }),
}));
