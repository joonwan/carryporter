# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

---

## 언어 및 커뮤니케이션 규칙

- **기본 응답 언어**: 한국어
- **코드 주석**: 한국어로 작성
- **커밋 메시지**: 한국어로 작성
- **문서화**: 한국어로 작성
- **변수명/함수명**: 영어 (코드 표준 준수)

---

## 코드 작성 규칙

### 최신 기술 스택 활용
- **React 19**: 최신 React 기능 사용 (Suspense, Transitions, Server Components 고려)
- **TypeScript 5.9**: 최신 타입 시스템 활용
- **Tailwind CSS v4**: 최신 유틸리티 클래스 및 CSS 변수 활용
- **shadcn/ui**: Radix UI 기반 접근성 높은 UI 컴포넌트 시스템
  - 복사-붙여넣기 방식으로 컴포넌트 소스를 직접 소유
  - Tailwind CSS와 완벽한 통합
  - 커스터마이징 가능한 컴포넌트 (Dialog, Button, Input, Card 등)
- **Context7 활용**: Context7 MCP를 사용하여 최신 라이브러리 문서 참조

### 현업 코딩 스타일
- **명확한 네이밍**: 함수와 변수명은 의도를 명확히 표현
- **단일 책임 원칙**: 각 함수/컴포넌트는 하나의 역할만 수행
- **재사용성**: 공통 로직은 커스텀 훅이나 유틸리티로 분리
- **타입 안정성**: `any` 타입 사용 금지, 모든 타입 명시
- **에러 핸들링**: try-catch로 에러를 처리하고 사용자에게 명확한 피드백 제공

### 코드 문서화 (CODE_REFERENCE.md)

**중요**: 모든 구현 후 다음 내용을 `docs/CODE_REFERENCE.md`에 추가 작성:

1. **코드 동작 원리**
   - 함수/컴포넌트가 어떻게 작동하는지 세세하게 분석
   - 주요 로직의 단계별 설명
   - 데이터 흐름 다이어그램 (텍스트 형태)

2. **트러블슈팅**
   - 발생했던 에러와 해결 방법
   - 에러 메시지와 원인 분석
   - 해결 과정에서 시도한 방법들

3. **성능 최적화**
   - 기존 방식의 문제점
   - 개선된 방식과 그 이유
   - 성능 향상 수치 (가능한 경우)
   - Before/After 코드 비교

4. **학습 포인트**
   - 이 코드에서 배울 수 있는 개념
   - 실무 활용 사례
   - 추천 학습 자료

**작성 형식 예시**:
```markdown
## [기능명] 구현

### 동작 원리
1. 사용자가 버튼 클릭
2. API 호출 (POST /api/missions)
3. 응답 데이터를 Store에 저장
4. SSE 연결 시작

### 트러블슈팅
**문제**: SSE 연결이 페이지 이동 후에도 유지됨
**원인**: useEffect cleanup 함수 누락
**해결**: return () => unsubscribe() 추가

### 성능 최적화
**기존 방식**: 1초마다 폴링 (불필요한 네트워크 요청)
**개선 방식**: SSE 사용 (서버 푸시)
**성능 향상**: 네트워크 요청 95% 감소, 실시간성 100% 향상
```

---

## 프로젝트 개요

**CARRY PORTER Admin Dashboard**는 공항 로봇 관리 시스템의 관리자 대시보드입니다.

### 관리자 플로우

```
1. 사용자가 로봇 호출
   ↓
2. 시스템이 로봇 배정 → 관리자 알림
   ↓
3. 관리자가 사물함 선택 (Lockers 페이지)
   ↓
4. 관리자가 로봇 열기 (Robots 페이지 → Open Robot)
   ↓
5. 관리자가 짐을 로봇에 넣음
   ↓
6. 관리자가 로봇 닫기 (Close Robot)
   ↓
7. 관리자가 로봇 출발 (Dispatch Robot)
   ↓
8. 로봇이 목적지로 이동
```

### 페이지 구성

| 페이지 | 경로 | 설명 |
|--------|------|------|
| Dashboard | `/` | 로봇/사물함 상태 개요 |
| Robots | `/robots` | 로봇 관리, 열기/닫기/출발 |
| Lockers | `/lockers` | 사물함 관리, 할당/해제 |

### 상태 관리 (Zustand)

- `themeStore.ts` - Light/Dark 모드
- `robotStore.ts` - 로봇 상태, 잠금 제어
- `lockerStore.ts` - 사물함 상태, 할당
   ├─ 가용 로봇 대수 표시 ✅
   └─ 최근 호출 구역 표시 ✅
   ↓
5. [로봇 호출] → 미션 생성 ✅ 구현됨
   ├─ 보관/반납 선택 ✅
   ├─ 6개 정류장 선택 ✅
   ├─ 미션 생성 API ✅
   └─ SSE 실시간 추적 시작 ✅
   ↓
6. 미션 추적 화면 ✅ 구현됨
   ├─ 실시간 상태 업데이트 (SSE) ✅
   ├─ 4자리 PIN 인증 ✅
   ├─ 무게 측정 애니메이션 ✅
   └─ 보관함 저장 (localStorage) ✅
```

### 핵심 도메인
- **인증 시스템**: Mattermost 기반 2단계 인증 (Email + Password → PIN)
- **티켓 관리**: OCR 기반 항공권 스캔 및 정보 저장
- **미션 관리**: 로봇 호출, 실시간 추적 (SSE), 비밀번호 인증
- **관리자 대시보드**: 로봇 제어, 실시간 이벤트 모니터링

**중요**: 티켓 시스템은 유지하고, 로봇 호출 기능을 추가 구현. `docs/next-step.md` 참조.

---

## 개발 환경 설정 및 명령어

### 필수 조건
- Node.js 18.0.0 이상
- npm 9.0.0 이상

### 개발 서버
```bash
npm run dev          # 개발 서버 시작 (http://localhost:5173)
```

### 빌드
```bash
npm run build        # TypeScript 컴파일 후 Production 빌드
npm run preview      # 빌드 결과 미리보기
```

### 코드 품질
```bash
npm run lint         # ESLint 실행 (TypeScript + React)
```

### TypeScript 컴파일
```bash
tsc -b              # TypeScript 빌드만 실행 (타입 체크)
```

### shadcn/ui 컴포넌트 추가
```bash
npx shadcn@latest add button     # Button 컴포넌트 추가
npx shadcn@latest add dialog     # Dialog 컴포넌트 추가
npx shadcn@latest add card       # Card 컴포넌트 추가
npx shadcn@latest add input      # Input 컴포넌트 추가
npx shadcn@latest add badge      # Badge 컴포넌트 추가
npx shadcn@latest add toast      # Toast 컴포넌트 추가

# 여러 컴포넌트 한번에 추가
npx shadcn@latest add button dialog card input
```

---

## 아키텍처 핵심 개념

### 1. 상태 관리 전략 (Zustand)

**네 가지 독립적인 Store:**

- **authStore** (`src/store/authStore.ts`) ✅ 구현됨
  - Access/Refresh Token 관리
  - 사용자 정보 (User)
  - 로그인/로그아웃 액션
  - **주의**: refreshToken은 localStorage에 영구 저장, accessToken은 메모리만

- **ticketStore** (`src/store/ticketStore.ts`) ✅ 구현됨
  - 티켓 정보 (OCR 결과)
  - 스캔 상태 관리
  - **중요**: 삭제하지 말 것! 티켓 시스템은 유지

- **missionStore** (`src/store/missionStore.ts`) ✅ 구현됨
  - 현재 미션 상태 (currentMission)
  - SSE 연결 상태 (sseConnected)
  - 보관된 짐 목록 (storedLuggages)
  - localStorage 영구 저장
  - 보관/반납 액션

- **adminStore** (`src/store/adminStore.ts`) 🆕 구현 필요 (선택)
  - 활성 미션 목록
  - 관리자 SSE 이벤트 히스토리
  - 로봇 제어 상태

**패턴**: Store는 비즈니스 로직을 포함하지 않고 순수 상태만 관리. 비즈니스 로직은 API 레이어와 컴포넌트에서 처리.

---

### 2. API 레이어 아키텍처

**구조**: `src/api/` 폴더 내 도메인별 분리

- **axios.ts** ✅ 구현됨
  - Request Interceptor: 자동 Bearer Token 주입
  - Response Interceptor: 401 에러 시 토큰 재발급 시도
  - **중요**: Refresh Token 만료 시 자동 로그아웃 및 `/login` 리다이렉트

- **auth.api.ts** ✅ 구현됨
  - `login()`: 이메일 + 비밀번호 로그인
  - `verifyPin()`: PIN 인증
  - `logout()`: 로그아웃
  - `adminLogin()`: 관리자 로그인

- **ticket.api.ts** ✅ 구현됨
  - `scanTicket()`: OCR 티켓 스캔
  - `getLatestTicket()`: 최신 티켓 조회

- **mission.api.ts** ✅ 구현됨
  - `createMission()`: 미션 생성 (Mock API)
  - `subscribeMissionUpdates()`: SSE 구독 (EventSource)
  - `verifyMission()`: 비밀번호 인증
  - `getMissionStatus()`: 현재 상태 조회
  - Mock API 모드 (`mission.api.mock.ts`)

**SSE 패턴**: EventSource를 사용한 실시간 통신. Cleanup 함수를 반환하여 컴포넌트 unmount 시 연결 종료.

---

### 3. 인증 플로우 (2단계)

**현재 플로우** ✅ 구현됨:
1. LoginPage: 이메일 + 비밀번호 + 비밀번호 확인 + 약관 동의
2. `login()` API 호출 → PIN 3개 반환
3. PinVerificationPage: 3개 PIN 중 Mattermost로 받은 것과 같은 번호 선택
4. `verifyPin()` API 호출 → 토큰 발급
5. AuthStore에 토큰 저장 → `/ticket/scan` 리다이렉트

**토큰 관리**:
- Access Token: Zustand Store (메모리)
- Refresh Token: localStorage 영구 저장 (향후 구현 예정)
- 401 에러 시 자동 로그아웃 (`axios.ts` interceptor)

**Protected Routes**: `src/routes/ProtectedRoute.tsx`에서 `isAuthenticated` 체크. 미인증 시 `/login` 리다이렉트.

---

### 4. 티켓 스캔 시스템 (OCR)

**구현 위치**: `src/pages/TicketScanPage.tsx`, `src/components/ticket/WebcamScanner.tsx` ✅ 구현됨

**플로우**:
1. 웹캠 활성화 (react-webcam 사용)
2. 사용자가 "스캔" 버튼 클릭
3. 현재 프레임을 base64로 캡처
4. base64 → File 객체 변환
5. `scanTicket()` API 호출 (multipart/form-data)
6. 백엔드에서 OCR 처리 후 티켓 정보 반환
7. ticketStore에 저장
8. 성공 모달 표시 후 `/home` 리다이렉트

**주요 타입**:
```typescript
interface TicketInfo {
  flight: string;         // 항공편명 (예: "KE932")
  gate: string;          // 탑승구 (예: "E23")
  seat: string;          // 좌석 번호 (예: "40B")
  boarding_time: string; // 탑승 시간
  departure_time: string;// 출발 시간
  origin: string;        // 출발지
  destination: string;   // 도착지
}
```

---

### 5. 실시간 통신 (SSE)

**구현 위치**: `src/hooks/useMissionSSE.ts` ✅ 구현됨, `src/hooks/useAdminSSE.ts` 🆕 구현 필요 (선택)

**패턴**:
```typescript
// EventSource 생성 → 이벤트 리스너 등록 → Cleanup 함수 반환
const unsubscribe = subscribeMissionUpdates(missionId, {
  onConnect: () => setConnected(true),
  onStatus: (status) => updateMissionStatus(status),
  onError: (error) => setConnectionError(error),
});

// useEffect cleanup
return () => unsubscribe();
```

**이벤트 타입**:
- 사용자 SSE: `CONNECT`, `STATUS` (REQUESTED, ASSIGNED, MOVING, ARRIVED, UNLOCKED, LOCKED, RETURNING, RETURNED, FINISHED)
- 관리자 SSE: `CONNECT`, `ROBOT_ASSIGNED`, `ROBOT_ARRIVED`, `ROBOT_RETURNED`

**주의사항**:
- SSE 연결은 missionId가 있을 때만 실행
- `useEffect` 의존성 배열에 missionId, 콜백 함수들 포함
- 컴포넌트 unmount 시 반드시 연결 종료

---

### 6. 타입 정의 전략

**위치**: `src/types/` 도메인별 분리

- **auth.types.ts** ✅ 구현됨
  - User, LoginRequest, LoginResponse, VerifyPinRequest, AuthResponse

- **ticket.types.ts** ✅ 구현됨
  - TicketInfo, TicketScanResponse

- **mission.types.ts** 🆕 구현 필요
  - Mission, MissionStatus, MissionStatusEvent, AdminMissionEvent

**패턴**:
- API 요청/응답 타입은 백엔드 스펙 (`docs/api-spec.md`)과 1:1 매칭
- Enum 대신 Union Type 사용 (`type MissionStatus = 'REQUESTED' | 'ASSIGNED' | ...`)
- 모든 interface는 `export` 처리
- 선택적 필드는 `?` 사용

---

### 7. 라우팅 구조

**파일**: `src/routes/index.tsx`

**현재 계층** ✅:
```
/ (SplashPage)
/login (LoginPage)
/login/verify (PinVerificationPage)

Protected Routes:
  /ticket/scan (TicketScanPage)
  /home (HomePage)
  /ticket/detail (TicketDetailPage)
```

**추가 필요** 🆕:
```
Protected Routes:
  /mission/create (MissionCreatePage)
  /mission/track (MissionTrackPage)

Admin Routes (ROLE_ADMIN):
  /admin (AdminDashboardPage)
```

**Protected Route 로직**: `ProtectedRoute.tsx`에서 Outlet 패턴 사용. 미인증 시 Navigate to /login.

---

### 8. 컴포넌트 구조

**컴포넌트 계층**:
- **ui/**: shadcn/ui 컴포넌트 (Button, Dialog, Card, Input 등)
  - CLI로 생성된 재사용 가능 UI primitives
  - 직접 수정 가능 (소스 코드를 소유)
  - 예: `@/components/ui/button`, `@/components/ui/dialog`

- **common/**: 자체 공통 컴포넌트 (shadcn/ui로 대체 가능)
  - 기존: Button, Input, Checkbox
  - 향후: shadcn/ui로 점진적 마이그레이션 권장

- **layouts/**: 레이아웃 컴포넌트 ✅
  - AuthLayout (중앙 정렬 레이아웃)

- **ticket/**: 티켓 도메인 컴포넌트 ✅
  - TicketCard, WebcamScanner, ScanSuccessModal

**추가 필요** 🆕:
- **mission/**: MissionStatusCard, MissionTimeline, VerificationModal
- **admin/**: EventLog, MissionCard

**컴포넌트 선택 가이드**:
```typescript
// ✅ Good: shadcn/ui 컴포넌트 우선 사용
import { Button } from "@/components/ui/button"
import { Dialog } from "@/components/ui/dialog"

// ✅ Good: 도메인 특화 로직은 자체 컴포넌트
import { TicketCard } from "@/components/ticket/TicketCard"
import { MissionTimeline } from "@/components/mission/MissionTimeline"

// ❌ Bad: shadcn/ui에 있는데 자체 구현
import { Button } from "@/components/common/Button" // 대신 ui/button 사용
```

**패턴**:
- Props는 interface로 명시
- children은 `React.ReactNode` 타입
- 이벤트 핸들러는 `onClick={handleClick}` 형태
- shadcn/ui 컴포넌트는 `forwardRef` 패턴 사용

---

## 스타일링 (Tailwind CSS v4 + shadcn/ui)

### Tailwind CSS v4 설정

**설정**: `postcss.config.js`에 `@tailwindcss/postcss` 플러그인 사용

**import 방식**: `src/index.css`에 `@import "tailwindcss";`

**주의사항**:
- Tailwind v4는 설정 파일 없이 CSS에서 직접 import
- `tailwind.config.js`는 커스텀 테마용으로만 사용
- 스타일 미적용 시 postcss.config.js 확인 후 서버 재시작

**권장 패턴**:
```typescript
// 조건부 클래스는 템플릿 리터럴 사용
className={`px-4 py-2 ${isActive ? 'bg-blue-500' : 'bg-gray-300'}`}

// 많은 조건부 클래스는 clsx 또는 객체 방식
className={cn(
  'px-4 py-2',
  isActive && 'bg-blue-500',
  !isActive && 'bg-gray-300'
)}
```

### shadcn/ui 컴포넌트 시스템

**개념**: shadcn/ui는 npm 패키지가 아닌 "복사-붙여넣기" 방식의 컴포넌트 시스템입니다.

**특징**:
- 컴포넌트 소스 코드를 프로젝트에 직접 복사 (`src/components/ui/`)
- Radix UI 기반의 접근성 높은 primitives 사용
- Tailwind CSS로 스타일링되어 커스터마이징 용이
- TypeScript 완벽 지원

**설치 방법**:
```bash
# shadcn/ui CLI를 사용하여 컴포넌트 추가
npx shadcn@latest add button
npx shadcn@latest add dialog
npx shadcn@latest add card
```

**사용 예시**:
```typescript
// ✅ Good: shadcn/ui 컴포넌트 사용
import { Button } from "@/components/ui/button"
import { Dialog, DialogContent, DialogHeader } from "@/components/ui/dialog"

export function MyComponent() {
  return (
    <Dialog>
      <DialogContent>
        <DialogHeader>제목</DialogHeader>
        <Button>확인</Button>
      </DialogContent>
    </Dialog>
  )
}
```

**커스터마이징**:
```typescript
// shadcn/ui 컴포넌트는 소스 코드를 직접 수정 가능
// src/components/ui/button.tsx에서 variant 추가
const buttonVariants = cva(
  "...",
  {
    variants: {
      variant: {
        default: "bg-primary text-primary-foreground",
        destructive: "bg-destructive text-destructive-foreground",
        custom: "bg-purple-500 text-white", // 🆕 커스텀 variant 추가
      }
    }
  }
)
```

**주의사항**:
- shadcn/ui 컴포넌트는 `src/components/ui/` 폴더에 저장
- 기존 컴포넌트(`src/components/common/`)와 공존 가능
- shadcn/ui 우선 사용, 커스텀이 필요한 경우에만 자체 컴포넌트 작성
- `@/` alias는 `src/` 경로를 가리킴 (`tsconfig.json`에서 설정)

---

## 폴더 구조 상세

```
src/
├── api/              # 도메인별 API 함수 (axios 클라이언트)
│   ├── axios.ts      # HTTP 클라이언트 + 인터셉터 ✅
│   ├── auth.api.ts   # 인증 API ✅
│   ├── ticket.api.ts # 티켓 스캔 API ✅
│   └── mission.api.ts # 미션 API ✅
├── components/
│   ├── ui/           # shadcn/ui 컴포넌트 (CLI로 생성) ✅
│   │   ├── button.tsx
│   │   ├── dialog.tsx
│   │   ├── card.tsx
│   │   └── input.tsx
│   ├── common/       # 자체 공통 컴포넌트 ✅
│   ├── layouts/      # AuthLayout ✅
│   ├── ticket/       # 티켓 관련 컴포넌트 ✅
│   │   ├── TicketCard.tsx
│   │   ├── WebcamScanner.tsx
│   │   └── ScanSuccessModal.tsx
│   ├── mission/      # 미션 관련 컴포넌트 ✅
│   │   ├── VerificationModal.tsx     # PIN 인증 모달
│   │   ├── MissionTypeSelector.tsx   # 보관/반납 선택
│   │   ├── StorageFlowModal.tsx      # 보관 플로우
│   │   └── ReturnFlowModal.tsx       # 반납 플로우
│   └── admin/        # 관리자 컴포넌트 🆕 (선택)
├── hooks/            # 커스텀 훅
│   ├── useMissionSSE.ts        # SSE 구독 훅 ✅
│   └── useWeightCountUp.ts     # 무게 카운트업 애니메이션 ✅
├── lib/              # shadcn/ui 유틸리티 (utils.ts - cn 함수 등) ✅
├── pages/            # 페이지 컴포넌트
│   ├── LoginPage.tsx           # 로그인 ✅
│   ├── PinVerificationPage.tsx # PIN 인증 ✅
│   ├── HomePage.tsx            # 홈 화면 ✅
│   ├── TicketScanPage.tsx      # 티켓 스캔 ✅
│   ├── TicketDetailPage.tsx    # 티켓 상세 ✅
│   ├── MissionCreatePage.tsx   # 미션 생성 ✅
│   └── MissionTrackPage.tsx    # 미션 추적 ✅
├── routes/           # ProtectedRoute ✅
├── store/            # 상태 관리
│   ├── authStore.ts    # 인증 상태 ✅
│   ├── ticketStore.ts  # 티켓 상태 ✅
│   ├── missionStore.ts # 미션 상태 ✅
│   └── adminStore.ts   # 관리자 상태 🆕 (선택)
├── types/            # 타입 정의
│   ├── auth.types.ts    # 인증 타입 ✅
│   ├── ticket.types.ts  # 티켓 타입 ✅
│   └── mission.types.ts # 미션 타입 ✅
└── utils/            # 유틸리티
    ├── validation.ts   # 검증 함수 ✅
    └── imageUtils.ts   # 이미지 처리 ✅

docs/                 # 프로젝트 문서
├── api-spec.md       # 백엔드 API 명세서 (필수 참조)
├── next-step.md      # 구현 가이드 (필수 참조)
├── DEVELOPMENT_GUIDE.md
├── CODE_REFERENCE.md # 코드 동작 원리 및 학습 자료
└── TECH_STACK.md
```

---

## 환경 변수

**파일**: `.env.development`, `.env.production`

**주요 변수**:
```bash
VITE_API_BASE_URL=http://localhost:8080  # 백엔드 API 서버
```

**사용법**: `import.meta.env.VITE_API_BASE_URL`

---

## 중요한 개발 컨벤션

### 1. API 호출 패턴
```typescript
// ❌ Bad: 컴포넌트에서 직접 axios 호출
const response = await axios.post('/api/auth/login', data);

// ✅ Good: API 레이어 함수 사용
const response = await login(data);
```

### 2. 상태 업데이트 패턴
```typescript
// ❌ Bad: Store에서 직접 API 호출
const useAuthStore = create((set) => ({
  login: async (data) => {
    const response = await loginAPI(data);
    set({ user: response.user });
  }
}));

// ✅ Good: 컴포넌트에서 API 호출 후 Store 업데이트
const response = await login(data);
authStore.login(response.accessToken, response.user);
```

### 3. SSE Cleanup
```typescript
// ✅ Good: useEffect cleanup으로 EventSource 종료
useEffect(() => {
  if (!missionId) return;

  const unsubscribe = subscribeMissionUpdates(missionId, callbacks);
  return () => unsubscribe();
}, [missionId]);
```

### 4. 에러 처리
```typescript
// ✅ Good: try-catch로 에러 처리 및 사용자 피드백
try {
  setIsLoading(true);
  await createMission(data);
  navigate('/mission/track');
} catch (error) {
  setError('미션 생성에 실패했습니다.');
  console.error('Mission creation failed:', error);
} finally {
  setIsLoading(false);
}
```

### 5. shadcn/ui 컴포넌트 사용 패턴
```typescript
// ❌ Bad: 자체 컴포넌트를 불필요하게 재작성
const Button = ({ children, onClick }) => (
  <button className="px-4 py-2 bg-blue-500" onClick={onClick}>
    {children}
  </button>
);

// ✅ Good: shadcn/ui 컴포넌트 활용
import { Button } from "@/components/ui/button"

<Button variant="default" size="lg" onClick={handleClick}>
  클릭
</Button>

// ✅ Good: 커스터마이징이 필요하면 className으로 확장
<Button className="w-full mt-4">
  전체 너비 버튼
</Button>

// ✅ Good: 복잡한 UI는 shadcn/ui 조합
import { Dialog, DialogContent, DialogHeader, DialogTitle } from "@/components/ui/dialog"
import { Button } from "@/components/ui/button"

<Dialog open={isOpen} onOpenChange={setIsOpen}>
  <DialogContent>
    <DialogHeader>
      <DialogTitle>미션 생성</DialogTitle>
    </DialogHeader>
    <div className="space-y-4">
      {/* 폼 내용 */}
    </div>
    <Button onClick={handleSubmit}>생성</Button>
  </DialogContent>
</Dialog>
```

### 6. Context7 활용
```typescript
// 최신 React 19 패턴 확인 시
// Context7 MCP를 사용하여 React 공식 문서 조회
// 예: "React 19 useTransition hook usage"

// 최신 Tailwind CSS v4 문법 확인 시
// Context7 MCP를 사용하여 Tailwind 문서 조회
// 예: "Tailwind CSS v4 container queries"

// shadcn/ui 컴포넌트 사용법 확인 시
// Context7 MCP를 사용하여 shadcn/ui 문서 조회
// 예: "shadcn/ui Dialog component usage"
```

---

## 구현 가이드

**현재 상태**: 미션 시스템 구현 완료 ✅ (90% 진행)

**완료된 기능**:
- ✅ 인증 시스템 (Email + PIN)
- ✅ 티켓 스캔 (OCR)
- ✅ 로봇 호출 (미션 생성)
- ✅ 실시간 추적 (SSE)
- ✅ 보관/반납 플로우
- ✅ localStorage 영구 저장

**다음 단계**: 관리자 대시보드 (선택 구현) 🆕

**상세 계획**: `docs/next-step.md` 참조 (대부분 완료됨)

**구현 순서**:
1. Types 정의 (`mission.types.ts`)
2. API 레이어 (`mission.api.ts`)
3. State Management (`missionStore.ts`, `adminStore.ts`)
4. Hooks (`useMissionSSE.ts`, `useAdminSSE.ts`)
5. Pages (`HomePage` 수정, `MissionCreatePage`, `MissionTrackPage`)
6. Routing (`routes/index.tsx` 업데이트)
7. 각 단계마다 `CODE_REFERENCE.md`에 문서화

---

## 테스트 (향후 계획)

**테스트 프레임워크**: Vitest (계획)
**E2E**: Playwright (계획)

---

## 트러블슈팅

### Tailwind 스타일 미적용
1. `src/index.css`에 `@import "tailwindcss";` 확인
2. `postcss.config.js`에 `@tailwindcss/postcss` 확인
3. 개발 서버 재시작 (`npm run dev`)

### CORS 에러
- 백엔드 서버에서 CORS 설정 필요
- `VITE_API_BASE_URL` 환경 변수 확인
- 브라우저 개발자 도구 Network 탭에서 요청 헤더 확인

### SSE 연결 실패
- Network 탭에서 EventStream 타입 요청 확인
- Authorization 헤더에 Bearer 토큰 포함 여부 확인
- 백엔드 SSE 엔드포인트 상태 확인 (로그 참조)

### 401 에러 무한 루프
- `axios.ts` interceptor에서 `_retry` 플래그 확인
- Refresh Token 만료 확인 (localStorage에서 확인)
- 로그아웃 후 재로그인 시도

### React Hook 의존성 배열 경고
- ESLint 경고 확인 후 필요한 의존성 추가
- 의도적으로 제외하는 경우 `// eslint-disable-next-line` 주석 추가
- useCallback, useMemo로 함수/객체 메모이제이션

### shadcn/ui 컴포넌트 import 에러
- `@/` alias가 설정되지 않은 경우: `tsconfig.json`에서 `paths` 설정 확인
```json
{
  "compilerOptions": {
    "baseUrl": ".",
    "paths": {
      "@/*": ["./src/*"]
    }
  }
}
```
- Vite 설정도 필요: `vite.config.ts`에서 `resolve.alias` 확인
```typescript
export default defineConfig({
  resolve: {
    alias: {
      "@": path.resolve(__dirname, "./src"),
    },
  },
})
```

### shadcn/ui 스타일 미적용
- `src/index.css`에 shadcn/ui CSS 변수 확인
- Tailwind CSS 설정 확인 (`@import "tailwindcss";`)
- `components.json` 설정 파일 확인 (shadcn/ui 초기화 시 생성)

---

## 참고 문서

- **API 명세**: `docs/api-spec.md` (백엔드 스펙, 필수 참조)
- **구현 가이드**: `docs/next-step.md` (로봇 호출 기능 추가, 필수 참조)
- **코드 레퍼런스**: `docs/CODE_REFERENCE.md` (동작 원리 및 학습 자료)
- **개발 가이드**: `docs/DEVELOPMENT_GUIDE.md`
- **기술 스택**: `docs/TECH_STACK.md`

---

## 코드 작성 후 필수 작업

1. **기능 구현**
   - 코드 작성
   - 로컬 테스트
   - 에러 확인 및 수정

2. **문서화** (필수!)
   - `docs/CODE_REFERENCE.md`에 다음 내용 추가:
     - 코드 동작 원리 (세세한 분석)
     - 발생한 에러와 해결 방법
     - 성능 최적화 과정
     - 학습 포인트

3. **커밋**
   - 한국어로 명확한 커밋 메시지 작성
   - 예: "feat: 미션 생성 API 및 SSE 연결 구현"

---

**최종 업데이트**: 2026년 1월 29일
**구현 진행률**: 90% (미션 시스템 + 보관/반납 플로우 완료)
