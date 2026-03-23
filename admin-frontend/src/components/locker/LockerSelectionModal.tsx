import { motion } from 'framer-motion';
import { X, Box, Lock, Unlock } from 'lucide-react';
import { cn } from '@/lib/utils';

interface LockerSelectionModalProps {
  onSelect: (id: number) => void; // 선택 시 실행될 함수
  onClose: () => void;
}

export default function LockerSelectionModal({ onSelect, onClose }: LockerSelectionModalProps) {
  // 더미 데이터 (실제론 API)
  const lockers = Array.from({ length: 32 }).map((_, i) => ({
    id: i + 1,
    status: Math.random() > 0.4 ? 'occupied' : 'available', 
  }));

  const stats = {
      total: 32,
      used: lockers.filter(l => l.status === 'occupied').length
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center px-4">
      <motion.div 
        initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}
        className="absolute inset-0 bg-slate-900/40 backdrop-blur-sm" onClick={onClose} 
      />
      
      <motion.div 
        initial={{ opacity: 0, scale: 0.95, y: 20 }} animate={{ opacity: 1, scale: 1, y: 0 }} exit={{ opacity: 0, scale: 0.95, y: 20 }}
        className="relative bg-white border border-slate-200 rounded-xl w-full max-w-4xl p-8 shadow-2xl overflow-hidden"
      >
        <div className="absolute top-0 left-0 w-full h-1.5 bg-gradient-to-r from-cyan-500 via-purple-500 to-amber-500" />
        
        {/* 헤더 */}
        <div className="flex justify-between items-start mb-6">
          <div>
            <h2 className="text-2xl font-black italic flex items-center gap-3 text-slate-900">
              <Box className="text-cyan-600" /> SELECT STORAGE UNIT
            </h2>
            <p className="text-xs text-slate-500 font-bold font-mono mt-1">로봇에 적재할 물품이 든 사물함을 선택하세요.</p>
          </div>
          <button onClick={onClose} className="p-2 text-slate-400 hover:text-slate-900 bg-slate-100 rounded-full"><X size={20} /></button>
        </div>

        {/* 그리드 */}
        <div className="grid grid-cols-8 gap-3 mb-6">
          {lockers.map((locker) => (
            <button
              key={locker.id}
              disabled={locker.status === 'occupied'} // 사용 중이면 클릭 불가
              onClick={() => onSelect(locker.id)}    // ✅ 클릭 시 부모에게 ID 전달
              className={cn(
                "aspect-square rounded-lg border flex flex-col items-center justify-center gap-1 transition-all shadow-sm",
                locker.status === 'occupied' 
                  ? "bg-amber-50 border-amber-200 text-amber-600 cursor-not-allowed opacity-60" 
                  : "bg-slate-50 border-slate-200 text-slate-500 hover:border-cyan-500 hover:text-cyan-600 hover:bg-white hover:scale-105 hover:shadow-md cursor-pointer"
              )}
            >
              {locker.status === 'occupied' ? <Lock size={16} /> : <Unlock size={16} />}
              <span className="text-xs font-mono font-bold">{locker.id < 10 ? `0${locker.id}` : locker.id}</span>
            </button>
          ))}
        </div>

        {/* 하단 요약 */}
        <div className="flex justify-between items-center pt-4 border-t border-slate-100">
           <div className="flex gap-4 text-xs font-mono font-bold text-slate-400">
             <span>AVAILABLE ({stats.total - stats.used})</span>
             <span className="text-amber-500">OCCUPIED ({stats.used})</span>
           </div>
        </div>

      </motion.div>
    </div>
  )
}