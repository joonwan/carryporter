import { create } from 'zustand'

export interface RobotData {
  id: string;
  status: 'working' | 'available';
  battery: number;
  position: { x: number; y: number };
  message?: string;
}

interface SseStore {
  // SSE 데이터
  robots: RobotData[];
  isConnected: boolean;
  lastMessage: string | null;
  alertEvent: string | null; // 알림 전용 필드 추가
  setAlertEvent: (event: string | null) => void;

  // Actions
  setRobots: (robots: RobotData[]) => void;
  setIsConnected: (connected: boolean) => void;
  setLastMessage: (message: string | null) => void;
  updateRobotStatus: (robotId: string, updates: Partial<RobotData>) => void;
}

export const useSseStore = create<SseStore>((set) => ({
  // 초기 상태
  robots: [],
  isConnected: false,
  lastMessage: null,
  alertEvent: null,

  // Actions
  setRobots: (robots) => set({ robots }),

  setIsConnected: (connected) => set({ isConnected: connected }),

  setLastMessage: (message) => set({ lastMessage: message }),
  
  setAlertEvent: (event) => set({ alertEvent: event }),

  updateRobotStatus: (robotId, updates) => set((state) => ({
    robots: state.robots.map((robot) =>
      robot.id === robotId ? { ...robot, ...updates } : robot
    ),
  })),
}))
