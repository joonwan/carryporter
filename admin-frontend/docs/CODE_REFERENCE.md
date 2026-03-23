# CARRY PORTER 관리자 대시보드 코드 레퍼런스

> 관리자 대시보드 컴포넌트, 상태 관리, 스타일 가이드

---

## 목차

1. [프로젝트 개요](#프로젝트-개요)
2. [폴더 구조](#폴더-구조)
3. [상태 관리 (Zustand)](#상태-관리-zustand)
4. [페이지 컴포넌트](#페이지-컴포넌트)
5. [레이아웃 컴포넌트](#레이아웃-컴포넌트)
6. [디자인 시스템](#디자인-시스템)

---

## 프로젝트 개요

**공항 로봇 관리 시스템 관리자 대시보드**

관리자가 로봇 상태, 사물함 할당, 사용자 관리를 수행하는 웹 애플리케이션입니다.

**기술 스택:**
- React 19 + TypeScript 5.9
- Tailwind CSS v4
- Zustand (상태 관리)
- Lucide Icons
- React Router v7

---

## 폴더 구조

```
src/
├── components/
│   ├── dashboard/     # 대시보드 컴포넌트
│   └── layout/        # 레이아웃 컴포넌트
├── pages/             # 페이지 컴포넌트
├── store/             # Zustand 스토어
├── lib/               # 유틸리티
├── App.tsx            # 라우팅 설정
└── index.css          # 글로벌 스타일
```

---

## 상태 관리 (Zustand)

### themeStore

**위치**: `src/store/themeStore.ts`

```typescript
interface ThemeState {
    theme: 'light' | 'dark'
    toggleTheme: () => void
    setTheme: (theme: Theme) => void
}
```

**사용법:**
```tsx
const { theme, toggleTheme } = useThemeStore()
```

---

### robotStore

**위치**: `src/store/robotStore.ts`

```typescript
interface Robot {
    id: string
    name: string
    status: 'available' | 'busy' | 'returning' | 'maintenance'
    lockStatus: 'locked' | 'unlocked'
    battery: number
    position: { x: number; y: number }
}

interface RobotState {
    robots: Robot[]
    selectedRobotId: string | null
    selectRobot: (id: string | null) => void
    unlockRobot: (id: string) => void    // 로봇 열기
    lockRobot: (id: string) => void      // 로봇 닫기
    dispatchRobot: (id: string) => void  // 로봇 출발
}
```

**로봇 열기/닫기 플로우:**
```
관리자 → unlockRobot() → 짐 넣기 → lockRobot() → dispatchRobot()
```

---

### lockerStore

**위치**: `src/store/lockerStore.ts`

```typescript
interface Locker {
    id: string
    name: string
    status: 'available' | 'occupied' | 'reserved'
    userId?: string
    userName?: string
    robotId?: string
    luggageWeight?: number
}

interface LockerState {
    lockers: Locker[]
    selectedLockerId: string | null
    selectLocker: (id: string | null) => void
    assignLocker: (...) => void
    releaseLocker: (lockerId: string) => void
}
```

---

## 페이지 컴포넌트

### DashboardPage

**위치**: `src/pages/DashboardPage.tsx`

**목적**: 관리자 대시보드 개요
- 로봇 상태 요약 (Total, Available, In Mission, Unlocked)
- 사물함 상태 요약 (Total, Available, Occupied)
- 활성 로봇 목록

---

### RobotsPage

**위치**: `src/pages/RobotsPage.tsx`

**목적**: 로봇 관리
- 모든 로봇 상태 그리드
- 로봇 상세 정보 패널
- **로봇 열기/닫기 버튼**
- 로봇 출발 버튼

**핵심 기능:**
```tsx
// 로봇 열기
<button onClick={() => unlockRobot(robot.id)}>Open Robot</button>

// 로봇 닫기
<button onClick={() => lockRobot(robot.id)}>Close Robot</button>

// 로봇 출발
<button onClick={() => dispatchRobot(robot.id)}>Dispatch Robot</button>
```

---

### LockersPage

**위치**: `src/pages/LockersPage.tsx`

**목적**: 사물함 관리
- 24개 사물함 그리드 (12 x 2)
- 사물함 상세 정보 패널
- 할당/해제 버튼

---

## 레이아웃 컴포넌트

### MainLayout

**위치**: `src/components/layout/MainLayout.tsx`

**목적**: 사이드바 + 메인 컨텐츠
- 테마 클래스 적용 (`light-mode`)
- React Router Outlet

---

## 디자인 시스템

### Glassmorphism 클래스

| 클래스 | 설명 |
|--------|------|
| `.glass-card` | 투명 카드 |
| `.glass-modal` | 모달 컨테이너 |
| `.gradient-title` | 그라데이션 제목 |
| `.gradient-button` | 그라데이션 버튼 |

### Neon 효과 클래스

| 클래스 | 색상 |
|--------|------|
| `.neon-icon-cyan` | 시안 |
| `.neon-icon-green` | 초록 |
| `.neon-icon-orange` | 주황 |
| `.neon-icon-purple` | 보라 |
| `.neon-border-cyan` | 시안 테두리 |
| `.neon-border-green` | 초록 테두리 |
| `.neon-border-orange` | 주황 테두리 |

### Light Mode

`light-mode` 클래스가 `<html>`에 적용되면 밝은 테마 활성화:
- 배경: 회색 계열
- 카드: 흰색 반투명
- 텍스트: 진한 회색

---

## 라우팅

| 경로 | 페이지 |
|------|--------|
| `/` | DashboardPage |
| `/robots` | RobotsPage |
| `/lockers` | LockersPage |

---

**최종 업데이트**: 2026년 1월 29일
