# ✨ carryporter

<div align="center">

**한 걸음이 무거운 순간, 짐 걱정은 여기까지**

**교통 약자를 위한 호출형 짐 운반 서비스**

<img width="800" height="450" alt="image" src="https://github.com/user-attachments/assets/505a5330-5f40-41f7-aa64-03e773280b31" />

Carry Porter는 공항 내 지정된 경로(Line)를 따라 수하물을 인수·보관·반환하는 라인트레이싱 기반의 스마트 포터 시스템입니다.

**개발 기간** : 2026.01.06 ~ 2026.02.09 **(6주)**  
**플랫폼** : AIoT & Web  
**개발 인원** : 6명  
**기관** : 삼성 청년 SW·AI 아카데미 14기

</div>

---

# 🔎 목차

- [🧑‍💻 팀 구성](#-팀-구성)
- [🛠️ 기술 스택](#️-기술-스택)
- [🎯 주요 기능](#-주요-기능)
- [📦 프로젝트 산출물](#-프로젝트-산출물)

---

# 🧑‍💻 팀 구성

| ![](https://github.com/user-attachments/assets/88fc8c78-a2fb-4447-b670-6b81ba2f188a) | ![](https://github.com/user-attachments/assets/8eef5d47-483e-488a-9a71-e3d517afb06f) | ![](https://github.com/user-attachments/assets/5d1b393e-fd2e-4696-af8c-f533976e1a6b) |
|:---:|:---:|:---:|
| **서기현** | **강희정(팀장)** | **정승현** |
| AI & EMB | FE & BE & EMB | EMB Leader |
| LiDAR & YOLO 기반 Safety Logic 구현 | Three.js 기반 3D 관리자 대시보드 개발 | ROS2 기반 Robot Control Logic 구현 |
| EasyOCR 기반 데이터 자동 추출 시스템 구현 | 백엔드 SSE 및 관리자 서비스 로직 개발 | Multi-Sensor & Actuator Interface 통합 |
| ROS2 기반 Multi-Sensor 주행 로직 구현 | Tinkercad를 활용한 로봇 디자인 | 로봇 구조 설계 및 제작 |

| ![](https://github.com/user-attachments/assets/d7bde38e-7d53-402b-9760-0ee547c2a2fc) | ![](https://github.com/user-attachments/assets/7ce61a8b-d5a2-46db-a4da-4a0b82d76738) | ![](https://github.com/user-attachments/assets/2b2ede2c-5209-4958-9bf6-440e017d96eb) |
|:---:|:---:|:---:|
| **박승찬** | **김정훈** | **전준완** |
| FE Leader | INFRA & BE | BE Leader & EMB |
| EventSource SSE 실시간 미션 추적 시스템 구현 | 인프라 및 CI/CD 파이프라인 구축·운영 | ERD 및 요구사항 명세 설계 |
| Axios 인터셉터 토큰 갱신 및 대기열 관리 | MQTT 통신 구현 및 연동 | Redis 활용한 로봇 가용 큐 관리 |
| React TS 상태 머신 기반 미션 플로우 설계 | SSE 구독 연결 안정화 작업 | ROS2 기반 주행 로직 안정화 작업 |

---

# 🛠️ 기술 스택
### 📟 Embedded & IoT

<img src="https://img.shields.io/badge/Python-3776AB?style=for-the-badge&logo=python&logoColor=FFD43B"> <img src="https://img.shields.io/badge/Visual%20Studio%20Code-007ACC?style=for-the-badge&logo=visualstudiocode&logoColor=white"> <img src="https://img.shields.io/badge/Jetson%20Orin%20Nano-76B900?style=for-the-badge&logo=nvidia&logoColor=white"> <img src="https://img.shields.io/badge/MQTT-660066?style=for-the-badge&logo=mqtt&logoColor=white"> <img src="https://img.shields.io/badge/ROS2-22314E?style=for-the-badge&logo=ros&logoColor=white"> <img src="https://img.shields.io/badge/YOLOv8-00FFFF?style=for-the-badge&logo=yolo&logoColor=black">

| 구분 | 사용 기술 |
|------|----------|
| Language | Python 3.10.12 |
| Development Tools | Vim, Visual Studio Code |
| SBC | Jetson Orin Nano board Jetpack 6.2 |
| OS | Ubuntu 22.04.5 LTS |
| Hardware & Robotics | LiDAR (YDLiDAR X4 pro), Camera (Logitech Brio 100), Motor Driver (PCA 9685, PWM Control), DC Motor (JGA25-371), Servo Motor (MG996R), Encoder, IMU (MPU6050), Solenoid, LED, Photo Sensor, Color Sensor (TCS34725) |
| Python Libs | OpenCV 4.10.0, Pytorch 2.3.0, torchvision 0.18.0, Ultralytics (YOLOv8n) 8.2.84, Numpy 1.26.4, ROS2 humble |
| Communication | MQTT |
| Features | Servo Control, Line Tracking, Obstacle Detection, YOLO Person Detection, LED Warning Expression, Solenoid Lock |

---

---

### 🍃 Backend

<img src="https://img.shields.io/badge/Java%2017-007396?style=for-the-badge&logo=openjdk&logoColor=white"> <img src="https://img.shields.io/badge/Spring%20Boot%203.5.10-6DB33F?style=for-the-badge&logo=springboot&logoColor=white"> <img src="https://img.shields.io/badge/Spring%20Data%20JPA-6DB33F?style=for-the-badge&logo=spring&logoColor=white"> <img src="https://img.shields.io/badge/Spring%20Data%20Redis-DC382D?style=for-the-badge&logo=redis&logoColor=white"> <img src="https://img.shields.io/badge/Spring%20WebFlux-6DB33F?style=for-the-badge&logo=spring&logoColor=white"> <img src="https://img.shields.io/badge/MQTT-660066?style=for-the-badge&logo=mqtt&logoColor=white"> <img src="https://img.shields.io/badge/JWT-000000?style=for-the-badge&logo=jsonwebtokens&logoColor=white"> <img src="https://img.shields.io/badge/MySQL-4479A1?style=for-the-badge&logo=mysql&logoColor=white"> <img src="https://img.shields.io/badge/Gradle-02303A?style=for-the-badge&logo=gradle&logoColor=white">

| 구분 | 사용 기술 |
|------|----------|
| Language | Java 17 |
| Framework | Spring Boot 3.5.10 |
| Library | Spring Data JPA, Spring Data Redis, Spring Integration MQTT, Spring WebFlux, Spring AOP, Spring Retry, Lombok, jBCrypt, JJWT 0.11.5, spring-dotenv |
| MQTT | Spring Integration MQTT + Eclipse Paho 1.2.5 |
| Build Tool | Gradle |
| Features | JWT 인증, 이메일 인증번호 (Mattermost 웹훅), 미션 이벤트 기반 처리, SSE 실시간 알림, MQTT 로봇 제어, Redis 로봇 상태 관리 |

---

### 🔍 OCR Service

<img src="https://img.shields.io/badge/Python-3776AB?style=for-the-badge&logo=python&logoColor=FFD43B"> <img src="https://img.shields.io/badge/FastAPI-009688?style=for-the-badge&logo=fastapi&logoColor=white"> <img src="https://img.shields.io/badge/Uvicorn-499848?style=for-the-badge&logo=gunicorn&logoColor=white"> <img src="https://img.shields.io/badge/EasyOCR-FF6B6B?style=for-the-badge&logo=python&logoColor=white"> <img src="https://img.shields.io/badge/OpenCV-5C3EE8?style=for-the-badge&logo=opencv&logoColor=white"> <img src="https://img.shields.io/badge/NumPy-013243?style=for-the-badge&logo=numpy&logoColor=white">

| 구분 | 사용 기술 |
|------|----------|
| Language | Python |
| Framework | FastAPI, Uvicorn |
| OCR Engine | EasyOCR |
| Library | OpenCV, NumPy |
| Features | 탑승권 이미지 텍스트 추출 |

---

### 🖥️ Frontend (사용자)

<img src="https://img.shields.io/badge/TypeScript-3178C6?style=for-the-badge&logo=typescript&logoColor=white"> <img src="https://img.shields.io/badge/React%2019-61DAFB?style=for-the-badge&logo=react&logoColor=black"> <img src="https://img.shields.io/badge/Vite%207-646CFF?style=for-the-badge&logo=vite&logoColor=white"> <img src="https://img.shields.io/badge/Tailwind%20CSS%20v4-06B6D4?style=for-the-badge&logo=tailwindcss&logoColor=white"> <img src="https://img.shields.io/badge/Radix%20UI-161618?style=for-the-badge&logo=radixui&logoColor=white"> <img src="https://img.shields.io/badge/Zustand-443E38?style=for-the-badge&logo=react&logoColor=white"> <img src="https://img.shields.io/badge/TanStack%20Query-FF4154?style=for-the-badge&logo=reactquery&logoColor=white"> <img src="https://img.shields.io/badge/React%20Router-CA4245?style=for-the-badge&logo=reactrouter&logoColor=white"> <img src="https://img.shields.io/badge/React%20Hook%20Form-EC5990?style=for-the-badge&logo=reacthookform&logoColor=white"> <img src="https://img.shields.io/badge/Zod-3E67B1?style=for-the-badge&logo=zod&logoColor=white"> <img src="https://img.shields.io/badge/Axios-5A29E4?style=for-the-badge&logo=axios&logoColor=white"> <img src="https://img.shields.io/badge/Framer%20Motion-0055FF?style=for-the-badge&logo=framer&logoColor=white">

| 구분 | 사용 기술 |
|------|----------|
| Language | TypeScript 5 |
| Framework | React 19, Vite 7 |
| 라우팅 | React Router DOM 7 |
| 상태 관리 | Zustand 5, TanStack Query 5 |
| 폼/검증 | React Hook Form, Zod |
| UI | Tailwind CSS v4, Radix UI, Framer Motion, lucide-react, sonner |
| 통신 | Axios, @microsoft/fetch-event-source, event-source-polyfill (SSE) |
| 기타 | jwt-decode, react-webcam |

---

### 🖥️ Admin Frontend (관리자)

<img src="https://img.shields.io/badge/TypeScript-3178C6?style=for-the-badge&logo=typescript&logoColor=white"> <img src="https://img.shields.io/badge/React%2019-61DAFB?style=for-the-badge&logo=react&logoColor=black"> <img src="https://img.shields.io/badge/Vite%206-646CFF?style=for-the-badge&logo=vite&logoColor=white"> <img src="https://img.shields.io/badge/Three.js-000000?style=for-the-badge&logo=threedotjs&logoColor=white"> <img src="https://img.shields.io/badge/React%20Three%20Fiber-000000?style=for-the-badge&logo=threedotjs&logoColor=white"> <img src="https://img.shields.io/badge/Tailwind%20CSS%20v4-06B6D4?style=for-the-badge&logo=tailwindcss&logoColor=white"> <img src="https://img.shields.io/badge/Zustand-443E38?style=for-the-badge&logo=react&logoColor=white"> <img src="https://img.shields.io/badge/React%20Router-CA4245?style=for-the-badge&logo=reactrouter&logoColor=white"> <img src="https://img.shields.io/badge/React%20Hook%20Form-EC5990?style=for-the-badge&logo=reacthookform&logoColor=white"> <img src="https://img.shields.io/badge/Zod-3E67B1?style=for-the-badge&logo=zod&logoColor=white"> <img src="https://img.shields.io/badge/Framer%20Motion-0055FF?style=for-the-badge&logo=framer&logoColor=white"> <img src="https://img.shields.io/badge/Axios-5A29E4?style=for-the-badge&logo=axios&logoColor=white">

| 구분 | 사용 기술 |
|------|----------|
| Language | TypeScript 5 |
| Framework | React 19, Vite 6 |
| 3D | Three.js, @react-three/fiber, @react-three/drei |
| 라우팅 | React Router DOM 7 |
| 상태 관리 | Zustand 5 |
| 폼/검증 | React Hook Form, Zod |
| UI | Tailwind CSS v4, Framer Motion, lucide-react, react-toastify |
| 통신 | Axios, @microsoft/fetch-event-source (SSE) |

---

### 💾 Database & Infra

<img src="https://img.shields.io/badge/MySQL%208-4479A1?style=for-the-badge&logo=mysql&logoColor=white"> <img src="https://img.shields.io/badge/Redis%207-DC382D?style=for-the-badge&logo=redis&logoColor=white"> <img src="https://img.shields.io/badge/Mosquitto-660066?style=for-the-badge&logo=mqtt&logoColor=white"> <img src="https://img.shields.io/badge/Ubuntu%2022.04-E95420?style=for-the-badge&logo=ubuntu&logoColor=white"> <img src="https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white"> <img src="https://img.shields.io/badge/Docker%20Compose-2496ED?style=for-the-badge&logo=docker&logoColor=white"> <img src="https://img.shields.io/badge/Jenkins-D24939?style=for-the-badge&logo=jenkins&logoColor=white"> <img src="https://img.shields.io/badge/Nginx-009639?style=for-the-badge&logo=nginx&logoColor=white"> <img src="https://img.shields.io/badge/AWS%20EC2-FF9900?style=for-the-badge&logo=amazonec2&logoColor=white">

| 구분 | 사용 기술 |
|------|----------|
| RDBMS | MySQL 8 |
| Cache / 세션 | Redis 7-alpine |
| MQTT 브로커 | Eclipse Mosquitto 2 |
| OS | Ubuntu 22.04.5 LTS |
| Container | Docker, Docker Compose |
| CI/CD | Jenkins |
| Reverse Proxy | Nginx |
| Cloud | AWS EC2 |

---


# 🎯 주요 기능

**🔀 유저 플로우**

![유저플로우](https://github.com/user-attachments/assets/1997f12f-0e20-4b6f-aab5-c82b51bf5061)

---

**📱 사용자**


| <img src="https://github.com/user-attachments/assets/55cfd51e-ae75-4bde-ad80-e7de674adab7" width="250" height="600"> | <img src="https://github.com/user-attachments/assets/9e4e4a54-3f82-4fe9-a40b-0761e8fe9eb0" width="250" height="600"> | <img src="https://github.com/user-attachments/assets/314d5cd0-b132-44ff-90f3-1e38a4874c79" width="250" height="600"> |
|:---:|:---:|:---:|
| **호출** | **잠금 해제** | **반납** |

---

**💻 관리자**

| <img src="https://github.com/user-attachments/assets/d53137ad-fc7b-44df-9fcb-99b2dc7a907e" width="400" height="250"> | <img src="https://github.com/user-attachments/assets/5668b659-04ff-4ebe-843c-9ed98601ff71" width="400" height="250"> | <img src="https://github.com/user-attachments/assets/b79b75bf-2ddd-4716-9016-8741eeeffc27" width="400" height="250"> |
|:---:|:---:|:---:|
| **로봇 3D** | **사물함 배정** | **주행 모니터링** |

---

**🤖 로봇**

| <img src="https://github.com/user-attachments/assets/b9fe61b2-5d98-490b-804b-bebfe7327d71" width="400" height="250"> |
|:---:|
| **실제 로봇 주행** | 

---

# 🌐 아키텍처 구조

<img width="1632" height="903" alt="image" src="https://github.com/user-attachments/assets/68711a4a-5284-4aa0-a659-c42a7f39b2eb" />

---

# 📁 프로젝트 디렉토리 구조

<details>
<summary>📱 Frontend (User)</summary>

```
src/
│  App.tsx
│  index.css
│  main.tsx
│  vite-env.d.ts
│
├─api/
│      auth.api.ts
│      axios.ts
│      locker.api.ts
│      mission.api.ts
│      ticket.api.ts
│
├─assets/
│  └─fonts/
│          beckman-free.otf
│
├─components/
│  ├─auth/
│  │      LoginForm.tsx
│  │      PasswordInputField.tsx
│  │      TermsCheckbox.tsx
│  │
│  ├─common/
│  │      Logo.tsx
│  │      SSEProvider.tsx
│  │
│  ├─features/
│  │  └─mission/
│  │          ConnectionStatusBadge.tsx
│  │          MissionModals.tsx
│  │          MissionTimeline.tsx
│  │          RobotInfoCard.tsx
│  │
│  ├─home/
│  │      LockerStatusCard.tsx
│  │      RobotCallCard.tsx
│  │      RobotStatusCard.tsx
│  │      TicketSection.tsx
│  │      WelcomeSection.tsx
│  │
│  ├─layouts/
│  │      AppHeader.tsx
│  │
│  ├─mission/
│  │      ChecklistModal.tsx
│  │      CompleteModal.tsx
│  │      LocationSelector.tsx
│  │      MissionSummaryCard.tsx
│  │      NumpadKeyboard.tsx
│  │      ProgressBar.tsx
│  │      ReturningModal.tsx
│  │      TimelineStep.tsx
│  │      VerificationModal.tsx
│  │
│  ├─ticket/
│  │      CameraErrorView.tsx
│  │      ScanSuccessModal.tsx
│  │      TicketCard.tsx
│  │      WebcamScanner.tsx
│  │
│  └─ui/
│          badge.tsx / button.tsx / card.tsx
│          checkbox.tsx / input.tsx / tabs.tsx
│
├─constants/
│      locations.ts
│
├─domain/
│  └─mission/
│          stateMachine.ts
│
├─hooks/
│      useChecklistFlow.ts / useGlobalSSE.ts
│      useLockerData.ts / useLoginForm.ts
│      useMissionCreate.ts / useMissionFlow.ts
│      useSessionRestore.ts / useTicketData.ts / useTiltEffect.ts
│
├─lib/
│      utils.ts
│
├─pages/
│      CodeVerificationPage.tsx / HomePage.tsx
│      LoginPage.tsx / MissionCreatePage.tsx
│      MissionTrackPage.tsx / SplashPage.tsx
│      TicketDetailPage.tsx / TicketScanPage.tsx
│
├─routes/
│      index.tsx / ProtectedRoute.tsx
│
├─services/
│  └─storage/
│          ticketStorage.ts
│
├─store/
│      authStore.ts / missionStore.ts
│      sseStore.ts / ticketStore.ts
│
├─types/
│      auth.types.ts / locker.types.ts
│      mission.types.ts / ticket.types.ts
│
└─utils/
        array.ts / imageUtils.ts / validation.ts
```

</details>

<details>
<summary>💻 Frontend (Admin)</summary>

```
src/
│  App.tsx
│  index.css
│  main.tsx
│  vite-env.d.ts
│
├─api/
│      authApi.ts / axiosConfig.ts
│
├─components/
│  ├─dashboard/
│  │      ActiveTaskList.tsx / AssignToLockerModal.tsx
│  │      DashboardWidgets.tsx / LockerManagementModal.tsx
│  │      MapView.tsx / RobotDetailsPanel.tsx
│  │      StatCard.tsx / StatusCard.tsx
│  │
│  ├─layout/
│  │      MainLayout.tsx / Sidebar.tsx
│  │
│  ├─locker/
│  │      LockerSelectionModal.tsx / MiniLockerWidget.tsx
│  │
│  ├─mission/
│  │      MissionControlModal.tsx / MissionProcessModal.tsx / MissionReturnModal.tsx
│  │
│  ├─monitoring/
│  │      RealtimeActivityFeed.tsx
│  │
│  └─robot/
│          RobotActivityTerminal.tsx / RobotDetailModal.tsx
│          RobotReturnModal.tsx / RobotStage.tsx
│
├─hooks/
│      useRobotFetch.ts / UserRobotSSE.ts
│
├─lib/
│      utils.ts
│
├─pages/
│      AlertPage.tsx / JoinPage.tsx / LockersPage.tsx
│      LoginPage.tsx / RobotsPage.tsx
│
├─store/
│      lockerStore.ts / robotStore.ts
│      sseStore.ts / themeStore.ts
│
├─types/
│      auth.ts / locker.ts / robotEvents.ts
│
└─utils/
        navigationPaths.ts
```

</details>

<details>
<summary>☕ Backend</summary>

```
src/
├─main/
│  ├─java/com/e101/carryporter/
│  │  │  CarryporterApplication.java
│  │  │
│  │  ├─domain/
│  │  │  ├─admin/
│  │  │  │  ├─controller/       AdminController.java
│  │  │  │  ├─dto/request/      DispatchRequestDto / FinalizeRequestDto
│  │  │  │  │                   JoinRequestDto / LockRequestDto / LoginRequestDto
│  │  │  │  ├─dto/response/     LockerResponseDto / MissionResponseDto / RobotResponseDto
│  │  │  │  ├─entity/           AdminCredential.java
│  │  │  │  ├─event/            AdminLockRequestEvent / AdminUnlockRequestEvent
│  │  │  │  ├─repository/       AdminCredentialRepository.java
│  │  │  │  └─service/          AdminLockerService / AdminService
│  │  │  │
│  │  │  ├─auth/
│  │  │  │  ├─controller/       AuthController.java
│  │  │  │  ├─dto/request/      AuthRequestDto / LockRequestDto
│  │  │  │  │                   VerifyCodeRequestDto / VerifyPasswordRequestDto
│  │  │  │  ├─dto/response/     AuthResponseDto / TokenResponseDto
│  │  │  │  ├─repository/       EmailCodeRedisRepository / LoginFailCountRedisRepository
│  │  │  │  │                   RefreshTokenRedisRepository / TempPasswordRedisRepository
│  │  │  │  └─service/          AuthService.java
│  │  │  │
│  │  │  ├─location/
│  │  │  │  ├─entity/           Location.java
│  │  │  │  ├─exception/        LocationErrorCode.java
│  │  │  │  ├─repository/       LocationRepository.java
│  │  │  │  └─service/          LocationService.java
│  │  │  │
│  │  │  ├─locker/
│  │  │  │  ├─entity/           Locker / LockerStatus / UserLockerStatus
│  │  │  │  ├─exception/        LockerErrorCode.java
│  │  │  │  ├─repository/       LockerRepository.java
│  │  │  │  └─service/          LockerService.java
│  │  │  │
│  │  │  ├─mission/
│  │  │  │  ├─controller/       MissionController.java
│  │  │  │  ├─entity/           Mission / MissionStatus
│  │  │  │  ├─event/            MissionAbortedEvent / MissionCreatedEvent
│  │  │  │  │                   MissionFailedEvent / MissionFinalizedEvent
│  │  │  │  │                   MissionLockedEvent / MissionStartedEvent
│  │  │  │  │                   MissionStoredEvent / MissionUnlockedEvent
│  │  │  │  ├─exception/        MissionErrorCode.java
│  │  │  │  ├─listener/         FailureCountHandler / MissionStatusHandler
│  │  │  │  ├─repository/       MissionRepository.java
│  │  │  │  └─service/          MissionService.java
│  │  │  │
│  │  │  ├─robot/
│  │  │  │  ├─entity/           Robot / RobotRealTimeInfo / RobotStatus
│  │  │  │  ├─event/            RobotArrivalEvent / RobotAssignedEvent
│  │  │  │  │                   RobotAvailabilityChangedEvent / RobotEmergencyEvent
│  │  │  │  │                   RobotLogEvent / RobotReturnedEvent
│  │  │  │  ├─exception/        RobotErrorCode.java
│  │  │  │  ├─listener/         RobotAssignmentHandler / RobotRedisSyncHandler
│  │  │  │  ├─repository/       RobotAvailableQueueRepository / RobotMacMappingRepository
│  │  │  │  │                   RobotRealTimeRepository / RobotRepository
│  │  │  │  └─service/          RobotCacheService / RobotService
│  │  │  │
│  │  │  ├─sse/
│  │  │  │  ├─controller/       SseController / SseTestController
│  │  │  │  ├─listener/         AdminSseNotificationHandler / UserSseNotificationHandler
│  │  │  │  ├─repository/       SseEmitterRepository.java
│  │  │  │  └─service/          SseService.java
│  │  │  │
│  │  │  ├─ticket/
│  │  │  │  ├─controller/       TicketController.java
│  │  │  │  ├─entity/           Ticket.java
│  │  │  │  ├─repository/       TicketRepository.java
│  │  │  │  └─service/          OcrClient / TicketService
│  │  │  │
│  │  │  └─user/
│  │  │      ├─controller/      UserController.java
│  │  │      ├─entity/          Role / User
│  │  │      ├─event/           UserAuthFailedEvent / UserAuthSuccessEvent
│  │  │      ├─exception/       UserErrorCode.java
│  │  │      ├─repository/      UserRepository.java
│  │  │      └─service/         UserService.java
│  │  │
│  │  └─global/
│  │      ├─config/             AsyncConfig / JpaConfig / MqttConfig
│  │      │                     RedisConfig / RetryConfig / WebConfig
│  │      ├─entity/             BaseEntity.java
│  │      ├─exception/          BusinessException / ControllerAdvice
│  │      │                     ErrorCode / ErrorResponse
│  │      ├─filter/             AuthorizationFilter / CorsFilter / JwtAuthenticationFilter
│  │      ├─listener/           MqttCommandHandler.java
│  │      ├─service/mqtt/       MqttPublisherService / MqttSubscriberService
│  │      └─utils/              JwtUtils / MattermostClient
│  │
│  └─resources/
│          application.yml / application-test.yml
│
└─test/
    └─java/com/e101/carryporter/
        ├─domain/
        │  ├─admin/     AdminControllerTest / AdminServiceTest
        │  ├─auth/      AuthControllerTest / AuthServiceTest
        │  ├─location/  LocationServiceTest
        │  ├─locker/    LockerServiceTest
        │  ├─mission/   MissionControllerTest / MissionServiceTest
        │  ├─robot/     RobotServiceTest
        │  ├─sse/       SseControllerTest / SseServiceTest
        │  ├─ticket/    TicketControllerTest
        │  └─user/      UserControllerTest / UserServiceTest
        ├─global/
        │  ├─listener/  MqttCommandHandlerTest
        │  ├─service/   MqttPublisherServiceTest / MqttSubscriberServiceTest
        │  └─utils/     JwtUtilsTest / MattermostClientTest
        └─support/
                IntegrationTestSupport.java / WebMvcTestSupport.java
```

</details>

---

# 📦 프로젝트 산출물

**📹 Video Portfolio**

- [carryporter_14기 공통PJT 영상 포트폴리오 E101](https://youtu.be/zutxE7PEOgU)
- [공통PJT_carryporter발표](https://youtu.be/zutxE7PEOgU)
- [carryporter_기획배경(AI)](https://youtu.be/Lsbg-lAFVb0)
- [carryporter_교통약자시연영상(in SSAFY)](https://youtu.be/EMQk7KcneSM)

---

**🖼️ 화면 설계서**

사용자
<details>
<summary>자세히</summary>
<img width="1374" height="870" alt="image" src="https://github.com/user-attachments/assets/eb942dc2-6268-49bb-b455-bef42c45ec62" />


</details>

관리자
<details>
<summary>자세히</summary>
<img width="1541" height="776" alt="image" src="https://github.com/user-attachments/assets/c8640f77-fff3-492b-a988-cea3c3757931" />


</details>

---

**🗄️ ERD**

<details>
<summary>자세히</summary>

<img width="1850" height="1042" alt="ERD" src="https://github.com/user-attachments/assets/f2377254-f759-400f-a894-025a2eb07652" />

</details>

---

**📅 Jira Issues**

<details>
<summary>자세히</summary>


<img width="1842" height="1472" alt="s14p11e101____2026-02-19_11 10pm" src="https://github.com/user-attachments/assets/30b546a1-fca8-4d22-90b0-3f92bbaac656" />




</details>

---

**📋 기능 명세서**

<details>
<summary>자세히</summary>

<img width="638" height="1124" alt="image" src="https://github.com/user-attachments/assets/f013ca36-4add-4508-b0d3-1ded6342a39a" />


</details>

---

**📡 API 명세서**

<details>
<summary>자세히</summary>

<img width="1119" height="932" alt="image" src="https://github.com/user-attachments/assets/e784fa05-6fcc-43a5-86f1-31f731841b5f" />


</details>
