import { motion } from 'framer-motion';
import { X, Bot, Package, Play } from 'lucide-react';

interface MissionControlModalProps {
  robotCode: string;
  lockerId: number; // ✅ 추가됨
  onStart: () => void;
  onClose: () => void;
}

export default function MissionControlModal({ robotCode, lockerId, onStart, onClose }: MissionControlModalProps) {
  return (
    <div className="fixed inset-0 z-[9999] flex items-center justify-center px-4">
      <motion.div 
        initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}
        className="absolute inset-0 bg-slate-900/50 backdrop-blur-sm" onClick={onClose} 
      />
      
      <motion.div 
        initial={{ opacity: 0, scale: 0.95 }} animate={{ opacity: 1, scale: 1 }} exit={{ opacity: 0, scale: 0.95 }}
        className="relative bg-white border border-slate-200 rounded-xl w-full max-w-lg p-8 shadow-2xl overflow-hidden"
      >
        <div className="absolute top-0 left-0 w-full h-1.5 bg-gradient-to-r from-cyan-500 via-purple-500 to-amber-500" />
        
        {/* 헤더 */}
        <div className="flex justify-between items-start mb-6">
          <div>
            <h2 className="text-2xl font-black italic flex items-center gap-3 text-slate-900">
              <Bot className="text-cyan-600" size={28} /> READY TO DEPART
            </h2>
            <p className="text-xs text-slate-500 font-bold font-mono mt-1">MISSION SEQUENCE INITIATED</p>
          </div>
          <button onClick={onClose} className="p-2 bg-slate-100 rounded-full"><X size={20} /></button>
        </div>

        {/* 안내 메시지 카드 */}
        <div className="bg-amber-50 border border-amber-200 rounded-lg p-5 mb-6 flex gap-4 items-start">
            <div className="p-2 bg-white rounded-md text-amber-500 shadow-sm"><Package size={24}/></div>
            <div>
                <h3 className="font-bold text-slate-900 text-lg mb-1">박스 적재 요청</h3>
                <p className="text-sm text-slate-600 leading-relaxed">
                   <span className="font-bold text-cyan-600 text-lg mx-1">{lockerId}번 사물함</span>의 물품을 꺼내 <br/>
                   <span className="font-bold text-slate-900 text-lg mx-1">{robotCode}</span>에 적재해주세요.
                </p>
            </div>
        </div>

        {/* 하단 버튼 */}
        <div className="pt-4 border-t border-slate-100 flex gap-3">
             <button 
                onClick={onStart} // ✅ 출발 버튼
                className="flex-1 bg-slate-900 hover:bg-slate-800 text-white py-4 px-4 rounded-lg font-bold flex items-center justify-center gap-2 transition-all hover:scale-[1.02] shadow-lg shadow-slate-200"
             >
                <Play size={18} fill="currentColor" /> 적재 완료 및 출발
             </button>
        </div>
      </motion.div>
    </div>
  );
}