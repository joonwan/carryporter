import React, { useEffect, useRef, useState } from 'react';
import { useSseStore } from '@/store/sseStore';

// 1. 이벤트 설정 맵
const EVENT_THEMES: Record<string, { text: string; color: string; sound: string }> = {
  RobotAssignedEvent: { text: '🤖 로봇 배정됨', color: '#39FF14', sound: 'ping.mp3' },
  MissionStartedEvent: { text: '🚀 로봇 출발!', color: '#39FF14', sound: 'ping.mp3' },
  RobotArrivalEvent: { text: '📍 로봇 도착!', color: '#00E5FF', sound: 'ping.mp3' },
  UserAuthSuccessEvent: { text: '✅ 인증 성공', color: '#39FF14', sound: 'ping.mp3' },
  MissionLockRequestEvent: { text: '🔒 잠금 요청 중', color: '#00E5FF', sound: 'lock.mp3' },
  ReturnStartedEvent: { text: '🏠 복귀 시작', color: '#FFFF00', sound: 'return.mp3' },
  MissionFailedEvent: { text: '⚠️ 미션 실패!', color: '#FF3131', sound: 'error.mp3' },
  RobotReturnedEvent: { text: '💤 복귀 완료', color: '#FFFF00', sound: 'done.mp3' },
  RobotEmergencyEvent: { text: '🚨 긴급 상황!', color: '#FF3131', sound: 'ping.mp3' },
};

const AlertPage = () => {
  const [currentAlert, setCurrentAlert] = useState<{ text: string; color: string } | null>(null);
  const audioRef = useRef<HTMLAudioElement>(null);

  const alertEvent = useSseStore((state) => state.alertEvent);
  const setAlertEvent = useSseStore((state) => state.setAlertEvent);

  useEffect(() => {
    if (!alertEvent) return;

    const theme = EVENT_THEMES[alertEvent] || { 
      text: alertEvent,
      color: '#FFFFFF',
      sound: 'ping.mp3'
    };

    triggerAlert(theme);

    const timer = setTimeout(() => setAlertEvent(null), 100);
    return () => clearTimeout(timer);

  }, [alertEvent, setAlertEvent]);

  const triggerAlert = (theme: { text: string; color: string; sound: string }) => {
    setCurrentAlert(theme);
    
    if (audioRef.current) {
      audioRef.current.currentTime = 0;
      audioRef.current.play().catch(() => console.log("상호작용 필요"));
    }

    setAlertEvent(null);
  };

  return (
    <div className="fixed inset-0 bg-black flex items-center justify-center overflow-hidden z-[9999]">
      <audio ref={audioRef} src="https://assets.mixkit.co/active_storage/sfx/2869/2869-preview.mp3" />

      {/* 대기 상태: 안녕하세요 표시 - 흐르는 애니메이션 */}
      {!currentAlert && (
        <div 
          className="whitespace-nowrap font-bold animate-slide-slow"
          style={{
            position: 'fixed',
            top: '50%',
            left: '0',
            transform: 'translateY(-50%)',
            color: '#FFFFFF',
            fontSize: 'clamp(3rem, 15vw, 12rem)',
            textShadow: '0 0 20px rgba(255,255,255,0.5)'
          }}
        >
          안녕하세요
        </div>
      )}

      {/* 알림 표시 - 오른쪽에서 왼쪽으로 흐름 */}
      {currentAlert && (
        <div 
          className="whitespace-nowrap font-[900] italic uppercase animate-slide"
          onAnimationEnd={() => setCurrentAlert(null)}
          style={{
            position: 'fixed',
            top: '50%',
            left: '0',
            transform: 'translateY(-50%)',
            color: currentAlert.color,
            fontSize: 'clamp(4rem, 20vw, 18vh)',
            textShadow: `0 0 30px ${currentAlert.color}, 0 0 60px ${currentAlert.color}`,
          }}
        >
          {currentAlert.text}
        </div>
      )}

      <style>{`
        @keyframes slide {
          0% { 
            left: 100%;
          }
          100% { 
            left: -100%;
          }
        }
        
        @keyframes slide-slow {
          0% { 
            left: 100%;
          }
          100% { 
            left: -100%;
          }
        }
        
        .animate-slide {
          animation: slide 3.5s linear forwards;
        }
        
        .animate-slide-slow {
          animation: slide-slow 8s linear infinite;
        }
      `}</style>
    </div>
  );
};

export default AlertPage;