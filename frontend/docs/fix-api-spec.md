# api-spec에서 변경된 정보 

## SSE
| 기능 | 사용자 | Method | URL | param | 설명 |
| --- | --- | --- | --- | --- | --- |
| SSE 구독 요청| | GET | `/api/sse/subscribe` | | 실시간 알림 구독(Server-Sent Events)

### 기타 설명
- **Headers**
  - `Authroization: Bearer {JWT_ACCESS_TOKEN}` **(필수)**
  - `Accept: text/event-stream`
  - `Cache-Control: no-cache`
  - `Connection: keep-alive`
    - **주의사항**: 브라우저 기본 `EventSource`는 헤더 설정을 지원하지 않습니다.
  - `event-source-polyfill` **라이브러리 사용이 필수**입니다.

- **이벤트 리스트 (Events)**
  - 서버는 상황에 따라 서로 다른 `Event Name`을 보냅니다. 프론트엔드에서는 `addEventListener("이벤트명", ...)`으로 분기 처리해야 합니다.

- **공통 데이터 구조(Data Payload)**
  - 모든 이벤트의 `data`는 아래와 같은 JSON 형식을 갖습니다.
  - ``` json
      {
        "msg": "사용자에게 노출할 메시지",
        "timestamp": "2026-01-30T15:00:00",
        "robotCode": "ROBOT_01" (선택사항, 로봇 관련 이벤트 시 포함)
      }
    ```

- **이벤트 별 메시지 정의서(프론트엔드 공유용)**

| 이벤트명 | 발생 상황 | 프론트 엔드 권장 액션 | 페이로드 예시 |
| --- | --- | --- | --- |
| `Connect` | 최초 구독 성공 시 | 연결 성공 로그 출력 | `"Connected!"` |
| `RobotAssignedEvent` | 로봇 매칭 완료 | 알림 팝업 ("로봇 배정됨") | `{"msg": "...", "robotCode": "R1"}` |
| `MissionStartedEvent` | 로봇 출발 시 | 배송 현황 지도로 이동 | `{"msg": "...", "robotCode": "R1"}` |
| `RobotArrivalEvent` | 로봇 도착 시 | **비밀번호 입력창 활성화** | `{"msg": "...", "robotCode": "R1"}` |
| `UserAuthSuccessEvent` | 인증 성공 시 | "인증 성공" 메시지 & 로딩 스피너 | `{"msg": "...", "timestamp": "..."}` |
| `MissionUnlockedEvent` | 로봇 문 열림 | "수하물을 꺼내 주세요" UI 출력 | `{"msg": "...", "timestamp": "..."}` |
| `MissionAbortedEvent` | 미션 중단 | 경고 팝업 및 홈으로 이동 | `{"msg": "...", "timestamp": "..."}` |
| `MissionLockedEvent` | 최종 종료/잠금 | **리뷰 작성 창 또는 종료 완료** | `{"msg": "...", "timestamp": "..."}` |

- **SSE 구독 및 처리 가이드**
  1. **구독 시작**: `new EventSource('/api/sse/subscribe?userId=1')`로 연결을 시작해 주세요.
  2. **이벤트 수신**: 브라우저 기본 `onmessage`가 아니라 `addEventListener("이벤트명", callback)` 형식을 사용해야 합니다.(저희 서버가 이벤트 이름을 지정해서 보내기 때문입니다.)
  3. **데이터 파싱**: 전달되는 데이터(`event.data`)는 JSON 문자열입니다. `JSON.parse()`로 객체화해서 사용하세요.
  4. **공통 데이터 구조**: 
      ``` json 
        {
          "msg": "사용자에게 보여줄 메시지",
          "timestamp": "2026-01-30T...",
          "robotCode": "R-123" (있는 경우만 포함)
        }
      ```

- **구현 예시**
``` javascript
  import { EventSourcePolyfill } from 'event-source-polyfill';

  const subscribeSSE = (token) => {
    const eventSource = new EventSourcePolyfill('/api/sse/subscribe', {
      headers: {
        'Authorization': `Bearer ${token}`
      },
      heartbeatTimeout: 60000 // 1분 동안 반응 없으면 재연결 시도
    });

    // 1. 로봇 도착 시
    eventSource.addEventListener('RobotArrivalEvent', (e) => {
      const data = JSON.parse(e.data);
      alert(data.msg); // "로봇이 도착했습니다. 비밀번호를 입력하세요."
      showPasswordInput(true); 
    });

    // 2. 로봇 실제 열림 응답 시
    eventSource.addEventListener('MissionUnlockedEvent', (e) => {
      const data = JSON.parse(e.data);
      updateUI('unlocked'); // 문 열림 애니메이션 등
    });

    // 에러 처리
    eventSource.onerror = (err) => {
      console.error("SSE 연결 오류:", err);
      eventSource.close();
    };
  };
```

## 미션
| 기능 | 사용자 | Method | URL | param | 설명 |
| --- | --- | --- | --- | --- | --- |
| 미션생성 | User | POST | `/api/missions` | | |
| 미션 SSE 수립 | User | GET | `/api/missions/{missionId}/subscribe` | | 사용자 미션 생성 시 수립. 미션 상태 변환가 발생할 경우 해당 미션을 구독한 사용자 실시간 알림용 |
| 관리자 SSE 수립 | 관리자 | GET | `/api/admin/sse/subscribe` | | 관리자 페이지 접속 시 수립. 미션 상태 변화가 발생할 경우 알림 받는 용 |

### 미션 생성
**Request**
| key | 설명 | value 타입 | 옵션 | Nullable | 예시 |
| --- | --- | --- | --- | --- | --- |
| userId | 사용자 아이디 | long | | false | 1 |
| startLocationId | 시작 위치 아이디 | long | | false | 1 |
| endLocationId | 도착 위치 아이디 | long | | false | 1 |

**Request Body**
``` json
{
	"userId": 1,
	"startLocation": 1,
	"endLocation": 2
}
```

**Response**
| key | 설명 | value 타입 | 옵션 | Nullable | 예시 |
| --- | --- | --- | --- | --- | --- |
| missionId | 생성된 mission id | long | | | |

**Example**
``` json
{
	"missionId": 1
}
```

### 미션 SSE 수립
**Headers**
| key | value | 
| --- | --- |
| Accept | text/event-stream |
| Cache-control | no-cache | 
| Connection | keep-alive | 

**Response(server push data)**
| key | 설명 | value 타입 | 옵션 | Nullable | 예시 |
| --- | --- | --- | --- | --- | --- |
| missionId | mission id | long | | | |
| status | Mission 상태 | String | | | |
| timestamp | 미션 상태 변경 시간 | String | | | |

**Example**
``` json
{
  "missionId": 1,
  "status": "ASSIGNED",
  "timestamp": "2026-01-24T19:30:00"
}
```

**mission status 종류에 따른 ui 노출 문구**
| | |
| --- | --- |
| `REQUESTED` | **배정 중** |
| `ASSIGNED` | **접수 중** |
| `MOVING` | **이동 중** |
| `ARRIVED` | **인증 중** |
| `UNLOCKED` | **짐 무게 측정** |
| `LOCKED` | **수령 완료** |
| `RETURNING` | 복귀 중 |
| `RETURNED` | 복귀 완료 |
| `FINISHED` | 종료 |

###관리자 SSE 수립###
**Headers**
| key | value | 
| --- | --- |
| Accept | text/event-stream |
| Cache-control | no-cache | 
| Connection | keep-alive | 

**Response**
| key | 설명 | value 타입 | 옵션 | Nullable | 예시 |
| --- | --- | --- | --- | --- | --- |
| missionId | mission id | long | | | |
| status | 로봇 상태 | String | | | |
| timestamp | 로봇봇 상태 변경 시간 | String | | | |

**Example**
``` json
{
  "missionId": 1,
  "status": "ASSIGNED",
  "timestamp": "2026-01-24T19:30:00"
}
```

| | |
| --- | --- |
| `ASSIGNED` | 미션에 로봇 배정 시 |
| `ARRIVED` | 사용자에게 로봇이 도착 시 |
| `RETURNED` | 관리소에 로봇이 도착 시 |