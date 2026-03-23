import { useState, useEffect, useRef } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import { 
  Terminal, Zap, CheckCircle2, AlertTriangle, 
  XCircle, ArrowRight, Activity, Wifi, MapPin, Server 
} from 'lucide-react'

interface LogEntry {
  id: string
  timestamp: string
  robotId: string
  action: string
  type: 'info' | 'success' | 'warning' | 'error' | 'move' | 'connect'
}

interface RobotActivityTerminalProps {
  realLogs?: LogEntry[]
  className?: string
}

// 더미 데이터 생성기 (다양성 추가)
const generateDummyLog = (index: number): LogEntry => {
  const actions = [
    { action: 'GATE-1 통과 확인', type: 'move' as const },
    { action: '목적지 경로 연산 수행', type: 'info' as const },
    { action: '서버 데이터 동기화', type: 'connect' as const },
    { action: 'STOP-2 적재 작업 완료', type: 'success' as const },
    { action: 'LIDAR 센서 보정 중', type: 'info' as const },
    { action: '전방 장애물 감지: 감속', type: 'warning' as const },
    { action: '충전 도크 신호 수신', type: 'connect' as const },
    { action: '미션 프로세스 초기화', type: 'move' as const },
    { action: '네트워크 패킷 전송', type: 'info' as const },
    { action: '구역 진입: Zone-B', type: 'move' as const },
  ]
  
  const robots = ['SYS', 'R-01', 'R-02', 'R-03', 'R-04']
  const randomAction = actions[Math.floor(Math.random() * actions.length)]
  const now = new Date()
  
  return {
    id: `log-${Date.now()}-${index}`,
    timestamp: `${String(now.getHours()).padStart(2, '0')}:${String(now.getMinutes()).padStart(2, '0')}:${String(now.getSeconds()).padStart(2, '0')}`,
    robotId: robots[Math.floor(Math.random() * robots.length)],
    ...randomAction
  }
}

export default function RobotActivityTerminal({ realLogs = [], className = '' }: RobotActivityTerminalProps) {
  const [logs, setLogs] = useState<LogEntry[]>([])
  const bottomRef = useRef<HTMLDivElement>(null)

  // 실제 데이터 수신 처리
  useEffect(() => {
    if (realLogs.length > 0) setLogs(prev => [...prev, ...realLogs].slice(-30))
  }, [realLogs])

  // 더미 데이터 흐름 생성
  useEffect(() => {
    setLogs(Array.from({ length: 6 }, (_, i) => generateDummyLog(i)))
    
    const interval = setInterval(() => {
      setLogs(prev => {
        const updated = [...prev, generateDummyLog(Date.now())]
        // 너무 길어지지 않게 유지하되, 스크롤 흐름을 위해 적당량 유지
        return updated.length > 30 ? updated.slice(updated.length - 30) : updated
      })
    }, 1500) // 메인 피드보다 약간 빠르게 움직여서 "데이터가 먼저 들어온다"는 느낌

    return () => clearInterval(interval)
  }, [])

  // 자동 스크롤
  useEffect(() => {
    if (bottomRef.current) {
      bottomRef.current.scrollIntoView({ behavior: 'smooth' })
    }
  }, [logs])

  // 메인 피드와 색상 매칭 (Dark Mode Version)
  const getStyle = (type: LogEntry['type']) => {
    switch (type) {
      case 'success': return { color: 'text-emerald-400', icon: <CheckCircle2 size={10} />, dot: 'bg-emerald-500' }
      case 'warning': return { color: 'text-amber-400', icon: <AlertTriangle size={10} />, dot: 'bg-amber-500' }
      case 'error': return { color: 'text-rose-400', icon: <XCircle size={10} />, dot: 'bg-rose-500' }
      case 'move': return { color: 'text-blue-400', icon: <ArrowRight size={10} />, dot: 'bg-blue-500' }
      case 'connect': return { color: 'text-violet-400', icon: <Wifi size={10} />, dot: 'bg-violet-500' }
      default: return { color: 'text-slate-400', icon: <Activity size={10} />, dot: 'bg-slate-500' }
    }
  }

  return (
    <div className={`
      flex flex-col w-full h-full 
      bg-slate-950 rounded-xl border border-slate-800 shadow-2xl overflow-hidden
      font-mono text-[10px] select-none relative
      ${className}
    `}>
      {/* 배경 그리드 (미세하게) */}
      <div className="absolute inset-0 bg-[linear-gradient(rgba(30,41,59,0.3)_1px,transparent_1px),linear-gradient(90deg,rgba(30,41,59,0.3)_1px,transparent_1px)] bg-[length:20px_20px] pointer-events-none" />

      {/* 헤더 */}
      <div className="flex-none flex items-center justify-between px-3 py-2 border-b border-slate-800 bg-slate-900/80 backdrop-blur z-10">
        <div className="flex items-center gap-2">
          <Terminal size={12} className="text-slate-500" />
          <span className="font-bold text-slate-400 tracking-wide">SYSTEM STREAM</span>
        </div>
        <div className="flex items-center gap-1.5">
          <div className="flex gap-0.5 items-end h-3">
            <motion.div animate={{ height: [4, 8, 4] }} transition={{ repeat: Infinity, duration: 1 }} className="w-0.5 bg-emerald-500/50 rounded-full" />
            <motion.div animate={{ height: [6, 10, 5] }} transition={{ repeat: Infinity, duration: 1.2 }} className="w-0.5 bg-emerald-500/50 rounded-full" />
            <motion.div animate={{ height: [3, 7, 3] }} transition={{ repeat: Infinity, duration: 0.8 }} className="w-0.5 bg-emerald-500/50 rounded-full" />
          </div>
        </div>
      </div>

      {/* 로그 리스트 영역 */}
      <div className="flex-1 overflow-hidden relative group">
        {/* 상단 페이드아웃 (오래된 로그 자연스럽게 사라짐) */}
        <div className="absolute top-0 left-0 right-0 h-8 bg-gradient-to-b from-slate-950 to-transparent z-10 pointer-events-none" />

        <div className="h-full overflow-y-auto custom-scrollbar p-3 pl-2">
          {/* 타임라인 수직선 */}
          <div className="absolute left-[19px] top-3 bottom-3 w-px bg-slate-800/60" />

          <div className="space-y-3 relative">
            {logs.map((log, index) => {
              const style = getStyle(log.type)
              const isLast = index === logs.length - 1
              
              return (
                <motion.div
                  key={log.id}
                  initial={{ opacity: 0, x: -10 }}
                  animate={{ opacity: 1, x: 0 }}
                  transition={{ duration: 0.2 }}
                  className={`relative pl-6 flex items-start gap-2 ${isLast ? 'opacity-100' : 'opacity-70'}`}
                >
                  {/* 타임라인 점 (Node) */}
                  <div className={`
                    absolute left-[5px] top-[3px] w-[5px] h-[5px] rounded-full 
                    ${style.dot} ring-4 ring-slate-950 z-10
                    ${isLast ? 'animate-pulse shadow-[0_0_8px_rgba(255,255,255,0.3)]' : ''}
                  `} />

                  {/* 시간 & 로봇ID */}
                  <div className="flex flex-col shrink-0 min-w-[50px]">
                    <span className="text-slate-500 font-medium tracking-tighter leading-none mb-0.5">
                      {log.timestamp}
                    </span>
                    <span className={`text-[9px] font-bold ${isLast ? 'text-slate-200' : 'text-slate-600'}`}>
                      {log.robotId}
                    </span>
                  </div>

                  {/* 로그 내용 */}
                  <div className="flex-1 min-w-0 flex flex-col pt-[1px]">
                    <div className="flex items-center gap-1.5">
                      <span className={`opacity-80 ${style.color}`}>
                        {style.icon}
                      </span>
                      <span className={`truncate font-medium ${style.color}`}>
                        {log.action}
                      </span>
                    </div>
                    {/* 데이터 바이트 느낌의 장식 (랜덤한 16진수) */}
                    <span className="text-[8px] text-slate-700 font-mono mt-0.5 truncate">
                      0x{log.id.slice(-4).toUpperCase()} :: HEAP_{(index * 1234) % 9999}
                    </span>
                  </div>
                </motion.div>
              )
            })}
            <div ref={bottomRef} className="h-1" />
          </div>
        </div>
      </div>

      {/* 스크롤바 숨김 */}
      <style>{`
        .custom-scrollbar::-webkit-scrollbar { width: 0px; }
        .custom-scrollbar { scrollbar-width: none; }
      `}</style>
    </div>
  )
}