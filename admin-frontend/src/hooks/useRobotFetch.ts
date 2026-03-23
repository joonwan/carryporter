// hooks/useRobotFetch.ts
// 백엔드 API로 로봇 목록을 조회하는 커스턴 훅
// GET /robots          → 전체 목록 조회
// GET /robots/{id}     → 단건 조회 (필요시)

import { useState, useEffect, useCallback } from 'react';

// ─────────────────────────────────────────────
// 백엔드 RobotResponseDto와 동일한 타입
// robotStatus는 백엔드 enum 문자열 그대로 받음
// ─────────────────────────────────────────────
export interface RobotApiResponse {
  id: number;
  robotCode: string;
  macAddress: string;
  robotStatus: string; // 백엔드 RobotStatus enum 값 (예: "AVAILABLE", "WORKING" 등)
}

// ─────────────────────────────────────────────
// 프론트엔드에서 사용하는 통일 타입
// 기존 SSE robots 객체와 같은 구조로 맞춤
// ─────────────────────────────────────────────
export interface RobotApiResponse {
  id: number;
  robotCode: string;
  macAddress: string;
  robotStatus: string;
}

export interface RobotItem {
  id: number;
  robotCode: string;
  macAddress: string;
  status: 'available' | 'working' | 'error' | 'offline';
  x?: number;
  y?: number;
  currentTask?: string;
}

const API_BASE = import.meta.env.DEV ? '' : (import.meta.env.VITE_API_BASE_URL || '');

function mapStatus(backendStatus: string): RobotItem['status'] {
  switch (backendStatus?.toUpperCase()) {
    case 'IDLE':     return 'available';
    case 'BUSY':     return 'working';
    case 'OFFLINE':  return 'offline';
    default:         return 'offline';
  }
}

export async function fetchAllRobots(): Promise<RobotApiResponse[]> {
  const token = localStorage.getItem('accessToken'); 
  const baseUrl = API_BASE ? `/${API_BASE}` : '';
  const url = `${baseUrl}/api/admin/robots`.replace(/\/+/g, '/');

  const res = await fetch(url, {
    method: 'GET',
    headers: {
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json',
    },
  });

  if (!res.ok) {
    if (res.status === 401) throw new Error("인증이 만료되었습니다.");
    throw new Error(`전체 로봇 조회 실패 (${res.status})`);
  }
  return res.json();
}

// ─────────────────────────────────────────────
// 커스텀 훅: useRobotFetch
// ─────────────────────────────────────────────
export function useRobotFetch() {
  const [robots, setRobots]     = useState<RobotItem[]>([]);
  const [loading, setLoading]   = useState(true);
  const [error,   setError]     = useState<string | null>(null);

  const load = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);

      const apiRobots = await fetchAllRobots();

      // ✅ [수정됨] 로봇 겹침 방지 로직 적용
      const mapped: RobotItem[] = apiRobots.map((r, index) => {
        // 1. 간격 설정 (3D 뷰에서 10으로 나누므로, 20은 실제 2만큼 떨어짐)
        const spacing = 20; 
        
        // 2. 전체 로봇이 중앙에 오도록 시작점 계산
        // (로봇 수 - 1) * 간격 / 2 만큼 왼쪽(-)으로 이동시켜 시작
        const totalWidth = (apiRobots.length - 1) * spacing;
        const startX = -totalWidth / 2;

        return {
          id:          r.id,
          robotCode:   r.robotCode,
          macAddress:  r.macAddress,
          status:      mapStatus(r.robotStatus),
          // 3. 인덱스에 따라 x 좌표 할당 (startX 부터 spacing 만큼 띄움)
          x: startX + (index * spacing),
          y: -70, // 메인 스테이션 위치 (상단)
        };
      });

      setRobots(mapped);
    } catch (e) {
      const msg = (e as Error).message;
      setError(msg);
      console.error('❌ useRobotFetch 에러:', msg);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  return { robots, loading, error, refetch: load };
}