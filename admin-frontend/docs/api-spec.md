# API 명세서

## 목차
- [API 개요](#api-개요)
- [인증 관련 API](#인증-관련-api)
  - [인증번호 발송](#인증번호-발송)
  - [로그인](#로그인)
  - [토큰 재발급](#토큰-재발급)
- [미션 관련 API](#미션-관련-api)
  - [미션 생성](#미션-생성)
  - [미션 사용자 수신용 SSE 연결](#미션-사용자-수신용-sse-연결)
  - [사용자 잠금 해제](#사용자-잠금-해제)
- [관리자 관련 API](#관리자-관련-api)
  - [관리자 수신용 SSE 연결](#관리자-수신용-sse-연결)
  - [관리자 권한 로봇 잠금 해제](#관리자-권한-로봇-잠금-해제)
  - [관리자 권한 로봇 잠금](#관리자-권한-로봇-잠금)
  - [관리자 권한 로봇 이동](#관리자-권한-로봇-이동)

---

## API 개요

### 엔드포인트 요약

| 카테고리 | 기능 | 사용자 | Method | Auth | End Point | 설명 |
|---------|------|--------|--------|------|-----------|------|
| 인증 | 인증번호 발송 | Guest | POST | X | `/api/auth/code` | 사용자의 이메일로 Mattermost 인증번호를 발송합니다. 이 정보는 DB에 저장되지 않고 Redis에만 3분간 저장됩니다. |
| 인증 | 로그인 | Guest | POST | X | `/api/auth/verify` | 인증번호를 검증합니다. 맞으면 DB에 사용자를 등록(또는 갱신)하고 토큰을 발급합니다. |
| 인증 | 토큰 재발급 | User | POST | O | `/api/auth/reissue` | Access Token이 만료되었을 때, Refresh Token으로 새 토큰을 받습니다. |
| 미션 | 미션 생성 | User | POST | O | `/api/missions` | 미션 생성 엔드포인트 |
| 미션 | 미션 사용자 수신용 SSE 연결 | User | GET | O | `/api/missions/{missionId}/subscribe` | 미션 상태 실시간 구독 (SSE) |
| 미션 | 사용자 잠금 해제 | User | PATCH | O | `/api/missions/{missionId}/verify` | 사용자가 짐을 넣거나 빼기 위해 자신의 4자리 비밀번호를 입력하는 단계 |
| 관리자 | 관리자 수신용 SSE 연결 | 관리자 | GET | O | `/api/admin/sse/subscribe` | 관리자가 로그인 성공후 대시보드 들어갔을 경우 SSE 연결 |
| 관리자 | 관리자 권한 로봇 잠금해제 | 관리자 | POST | O | `/api/admin/missions/{missionId}/unlock` | 관리자 권한 로봇 잠금해제 엔드포인트 |
| 관리자 | 관리자 권한 로봇 잠금 | 관리자 | POST | O | `/api/admin/missions/{missionId}/lock` | 관리자 권한 로봇 잠금 엔드포인트 |
| 관리자 | 관리자 권한 로봇 이동 | 관리자 | PATCH | O | `/api/admin/missions/{missionId}/move` | 관리자가 고객위치로 로봇을 이동 명령하는 엔드포인트 |

### 인증 관련 참고사항

- `/api/auth/reissue`는 Access Token 인증은 필요 없으며, HttpOnly Cookie에 저장된 Refresh Token으로 인증합니다.
- 관리자 API는 `ROLE_ADMIN`(또는 `is_admin`) 권한이 추가로 필요합니다.
- 사용자 미션 SSE/verify API는 `mission.user_id == current_user.id` (또는 참가자 권한) 체크가 필요합니다.

---

## 인증 관련 API

### 인증번호 발송

사용자의 이메일로 Mattermost 인증번호를 발송합니다.

**Endpoint:** `POST /api/auth/request`
**Auth:** X

#### Request

| Key | 설명 | Type | Nullable | 예시 |
|-----|------|------|----------|------|
| email | Mattermost 이메일 | String | X | "test@ssafy.com" |
| password | 4자리 비밀번호 | Integer | X | 1234 |

**Request Body:**
```json
{
  "email": "ssafy@ssafy.com",
  "password": 1234
}
```

#### Response

**Success (200):**
```json
{
  "status": "SUCCESS",
  "message": "인증번호가 전송되었습니다.",
  "code": 76,
  "expiresIn": 300
}
```

#### Status Codes

| Status | Description |
|--------|-------------|
| 200 | 성공 |

---

### 로그인

인증번호를 검증하고 DB에 사용자를 등록(또는 갱신)하고 토큰을 발급합니다.

**Endpoint:** `POST /api/auth/verify`
**Auth:** X

#### Request

| Key | 설명 | Type | Nullable | 예시 |
|-----|------|------|----------|------|
| email | Mattermost 이메일 | String | X | "test@ssafy.com" |
| code | 인증번호(2자리) | Integer | X | 35 |

**Request Body:**
```json
{
  "email": "ssafy@ssafy.com",
  "code": 35
}
```

#### Response

**Success (200):**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "expiresIn": 3600
}
```

**Error (4xx/5xx):**
```json
{
  "timestamp": "2024-05-21T14:20:30",
  "status": 500,
  "error": "Internal Server Error",
  "message": "인증번호가 일치하지 않습니다.",
  "path": "/api/auth/verify"
}
```

#### Status Codes

| Status | Description |
|--------|-------------|
| 200 | 성공 |
| 4xx | Client Error |
| 5xx | Server Error |

---

### 토큰 재발급

Access Token이 만료되었을 때, Refresh Token으로 새 토큰을 받습니다.

**Endpoint:** `POST /api/auth/reissue`
**Auth:** O (Refresh Token)

#### Request

Request Body 없음. HTTP 요청 헤더에 Refresh Token을 전달합니다.

**Headers:**
```
Authorization-Refresh: Bearer {RefreshToken}
```

#### Response

**Success (200):**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "expiresIn": 3600
}
```

**Error (4xx/5xx):**
```json
{
  "timestamp": "2024-05-21T14:20:30",
  "status": 401,
  "error": "Unauthorized",
  "message": "Refresh Token이 만료되었습니다.",
  "path": "/api/auth/reissue"
}
```

#### Status Codes

| Status | Description |
|--------|-------------|
| 200 | 성공 |
| 401 | Refresh Token 만료 또는 유효하지 않음 |
| 4xx | Client Error |
| 5xx | Server Error |

#### 기타 설명

- HTTP 요청 헤더에 `Authorization-Refresh: Bearer {RefreshToken}` 형식으로 Refresh Token을 전달합니다.
- Response에는 새로운 Access Token만 반환되며, Refresh Token은 반환되지 않습니다.
- Refresh Token이 만료된 경우 401 에러가 발생하며, 사용자는 다시 로그인해야 합니다.

---

## 미션 관련 API

### 미션 생성

새로운 미션을 생성합니다.

**Endpoint:** `POST /api/missions`
**Auth:** O

#### Request

| Key | 설명 | Type | Nullable | 예시 |
|-----|------|------|----------|------|
| userId | 사용자 ID | Long | X | 1 |
| startLocationId | 시작 위치 ID | Long | X | 1 |
| endLocationId | 도착 위치 ID | Long | X | 3 |

> ⚠️ `userId`는 추후 삭제 예정입니다. JWT 토큰에서 userId를 추출하는 방식으로 변경될 예정입니다.

**Request Body:**
```json
{
  "userId": 1,
  "startLocationId": 1,
  "endLocationId": 3
}
```

#### Response

**Success (200):**
```json
{
  "missionId": 1
}
```

#### Status Codes

| Status | Description |
|--------|-------------|
| 200 | 성공 |
| 400 | 잘못된 요청 |

---

### 미션 사용자 수신용 SSE 연결

미션 상태를 실시간으로 구독합니다.

**Endpoint:** `GET /api/missions/{missionId}/subscribe`
**Auth:** O

#### Request

| Key | 설명 | Type | 옵션 | Nullable | 예시 |
|-----|------|------|------|----------|------|
| missionId | 구독할 Mission ID | Long | Path Variable | X | 1 |

#### Response

**Headers:**
- `Content-Type: text/event-stream`
- `Connection: keep-alive`
- `Timeout: 30분`

**Event Types:**

| 상황 | Event | Data | Description |
|------|-------|------|-------------|
| 연결 확립 | CONNECT | `Connected! [MissionId: {missionId}]` | SSE 연결 성공 시 최초 1회 전송 |
| 초기 상태 전송 | STATUS | `REQUESTED` | 연결 직후 현재 미션 상태 전송 |
| 상태 변경 | STATUS | `ASSIGNED` | 로봇 배정 시 |
| 상태 변경 | STATUS | `MOVING` | 로봇 이동 시작 시 |
| 상태 변경 | STATUS | `ARRIVED` | 로봇 도착 시 |
| 상태 변경 | STATUS | `UNLOCKED` | 잠금 해제 시 |
| 상태 변경 | STATUS | `LOCKED` | 잠금 시 |
| 상태 변경 | STATUS | `RETURNING` | 로봇 복귀 시작 시 |
| 상태 변경 | STATUS | `RETURNED` | 로봇 복귀 완료 시 |
| 상태 변경 | STATUS | `FINISHED` | 미션 완료 시 |

**Example (SSE Raw Format):**
```
event: CONNECT
data: Connected! [MissionID: 1]

event: STATUS
data: REQUESTED

event: STATUS
data: ASSIGNED

event: STATUS
data: ARRIVED
```

#### Status Codes

| Status | Description |
|--------|-------------|
| 200 | SSE 연결 성공 |
| 401 | 인증 실패 |
| 403 | 권한 없음 |
| 404 | 해당 미션 없음 |

---

### 사용자 잠금 해제

사용자가 짐을 넣거나 빼기 위해 4자리 비밀번호를 입력하여 잠금을 해제합니다.

**Endpoint:** `PATCH /api/missions/{missionId}/verify`
**Auth:** O

#### Request

| Key | 설명 | Type | 옵션 | Nullable | 예시 |
|-----|------|------|------|----------|------|
| missionId | 인증할 미션 ID | Long | Path Variable | X | 1 |
| password | 4자리 비밀번호 | Integer | Body | X | 1234 |

**Request Body:**
```json
{
  "password": 1234
}
```

#### Response

**Success:** `204 No Content`

#### Status Codes

| Status | Description |
|--------|-------------|
| 204 | 성공 |
| 400 | 잘못된 인증 코드 |
| 401 | 인증에 실패하였습니다 |
| 404 | 미션을 찾을 수 없습니다 |
| 409 | 인증이 불가능한 상태입니다 |

---

## 관리자 관련 API

### 관리자 수신용 SSE 연결

관리자가 대시보드에서 실시간으로 미션 상태를 구독합니다.

**Endpoint:** `GET /api/admin/sse/subscribe`
**Auth:** O (ROLE_ADMIN)

#### Request

| Key | 설명 | Type | 옵션 | Nullable | 예시 |
|-----|------|------|------|----------|------|
| adminId | 관리자 ID | Long | Query Parameter | X | 1 |

> ⚠️ `adminId`는 삭제될 예정입니다. JWT에서 user ID 값을 추출하는 방식으로 변경될 예정입니다.

#### Response

**Headers:**
- `Content-Type: text/event-stream`
- `Connection: keep-alive`
- `Timeout: 60분`

**Event Types:**

| Event | Data | 발생시점 |
|-------|------|----------|
| CONNECT | `Admin Connected!` | 관리자 SSE 연결 수립 시 (최초 1회) |
| ROBOT_ASSIGNED | `{"missionId": 1, "robotCode": "CP-001", "status": "ASSIGNED", "pickupLocation": "GATE_A", "timestamp": "2026-01-25T10:01:00Z"}` | 미션에 로봇이 배정되었을 때 (ASSIGNED) |
| ROBOT_ARRIVED | `{"missionId": 1, "robotCode": "CP-001", "status": "ARRIVED", "location": "GATE_A", "timestamp": "2026-01-25T10:15:00Z"}` | 로봇이 사용자 위치에 도착했을 때 (ARRIVED) |
| ROBOT_RETURNED | `{"missionId": 1, "robotCode": "CP-001", "status": "RETURNED", "station": "STATION_1", "timestamp": "2026-01-25T10:35:00Z"}` | 로봇이 관리소로 복귀했을 때 (RETURNED) |

**Example (SSE Raw Format):**
```
event: CONNECT
data: Admin Connected!

event: ROBOT_ASSIGNED
data: {"missionId":1,"robotCode":"CP-001","status":"ASSIGNED","pickupLocation":"GATE_A","timestamp":"2026-01-25T10:01:00Z"}

event: ROBOT_ARRIVED
data: {"missionId":1,"robotCode":"CP-001","status":"ARRIVED","location":"GATE_A","timestamp":"2026-01-25T10:15:00Z"}

event: ROBOT_RETURNED
data: {"missionId":1,"robotCode":"CP-001","status":"RETURNED","station":"STATION_1","timestamp":"2026-01-25T10:35:00Z"}
```

#### Status Codes

| Status | Description |
|--------|-------------|
| 200 | 성공 |
| 401 | 인증 실패 |
| 403 | 권한 없음 |
| 404 | 사용자를 찾을 수 없음 |

---

### 관리자 권한 로봇 잠금 해제

관리자가 로봇의 잠금을 해제합니다.

**Endpoint:** `POST /api/admin/missions/{missionId}/unlock`
**Auth:** O (ROLE_ADMIN)

#### Request

| Key | 설명 | Type | 옵션 | Nullable | 예시 |
|-----|------|------|------|----------|------|
| missionId | 잠금해제 할 로봇이 배정된 Mission ID | Long | Path Variable | X | 1 |

#### Response

**Success:** `204 No Content`

#### Status Codes

| Status | Description |
|--------|-------------|
| 204 | 성공 |
| 403 | 권한 없음 |
| 404 | 해당 미션을 찾을 수 없습니다 |
| 409 | 잠금 해제가 불가능한 상태입니다 |

---

### 관리자 권한 로봇 잠금

관리자가 로봇을 잠급니다.

**Endpoint:** `POST /api/admin/missions/{missionId}/lock`
**Auth:** O (ROLE_ADMIN)

#### Request

| Key | 설명 | Type | 옵션 | Nullable | 예시 |
|-----|------|------|------|----------|------|
| missionId | 로봇이 할당된 Mission ID | Long | Path Variable | X | 1 |

#### Response

**Success:** `204 No Content`

#### Status Codes

| Status | Description |
|--------|-------------|
| 204 | 성공 |
| 400 | 유효하지 않은 형식입니다 |
| 403 | 권한이 없습니다 |
| 404 | 로봇을 찾을 수 없습니다 |
| 409 | 이미 LOCKED 상태이거나 잠금이 불가능한 상태입니다 |

---

### 관리자 권한 로봇 이동

관리자가 고객 위치로 로봇을 이동시킵니다.

**Endpoint:** `PATCH /api/admin/missions/{missionId}/move`
**Auth:** O (ROLE_ADMIN)

#### Request

| Key | 설명 | Type | 옵션 | Nullable | 예시 |
|-----|------|------|------|----------|------|
| missionId | 로봇이 할당된 Mission ID | Long | Path Variable | X | 1 |

#### Response

**Success:** `204 No Content`

#### Status Codes

| Status | Description |
|--------|-------------|
| 204 | 성공 |
| 400 | 잘못된 요청입니다 |
| 403 | 해당 로봇을 움직일 권한이 없습니다 |
| 404 | 로봇을 찾을 수 없습니다 |
| 409 | 이동 불가능한 상태입니다 |
