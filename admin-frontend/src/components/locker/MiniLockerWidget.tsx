import { useMemo } from 'react'
import { motion } from 'framer-motion'
import { Package, Grid3X3, Lock, Unlock } from 'lucide-react'

// 백엔드 DTO 타입 정의
export interface LockerResponseDto {
  lockerId: number
  lockerCode: string
  status: 'AVAILABLE' | 'OCCUPIED'
}

interface MiniLockerWidgetProps {
  onClick: () => void
  lockers: LockerResponseDto[] // 기존 stats 대신 구체적인 리스트를 받음
}

export default function MiniLockerWidget({ onClick, lockers = [] }: MiniLockerWidgetProps) {
  // 통계 계산
  const stats = useMemo(() => {
    const total = lockers.length
    const used = lockers.filter(l => l.status === 'OCCUPIED').length
    const available = total - used
    const percent = total > 0 ? Math.round((used / total) * 100) : 0
    return { total, used, available, percent }
  }, [lockers])

  return (
    <motion.div
      whileHover={{ scale: 1.01 }}
      whileTap={{ scale: 0.99 }}
      onClick={onClick}
      className="h-full w-full cursor-pointer relative group overflow-hidden rounded-xl border border-purple-500/30 bg-slate-900/60 backdrop-blur-md shadow-[0_0_30px_rgba(168,85,247,0.15)] flex"
    >
      {/* 🌟 배경 스캔라인 애니메이션 */}
      <motion.div
        animate={{ top: ['-100%', '200%'] }}
        transition={{ duration: 4, repeat: Infinity, ease: 'linear' }}
        className="absolute left-0 right-0 h-20 bg-gradient-to-b from-transparent via-purple-500/10 to-transparent pointer-events-none z-0"
      />

      {/* 📊 왼쪽: 요약 정보 (고정 너비) */}
      <div className="w-48 flex-none p-4 flex flex-col justify-between border-r border-purple-500/20 bg-slate-900/40 relative z-10">
        <div className="flex items-center gap-3">
          <div className="relative">
            <div className="absolute inset-0 bg-purple-500/20 blur-lg rounded-full animate-pulse" />
            <div className="relative w-10 h-10 rounded-lg bg-gradient-to-br from-purple-500/20 to-pink-500/20 border border-purple-400/30 flex items-center justify-center">
              <Package className="w-5 h-5 text-purple-400" />
            </div>
          </div>
          <div>
            <div className="text-2xl font-black text-white leading-none">{stats.available}</div>
            <div className="text-[10px] font-bold text-purple-400/60 tracking-wider mt-0.5">AVAILABLE</div>
          </div>
        </div>

        <div className="space-y-1">
           <div className="flex justify-between text-[10px] font-bold text-slate-400">
             <span>USAGE</span>
             <span className="text-purple-300">{stats.percent}%</span>
           </div>
           <div className="h-1.5 w-full bg-slate-800 rounded-full overflow-hidden">
             <motion.div 
               initial={{ width: 0 }}
               animate={{ width: `${stats.percent}%` }}
               transition={{ duration: 1 }}
               className="h-full bg-gradient-to-r from-purple-500 to-pink-500 relative"
             >
                <div className="absolute inset-0 bg-white/30 animate-[shimmer_2s_infinite]" />
             </motion.div>
           </div>
           <div className="flex justify-between text-[9px] text-slate-500 font-mono pt-1">
              <span>TOT: {stats.total}</span>
              <span>USE: {stats.used}</span>
           </div>
        </div>
      </div>

      {/* 🔲 오른쪽: 사물함 그리드 비주얼 */}
      <div className="flex-1 p-3 overflow-hidden relative z-10">
        <div className="h-full overflow-y-auto custom-scrollbar pr-1">
            {/* 그리드 레이아웃: 공간에 맞춰 자동 채움 */}
            <div className="grid grid-cols-8 gap-1.5 auto-rows-min">
              {lockers.map((locker, idx) => (
                <LockerItem key={locker.lockerId} locker={locker} index={idx} />
              ))}
              
              {/* 데이터가 없을 때 표시할 빈 슬롯들 (디자인 유지용) */}
              {lockers.length === 0 && Array.from({ length: 32 }).map((_, i) => (
                 <div key={i} className="aspect-square rounded bg-slate-800/20 border border-slate-800/50" />
              ))}
            </div>
        </div>
      </div>
      
      {/* 호버 시 전체 테두리 발광 */}
      <div className="absolute inset-0 rounded-xl border-2 border-purple-400/50 opacity-0 group-hover:opacity-100 transition-opacity pointer-events-none" />
    </motion.div>
  )
}

// 📦 개별 사물함 아이템 (컴포넌트 분리)
function LockerItem({ locker, index }: { locker: LockerResponseDto, index: number }) {
  const isOccupied = locker.status === 'OCCUPIED';

  return (
    <motion.div
      initial={{ opacity: 0, scale: 0 }}
      animate={{ opacity: 1, scale: 1 }}
      transition={{ delay: index * 0.02 }} // 순차적으로 나타나는 효과
      className={`
        aspect-square rounded-md flex flex-col items-center justify-center relative overflow-hidden transition-all duration-300
        ${isOccupied 
          ? 'bg-purple-600/20 border border-purple-400/60 shadow-[0_0_10px_rgba(168,85,247,0.4)]' 
          : 'bg-slate-800/30 border border-slate-700 hover:border-slate-500'
        }
      `}
    >
      {/* 사용 중일 때 배경 애니메이션 (심장 박동) */}
      {isOccupied && (
        <motion.div 
          animate={{ opacity: [0.3, 0.6, 0.3] }}
          transition={{ duration: 2, repeat: Infinity, ease: "easeInOut" }}
          className="absolute inset-0 bg-purple-500/30"
        />
      )}

      {/* 사물함 번호 */}
      <span className={`text-[10px] font-bold z-10 ${isOccupied ? 'text-purple-200' : 'text-slate-500'}`}>
        {locker.lockerCode}
      </span>

      {/* 아이콘 (작게) */}
      <div className="z-10 mt-[-2px]">
         {isOccupied ? (
            <Lock className="w-2.5 h-2.5 text-purple-400 fill-purple-400/50" />
         ) : (
            <div className="w-2.5 h-2.5 rounded-full border border-slate-600/50" />
         )}
      </div>
    </motion.div>
  )
}