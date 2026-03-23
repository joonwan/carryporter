import { useEffect, useRef, useState } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import { 
  Bell, Zap, CheckCircle, AlertCircle, Info, Wifi, 
  MapPin, UserCheck, LockOpen, RotateCcw, BatteryCharging, Flag, Navigation 
} from 'lucide-react' // ✅ 추가된 아이콘들
import { useSseStore } from '@/store/sseStore'

// ActivityLog 타입 정의 확장
interface ActivityLog {
  id: string
  timestamp: Date
  type: string // 유연성을 위해 string으로 변경하거나 union type 확장
  robotCode?: string
  message: string
  icon: React.ReactNode
  color: string
  bgColor: string
  borderColor: string
}

export default function RealtimeActivityFeed() {
  const lastMessage = useSseStore(state => state.lastMessage)
  const [activities, setActivities] = useState<ActivityLog[]>([])
  const scrollRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    if (!lastMessage) return;

    const timestamp = new Date();
    let newActivity: ActivityLog | null = null;

    let eventNameHeader = '';
    let rawData = '';

    // 1. SSE 데이터 파싱 (EventName 헤더와 Data 분리)
    if (typeof lastMessage === 'string') {
      const lines = lastMessage.split('\n');
      lines.forEach(line => {
        if (line.startsWith('event:')) eventNameHeader = line.replace('event:', '').trim();
        else if (line.startsWith('data:')) rawData = line.replace('data:', '').trim();
      });
      if (!rawData && !eventNameHeader) rawData = lastMessage;
    }

    try {
      // --- A. 시스템 연결/핑 처리 ---
      if (eventNameHeader === 'CONNECT' || rawData.includes('Connected!')) {
        newActivity = {
          id: `${Date.now()}-conn`, timestamp, type: 'connect',
          message: `🌐 시스템 연결 성공: 실시간 모니터링 시작`,
          icon: <Wifi className="w-4 h-4" />,
          color: 'text-emerald-600', bgColor: 'bg-emerald-50', borderColor: 'border-emerald-200'
        };
      } 
      else if (eventNameHeader === 'heartbeat' || rawData === 'ping') {
        return; // 핑 무시
      } 
      // --- B. 비즈니스 로직 이벤트 처리 ---
      else {
        const parsed = JSON.parse(rawData);
        
        // 이벤트 타입 결정 (SSE 헤더 우선, 없으면 JSON 내부 eventName, 그것도 없으면 추론)
        const eventType = eventNameHeader || parsed.eventName || parsed.type;
        const rCode = parsed.robotCode || parsed.robotId || '-';

        switch (eventType) {
          // 1. 로봇 배정 (RobotAssignedEvent)
          case 'RobotAssignedEvent':
            newActivity = {
              id: `${Date.now()}-assign`, timestamp, type: 'assigned', robotCode: rCode,
              message: `🤖 [배정] ${rCode}번 로봇이 '${parsed.callLocationName}'(으)로 호출되었습니다.`,
              icon: <Zap className="w-4 h-4" />,
              color: 'text-amber-600', bgColor: 'bg-amber-50', borderColor: 'border-amber-200'
            };
            break;

          // 2. 미션 시작/이동 (MissionStartedEvent)
          case 'MissionStartedEvent':
            newActivity = {
              id: `${Date.now()}-start`, timestamp, type: 'moving', robotCode: rCode,
              message: `🚚 [이동] ${rCode}번 로봇이 목적지로 출발했습니다.`,
              icon: <Navigation className="w-4 h-4" />,
              color: 'text-blue-600', bgColor: 'bg-blue-50', borderColor: 'border-blue-200'
            };
            break;

          // 3. 목적지 도착 (RobotArrivalEvent)
          case 'RobotArrivalEvent':
            newActivity = {
              id: `${Date.now()}-arrive`, timestamp, type: 'arrival', robotCode: rCode,
              message: `📍 [도착] ${rCode}번 로봇이 지정된 위치에 도착했습니다.`,
              icon: <MapPin className="w-4 h-4" />,
              color: 'text-indigo-600', bgColor: 'bg-indigo-50', borderColor: 'border-indigo-200'
            };
            break;

          // 4. 사용자 인증 성공 (UserAuthSuccessEvent)
          case 'UserAuthSuccessEvent':
            newActivity = {
              id: `${Date.now()}-auth`, timestamp, type: 'auth', robotCode: rCode,
              message: `🔐 [인증] 사용자(${parsed.userId}) 인증에 성공했습니다.`,
              icon: <UserCheck className="w-4 h-4" />,
              color: 'text-teal-600', bgColor: 'bg-teal-50', borderColor: 'border-teal-200'
            };
            break;

          // 5. 잠금 해제 요청 (MissionLockRequestEvent)
          case 'MissionLockRequestEvent':
            newActivity = {
              id: `${Date.now()}-unlock`, timestamp, type: 'unlock', robotCode: rCode,
              message: `🔓 [개방] 로봇의 적재함 잠금 해제가 요청되었습니다.`,
              icon: <LockOpen className="w-4 h-4" />,
              color: 'text-purple-600', bgColor: 'bg-purple-50', borderColor: 'border-purple-200'
            };
            break;

          // 6. 복귀 시작 (ReturnStartedEvent)
          case 'ReturnStartedEvent':
            newActivity = {
              id: `${Date.now()}-return-start`, timestamp, type: 'return_start', robotCode: rCode,
              message: `↩️ [복귀] 충전 스테이션으로 복귀를 시작합니다.`,
              icon: <RotateCcw className="w-4 h-4" />,
              color: 'text-orange-600', bgColor: 'bg-orange-50', borderColor: 'border-orange-200'
            };
            break;

          // 7. 복귀 완료 (RobotReturnedEvent)
          case 'RobotReturnedEvent':
            newActivity = {
              id: `${Date.now()}-returned`, timestamp, type: 'returned', robotCode: rCode,
              message: `🔋 [완료] 로봇이 복귀하여 충전을 시작합니다.`,
              icon: <BatteryCharging className="w-4 h-4" />,
              color: 'text-slate-600', bgColor: 'bg-slate-100', borderColor: 'border-slate-300'
            };
            break;

          // 8. 미션 실패 (MissionFailedEvent)
          case 'MissionFailedEvent':
            newActivity = {
              id: `${Date.now()}-fail`, timestamp, type: 'error', robotCode: rCode,
              message: `🚨 [실패] 미션 오류: ${parsed.message || '알 수 없는 오류'}`,
              icon: <AlertCircle className="w-4 h-4" />,
              color: 'text-red-600', bgColor: 'bg-red-50', borderColor: 'border-red-200'
            };
            break;

          // 9. 미션 최종 종료 (MissionFinalizedEvent)
          case 'MissionFinalizedEvent':
            newActivity = {
              id: `${Date.now()}-final`, timestamp, type: 'success', robotCode: rCode,
              message: `🏁 [종료] 모든 프로세스가 정상적으로 마무리되었습니다.`,
              icon: <Flag className="w-4 h-4" />,
              color: 'text-green-600', bgColor: 'bg-green-50', borderColor: 'border-green-200'
            };
            break;

          default:
            // 정의되지 않은 이벤트지만 데이터는 있는 경우
            console.warn("Unknown Event Type:", eventType);
            newActivity = {
              id: `${Date.now()}-unknown`, timestamp, type: 'unknown',
              message: `알림: ${eventType || '새로운 이벤트'} 수신`,
              icon: <Info className="w-4 h-4" />,
              color: 'text-gray-500', bgColor: 'bg-gray-50', borderColor: 'border-gray-200'
            };
            break;
        }
      }
    } catch (e) {
      // JSON 파싱 실패 (일반 텍스트 로그)
      if (rawData && !rawData.includes('ping')) {
        newActivity = {
          id: `${Date.now()}-text`, timestamp, type: 'system',
          message: rawData,
          icon: <Info className="w-4 h-4" />,
          color: 'text-slate-500', bgColor: 'bg-slate-50', borderColor: 'border-slate-200'
        };
      }
    }

    if (newActivity) {
      setActivities(prev => [newActivity!, ...prev].slice(0, 50)); // 최대 50개 유지
    }
  }, [lastMessage]);

  // ... (formatTime 함수 및 return JSX 코드는 기존과 동일하게 유지)
  
  const formatTime = (date: Date) => {
    const now = new Date()
    const diff = Math.floor((now.getTime() - date.getTime()) / 1000)
    
    if (diff < 10) return '방금 전'
    if (diff < 60) return `${diff}초 전`
    if (diff < 3600) return `${Math.floor(diff / 60)}분 전`
    return date.toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit' })
  }

  return (
     // ... 기존 JSX 코드 (수정 불필요) ...
     <div className="h-full flex flex-col rounded-xl overflow-hidden border border-slate-300 bg-white shadow-xl">
       {/* (기존 헤더 코드) */}
       <div className="flex-none px-4 py-3 bg-gradient-to-r from-slate-50 to-blue-50 border-b border-slate-200">
         <div className="flex items-center justify-between">
           <div className="flex items-center gap-2">
             <motion.div
               animate={{ rotate: [0, 10, 0, -10, 0], scale: [1, 1.1, 1] }}
               transition={{ duration: 2, repeat: Infinity }}
             >
               <Bell className="w-4 h-4 text-cyan-600" />
             </motion.div>
             <h3 className="text-sm font-bold text-slate-800">실시간 활동</h3>
           </div>
           
           <motion.div
             key={activities.length}
             initial={{ scale: 1.3 }}
             animate={{ scale: 1 }}
             className="flex items-center gap-2"
           >
             <span className="text-xs font-bold text-slate-600">{activities.length}</span>
             <div className="w-2 h-2 rounded-full bg-emerald-500 shadow-[0_0_10px_rgba(16,185,129,0.8)] animate-pulse" />
           </motion.div>
         </div>
       </div>

       {/* 활동 목록 */}
       <div ref={scrollRef} className="flex-1 overflow-y-auto custom-scrollbar p-3 space-y-2 bg-slate-50">
         {activities.length === 0 ? (
           <div className="flex flex-col items-center justify-center h-full text-center py-8">
             <Bell className="w-8 h-8 text-slate-400 mb-2 opacity-50" />
             <p className="text-xs text-slate-500">활동 대기 중...</p>
           </div>
         ) : (
           <AnimatePresence mode="popLayout">
             {activities.map((activity, index) => (
               <motion.div
                 key={activity.id}
                 initial={{ opacity: 0, y: -20, scale: 0.9 }}
                 animate={{ opacity: 1, y: 0, scale: 1 }}
                 exit={{ opacity: 0, x: 100, scale: 0.8 }}
                 transition={{ type: "spring", stiffness: 500, damping: 30, delay: index * 0.05 }}
                 className="group relative"
               >
                 <div className={`
                   relative p-3 rounded-lg border ${activity.borderColor} ${activity.bgColor}
                   backdrop-blur-sm transition-all duration-300 hover:shadow-md cursor-pointer
                 `}>
                   <div className="relative flex gap-3">
                     <div className={`
                       flex-none w-8 h-8 rounded-lg ${activity.bgColor} ${activity.borderColor}
                       border flex items-center justify-center ${activity.color} shadow-sm
                     `}>
                       {activity.icon}
                     </div>
                     <div className="flex-1 min-w-0">
                       <p className="text-xs font-medium text-slate-800 leading-relaxed break-words">
                         {activity.message}
                       </p>
                       {activity.robotCode && activity.robotCode !== '-' && (
                         <span className={`
                           inline-block mt-1.5 px-2 py-0.5 rounded text-[10px] font-bold
                           ${activity.bgColor} ${activity.borderColor} border ${activity.color}
                         `}>
                           {activity.robotCode}
                         </span>
                       )}
                       <p className="text-[10px] text-slate-500 mt-1">
                         {formatTime(activity.timestamp)}
                       </p>
                     </div>
                   </div>
                 </div>
                 {index < activities.length - 1 && (
                   <div className="absolute left-7 top-full h-2 w-px bg-gradient-to-b from-slate-300 to-transparent" />
                 )}
               </motion.div>
             ))}
           </AnimatePresence>
         )}
       </div>
       
       {/* 하단 상태바 */}
       <div className="flex-none px-3 py-2 bg-gradient-to-r from-slate-50 to-cyan-50 border-t border-slate-200 flex items-center justify-between">
         <span className="text-[9px] font-medium text-slate-600">LIVE FEED</span>
         <div className="flex items-center gap-1.5">
           <div className="w-1.5 h-1.5 rounded-full bg-emerald-500" />
           <span className="text-[9px] font-bold text-emerald-600">ACTIVE</span>
         </div>
       </div>
     </div>
  )
}