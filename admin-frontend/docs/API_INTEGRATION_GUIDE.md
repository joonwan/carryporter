# API 연동 가이드

> API 명세서가 완성되면 이 가이드를 참고하여 프론트엔드를 수정하세요.

---

## 목차

1. [수정 대상 파일 목록](#수정-대상-파일-목록)
2. [API 레이어 생성](#api-레이어-생성)
3. [Store 수정 방법](#store-수정-방법)
4. [SSE 연동 방법](#sse-연동-방법)
5. [컴포넌트 수정 방법](#컴포넌트-수정-방법)

---

## 수정 대상 파일 목록

| 우선순위 | 생성/수정 | 파일 | 설명 |
|---------|---------|------|------|
| 1 | 생성 | `src/api/client.ts` | Axios 인스턴스 설정 |
| 2 | 생성 | `src/api/admin.api.ts` | 관리자 API 함수 |
| 3 | 생성 | `src/api/sse.ts` | SSE 연결 유틸리티 |
| 4 | 생성 | `src/types/api.types.ts` | API 요청/응답 타입 |
| 5 | 수정 | `src/store/robotStore.ts` | API 연동 추가 |
| 6 | 수정 | `src/store/lockerStore.ts` | API 연동 추가 |
| 7 | 수정 | `src/pages/RobotsPage.tsx` | API 호출로 변경 |

---

## 1. API 레이어 생성

### src/api/client.ts

```typescript
import axios from 'axios'

const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080'

export const apiClient = axios.create({
    baseURL: API_BASE_URL,
    headers: {
        'Content-Type': 'application/json',
    },
})

// 요청 인터셉터: Access Token 추가
apiClient.interceptors.request.use((config) => {
    const token = localStorage.getItem('accessToken')
    if (token) {
        config.headers.Authorization = `Bearer ${token}`
    }
    return config
})

// 응답 인터셉터: 토큰 만료 시 재발급
apiClient.interceptors.response.use(
    (response) => response,
    async (error) => {
        if (error.response?.status === 401) {
            // 토큰 재발급 로직
            try {
                const refreshToken = localStorage.getItem('refreshToken')
                const res = await axios.post(`${API_BASE_URL}/api/auth/reissue`, {}, {
                    headers: { 'Authorization-Refresh': `Bearer ${refreshToken}` }
                })
                localStorage.setItem('accessToken', res.data.accessToken)
                error.config.headers.Authorization = `Bearer ${res.data.accessToken}`
                return apiClient(error.config)
            } catch {
                // 로그인 페이지로 리다이렉트
                window.location.href = '/login'
            }
        }
        return Promise.reject(error)
    }
)
```

---

### src/types/api.types.ts

```typescript
/** 미션 상태 */
export type MissionStatus = 
    | 'REQUESTED' 
    | 'ASSIGNED' 
    | 'MOVING' 
    | 'ARRIVED' 
    | 'UNLOCKED' 
    | 'LOCKED' 
    | 'RETURNING' 
    | 'RETURNED' 
    | 'FINISHED'

/** 관리자 SSE 이벤트 타입 */
export type AdminSSEEventType = 
    | 'CONNECT' 
    | 'ROBOT_ASSIGNED' 
    | 'ROBOT_ARRIVED' 
    | 'ROBOT_RETURNED'

/** 로봇 배정 이벤트 데이터 */
export interface RobotAssignedEvent {
    missionId: number
    robotCode: string
    status: 'ASSIGNED'
    pickupLocation: string
    timestamp: string
}

/** 로봇 도착 이벤트 데이터 */
export interface RobotArrivedEvent {
    missionId: number
    robotCode: string
    status: 'ARRIVED'
    location: string
    timestamp: string
}

/** 로봇 복귀 이벤트 데이터 */
export interface RobotReturnedEvent {
    missionId: number
    robotCode: string
    status: 'RETURNED'
    station: string
    timestamp: string
}
```

---

### src/api/admin.api.ts

```typescript
import { apiClient } from './client'

/**
 * 관리자 권한 로봇 잠금 해제 (Open Robot)
 * POST /api/admin/missions/{missionId}/unlock
 */
export async function unlockRobot(missionId: number): Promise<void> {
    await apiClient.post(`/api/admin/missions/${missionId}/unlock`)
}

/**
 * 관리자 권한 로봇 잠금 (Close Robot)
 * POST /api/admin/missions/{missionId}/lock
 */
export async function lockRobot(missionId: number): Promise<void> {
    await apiClient.post(`/api/admin/missions/${missionId}/lock`)
}

/**
 * 관리자 권한 로봇 이동 (Dispatch Robot)
 * PATCH /api/admin/missions/{missionId}/move
 */
export async function moveRobot(missionId: number): Promise<void> {
    await apiClient.patch(`/api/admin/missions/${missionId}/move`)
}
```

---

### src/api/sse.ts

```typescript
import { AdminSSEEventType, RobotAssignedEvent, RobotArrivedEvent, RobotReturnedEvent } from '@/types/api.types'

const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080'

type SSEEventHandler = {
    onConnect?: () => void
    onRobotAssigned?: (data: RobotAssignedEvent) => void
    onRobotArrived?: (data: RobotArrivedEvent) => void
    onRobotReturned?: (data: RobotReturnedEvent) => void
    onError?: (error: Event) => void
}

/**
 * 관리자 SSE 연결
 * GET /api/admin/sse/subscribe
 */
export function subscribeAdminSSE(handlers: SSEEventHandler): () => void {
    const token = localStorage.getItem('accessToken')
    const url = `${API_BASE_URL}/api/admin/sse/subscribe`
    
    const eventSource = new EventSource(url, {
        // 인증 헤더는 EventSource에서 지원하지 않음
        // 백엔드에서 쿠키 또는 쿼리 파라미터로 인증 처리 필요
    })

    eventSource.addEventListener('CONNECT', () => {
        handlers.onConnect?.()
    })

    eventSource.addEventListener('ROBOT_ASSIGNED', (event) => {
        const data: RobotAssignedEvent = JSON.parse(event.data)
        handlers.onRobotAssigned?.(data)
    })

    eventSource.addEventListener('ROBOT_ARRIVED', (event) => {
        const data: RobotArrivedEvent = JSON.parse(event.data)
        handlers.onRobotArrived?.(data)
    })

    eventSource.addEventListener('ROBOT_RETURNED', (event) => {
        const data: RobotReturnedEvent = JSON.parse(event.data)
        handlers.onRobotReturned?.(data)
    })

    eventSource.onerror = (error) => {
        handlers.onError?.(error)
    }

    // cleanup 함수 반환
    return () => {
        eventSource.close()
    }
}
```

---

## 2. Store 수정 방법

### robotStore.ts 수정 예시

```typescript
import { create } from 'zustand'
import * as adminApi from '@/api/admin.api'

interface RobotState {
    // ... 기존 상태
    isLoading: boolean
    error: string | null
    
    // API 연동 액션
    unlockRobotAsync: (missionId: number) => Promise<void>
    lockRobotAsync: (missionId: number) => Promise<void>
    dispatchRobotAsync: (missionId: number) => Promise<void>
}

export const useRobotStore = create<RobotState>((set, get) => ({
    // ... 기존 상태
    isLoading: false,
    error: null,

    // 로봇 잠금 해제 (API 호출)
    unlockRobotAsync: async (missionId) => {
        set({ isLoading: true, error: null })
        try {
            await adminApi.unlockRobot(missionId)
            // SSE 이벤트로 상태 업데이트 (또는 직접 업데이트)
            set({ isLoading: false })
        } catch (error) {
            set({ isLoading: false, error: '로봇 잠금 해제 실패' })
            throw error
        }
    },

    // 로봇 잠금 (API 호출)
    lockRobotAsync: async (missionId) => {
        set({ isLoading: true, error: null })
        try {
            await adminApi.lockRobot(missionId)
            set({ isLoading: false })
        } catch (error) {
            set({ isLoading: false, error: '로봇 잠금 실패' })
            throw error
        }
    },

    // 로봇 출발 (API 호출)
    dispatchRobotAsync: async (missionId) => {
        set({ isLoading: true, error: null })
        try {
            await adminApi.moveRobot(missionId)
            set({ isLoading: false })
        } catch (error) {
            set({ isLoading: false, error: '로봇 출발 실패' })
            throw error
        }
    },
}))
```

---

## 3. 컴포넌트 수정 방법

### RobotsPage.tsx 수정 예시

**변경 전 (더미 데이터):**
```tsx
const { unlockRobot, lockRobot, dispatchRobot } = useRobotStore()

<button onClick={() => unlockRobot(robot.id)}>Open Robot</button>
```

**변경 후 (API 호출):**
```tsx
const { unlockRobotAsync, lockRobotAsync, dispatchRobotAsync, isLoading, error } = useRobotStore()

const handleUnlock = async () => {
    if (!selectedRobot?.currentMissionId) return
    try {
        await unlockRobotAsync(selectedRobot.currentMissionId)
        toast.success('로봇 잠금 해제 완료')
    } catch {
        toast.error('로봇 잠금 해제 실패')
    }
}

<button 
    onClick={handleUnlock} 
    disabled={isLoading}
    className={cn("w-full gradient-button", isLoading && "opacity-50")}
>
    {isLoading ? 'Loading...' : 'Open Robot'}
</button>

{error && <p className="text-status-error text-sm">{error}</p>}
```

---

## 4. SSE 연동 방법

### MainLayout.tsx에 SSE 연결 추가

```tsx
import { useEffect } from 'react'
import { subscribeAdminSSE } from '@/api/sse'
import { useRobotStore } from '@/store/robotStore'

export default function MainLayout() {
    const { updateRobotFromSSE } = useRobotStore()

    useEffect(() => {
        // SSE 연결
        const unsubscribe = subscribeAdminSSE({
            onConnect: () => console.log('SSE Connected'),
            onRobotAssigned: (data) => {
                console.log('Robot Assigned:', data)
                updateRobotFromSSE(data)
            },
            onRobotArrived: (data) => {
                console.log('Robot Arrived:', data)
                updateRobotFromSSE(data)
            },
            onRobotReturned: (data) => {
                console.log('Robot Returned:', data)
                updateRobotFromSSE(data)
            },
            onError: (error) => {
                console.error('SSE Error:', error)
            },
        })

        // cleanup
        return () => unsubscribe()
    }, [])

    // ... 기존 코드
}
```

---

## 5. 환경 변수 설정

### .env 파일 생성

```env
VITE_API_URL=http://localhost:8080
```

### .env.production 파일 생성

```env
VITE_API_URL=https://api.carryporter.com
```

---

## 6. 체크리스트

API 연동 시 확인해야 할 항목:

- [ ] `src/api/client.ts` 생성 (Axios 인스턴스)
- [ ] `src/api/admin.api.ts` 생성 (관리자 API 함수)
- [ ] `src/api/sse.ts` 생성 (SSE 연결)
- [ ] `src/types/api.types.ts` 생성 (타입 정의)
- [ ] `robotStore.ts` 수정 (API 호출 추가)
- [ ] `lockerStore.ts` 수정 (API 호출 추가)
- [ ] `RobotsPage.tsx` 수정 (API 호출로 변경)
- [ ] `LockersPage.tsx` 수정 (API 호출로 변경)
- [ ] `MainLayout.tsx` 수정 (SSE 연결)
- [ ] `.env` 파일 생성
- [ ] 에러 핸들링 및 Toast 알림 추가
- [ ] 로딩 상태 UI 추가

---

## 7. API 엔드포인트 매핑

| 현재 함수 | API 엔드포인트 | Method |
|----------|---------------|--------|
| `unlockRobot()` | `/api/admin/missions/{missionId}/unlock` | POST |
| `lockRobot()` | `/api/admin/missions/{missionId}/lock` | POST |
| `dispatchRobot()` | `/api/admin/missions/{missionId}/move` | PATCH |
| SSE 연결 | `/api/admin/sse/subscribe` | GET |

---

**작성일**: 2026년 1월 29일
