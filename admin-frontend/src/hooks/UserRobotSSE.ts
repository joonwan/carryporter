import { useEffect } from 'react';
import { fetchEventSource } from '@microsoft/fetch-event-source';
import { useSseStore } from '@/store/sseStore';

export type { RobotData } from '@/store/sseStore';

export function useRobotSSE() {
  // Store에서 필요한 것들 가져오기
  const setIsConnected = useSseStore(state => state.setIsConnected);
  const setLastMessage = useSseStore(state => state.setLastMessage);
  const updateRobotStatus = useSseStore(state => state.updateRobotStatus);
  const setAlertEvent = useSseStore(state => state.setAlertEvent);

  useEffect(() => {
    // 토큰을 useEffect 내부에서 가져오기 (컴포넌트 마운트 시 한 번만)
    const token = localStorage.getItem('accessToken');

    if (!token) {
      console.error('🔒 토큰이 없습니다. 로그인해주세요.');
      return;
    }

    console.log('[SSE] 구독 시작');

    const controller = new AbortController();

    // 🎮 서버 이벤트 처리 로직
    const handleServerEvent = (eventName: string, data: any) => {
      // 백엔드 데이터에 robotCode나 robotId가 있다고 가정
      const targetId = data.robotCode || data.robotId;
      if (!targetId) return;

      // 이벤트별 로봇 상태 변경
      switch (eventName) {
        case 'RobotAssignedEvent': // 로봇 배정
          updateRobotStatus(targetId, { status: 'working', message: '배정 완료' });
          break;

        case 'MissionStartedEvent': // 미션 시작
          updateRobotStatus(targetId, { status: 'working', message: '미션 시작' });
          break;

        case 'ROBOT_RETURNED': // 로봇 복귀
          updateRobotStatus(targetId, {
            status: 'available',
            position: { x: 0, y: -5 },
            message: '복귀 완료'
          });
          break;

        default:
          break;
      }
    };

    const fetchData = async () => {
      await fetchEventSource('/api/sse/subscribe', {
        method: 'GET',
        headers: {
          Authorization: `Bearer ${token}`,
          'Content-Type': 'text/event-stream',
        },
        signal: controller.signal,
        openWhenHidden: true, // 탭이 백그라운드에 있어도 연결 유지 시도
        
        // 1. 연결 성공 시
        async onopen(response) {
          if (response.ok) {
            console.log('✅ SSE Connected!');
            setIsConnected(true);
            return; // OK
          } else {
            console.error('❌ SSE Connection Failed', response.status);
            throw new Error(`HTTP ${response.status}`);
          }
        },

        // 2. 메시지 수신 (이벤트 분기 처리)
        onmessage(msg) {
          console.log(`📩 Event Received: [${msg.event}]`, msg.data);

          // 🔇 시스템 메시지 필터링 (ping, Connected 등)
          const trimmedData = msg.data.trim();
          if (trimmedData === 'ping' || 
              trimmedData.startsWith('Connected') || 
              trimmedData === 'keep-alive' ||
              msg.event === 'ping' ||
              msg.event === 'heartbeat') {
            console.log('🔇 시스템 메시지 무시:', msg.event || trimmedData);
            return; // 무시하고 종료
          }

          // 🔥 SSE 원본 형식으로 화면에 전달 (실제 이벤트만)
          let rawMessage = '';
          if (msg.id) rawMessage += `id:${msg.id}\n`;
          if (msg.event) rawMessage += `event:${msg.event}\n`;
          rawMessage += `data:${msg.data}`;
          
          setLastMessage(rawMessage);

          try {
            // JSON 형식이 맞는지 확인하기 위해 파싱 시도
            const parsedData = JSON.parse(msg.data);
            setAlertEvent(msg.event);

            // 이벤트 종류에 따른 로봇 상태 업데이트 로직
            handleServerEvent(msg.event, parsedData);

          } catch (err) {
            // JSON이 아니라면(단순 텍스트 메시지) 여기서 처리
            console.warn("⚠️ JSON 형식이 아닙니다. 텍스트로 처리합니다:", msg.data);
          }
        },

        // 3. 에러 발생 시
        onerror(err) {
          console.error('❌ SSE Error:', err);
          setIsConnected(false);

          // AbortError는 정상 종료 → 재연결하지 않음
          if (err instanceof Error && err.name === 'AbortError') {
            throw err;
          }

          // throw하지 않으면 fetch-event-source가 자동 재연결함
        },

        // 4. 닫힘 처리
        onclose() {
          console.log('🔒 SSE Closed');
          setIsConnected(false);
        }
      });
    };

    fetchData();

    return () => {
      console.log('[SSE] 기존 연결 종료');
      controller.abort();
      setIsConnected(false);
    };
  }, []); // 빈 배열: 컴포넌트 마운트 시 한 번만 실행

  // Store 데이터 반환 (호환성 유지)
  const robots = useSseStore(state => state.robots);
  const isConnected = useSseStore(state => state.isConnected);
  const lastMessage = useSseStore(state => state.lastMessage);

  return { robots, isConnected, lastMessage };
}