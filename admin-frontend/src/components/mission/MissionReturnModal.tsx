import { useState } from 'react';
import { motion } from 'framer-motion';
import {
  X, Unlock, CheckCircle, BatteryCharging,
  Archive, RotateCcw, User, Send // ✅ Send 아이콘 추가 (선택사항)
} from 'lucide-react';
import { api } from '@/api/axiosConfig';
import { RobotReturnedAdminEvent } from '@/types/robotEvents';
import { toast } from 'react-toastify';

interface Props {
  data: RobotReturnedAdminEvent;
  onClose: () => void;
  onComplete: () => void;
}

type Step = 'WAIT_OPEN' | 'PROCESS_TASK' | 'READY_TO_CHARGE';
type ActionType = 'STORE' | 'RETURN' | null;

export default function MissionReturnModal({ data, onClose, onComplete }: Props) {
  
  const [step, setStep] = useState<Step>('WAIT_OPEN');
  const [actionType, setActionType] = useState<ActionType>(null);
  
  const [isProcessing, setIsProcessing] = useState(false);
  const [isSent, setIsSent] = useState(false); // ✅ 전송 완료 상태 추가

  // 1️⃣ [API] 로봇 도어 잠금 해제
  const handleUnlockRobot = async () => {
    setIsProcessing(true);
    try {
      await api.post(`/api/admin/missions/${data.missionId}/unlock`, {});
      // (테스트용 주석: 실제 API 호출 시 주석 해제)
      // await new Promise(resolve => setTimeout(resolve, 500)); 

      console.log("✅ 도어 개방 성공");
      setStep('PROCESS_TASK'); 
    } catch (err) {
      toast.error("도어 개방 요청에 실패했습니다.");
    } finally {
      setIsProcessing(false);
    }
  };

  // 2️⃣ [API] 작업 완료
  const handleTaskComplete = async () => {
    if (!actionType) {
        toast.warning("보관 또는 반납을 선택해주세요.");
        return;
    }
    setIsProcessing(true);
    
    try {
      if (actionType === 'RETURN') {
        await api.post(`/api/admin/missions/${data.missionId}/finalize`, {});
        toast.success("반납 처리 및 락커 해제가 완료되었습니다.");
      } else if (actionType === 'STORE') {
        await api.post(`/api/admin/missions/${data.missionId}/store`, {});
        toast.success("물품 보관 처리가 완료되었습니다.");
      }

      setTimeout(() => {
        setStep('READY_TO_CHARGE');
        setIsProcessing(false);
      }, 500);

    } catch (err) {
      console.error(err);
      toast.error("작업 처리에 실패했습니다.");
      setIsProcessing(false);
    }
  };

  // 3️⃣ [UI Only] 충전 복귀 요청 (수정된 부분)
  const handleGoCharge = async () => {
    setIsProcessing(true); // 1. 버튼 비활성화 및 '전송 중' 표시

    // TODO: 실제 API 호출 (예: await api.post(...))
    // 여기서는 통신하는 척 0.5초 대기
    await new Promise(resolve => setTimeout(resolve, 500));

    // 2. 상태 변경: 처리 끝(false) -> 전송 완료(true)
    setIsProcessing(false);
    setIsSent(true); 

    toast.success(`${data.robotCode} 충전 스테이션으로 이동합니다.`);
    
    // 3. 사용자가 "전송 완료" 문구를 볼 수 있게 0.8초 뒤에 모달 닫기
    setTimeout(() => {
      onComplete(); 
    }, 800);
  };

  return (
    <div className="fixed inset-0 z-[9999] flex items-center justify-center px-4">
      <motion.div 
        initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}
        className="absolute inset-0 bg-slate-900/70 backdrop-blur-sm"
      />

      <motion.div 
        initial={{ scale: 0.9, y: 20 }} animate={{ scale: 1, y: 0 }}
        className="relative bg-white w-full max-w-lg rounded-2xl shadow-2xl overflow-hidden flex flex-col max-h-[90vh]"
      >
        {/* 헤더 */}
        <div className="bg-slate-50 p-6 border-b border-slate-200 flex-none relative">
          <button onClick={onClose} className="absolute top-4 right-4 text-slate-400 hover:text-slate-600">
            <X size={20} />
          </button>

          <div className="flex items-center justify-between mb-4">
            <span className="px-2 py-1 rounded text-xs font-bold bg-purple-100 text-purple-700">
                🏁 로봇 복귀 감지
            </span>
            <span className="text-slate-400 font-mono text-xs">Mission #{data.missionId}</span>
          </div>
          
          <h2 className="text-xl font-bold text-slate-800 mb-2">관리소 도착 완료</h2>
          <div className="flex items-center gap-4 text-sm text-slate-600">
             <div className="flex items-center gap-1"><User size={14}/> {data.userId}</div>
             <div className="flex items-center gap-1"><div className="w-2 h-2 rounded-full bg-green-500"/> {data.robotCode}</div>
          </div>
        </div>

        {/* 바디 */}
        <div className="overflow-y-auto flex-1 p-6 custom-scrollbar">
            
          {/* STEP 1: 도어 개방 */}
          {step === 'WAIT_OPEN' && (
            <div className="text-center py-4">
               <div className="w-20 h-20 bg-slate-100 rounded-full flex items-center justify-center mx-auto mb-6 text-slate-400">
                 <Unlock size={36} />
               </div>
               <h3 className="text-lg font-bold text-slate-800">도어 개방이 필요합니다</h3>
               <p className="text-slate-500 mt-2 mb-8 text-sm">로봇이 도착했습니다.<br/>내부 물품 확인을 위해 잠금을 해제해주세요.</p>

               <button 
                 onClick={handleUnlockRobot}
                 disabled={isProcessing}
                 className="w-full bg-slate-900 text-white py-4 rounded-xl font-bold flex items-center justify-center gap-2 hover:bg-slate-800 transition-all disabled:opacity-50"
               >
                 {isProcessing ? '통신 중...' : <> <Unlock size={18} /> 도어 잠금 해제 (UNLOCK) </>}
               </button>
            </div>
          )}

          {/* STEP 2: 작업 선택 */}
          {step === 'PROCESS_TASK' && (
            <div>
               <div className="bg-purple-50 border border-purple-100 p-4 rounded-lg mb-6 text-center">
                  <p className="text-purple-800 text-xs font-bold mb-1">TARGET LOCKER</p>
                  <p className="text-3xl font-black text-purple-600 tracking-widest">{data.lockerCode}</p>
                  <p className="text-purple-400 text-[10px] mt-1">해당 사물함을 사용하세요</p>
               </div>

               <p className="text-sm font-bold text-slate-700 mb-3">작업 유형 선택</p>
               <div className="grid grid-cols-2 gap-3 mb-6">
                 <button 
                   onClick={() => setActionType('STORE')}
                   className={`p-4 rounded-xl border-2 flex flex-col items-center gap-2 transition-all ${
                     actionType === 'STORE' 
                     ? 'border-blue-500 bg-blue-50 text-blue-700 shadow-md' 
                     : 'border-slate-100 bg-white text-slate-400 hover:border-slate-300'
                   }`}
                 >
                   <Archive size={24} />
                   <span className="font-bold text-sm">보관 (입고)</span>
                 </button>

                 <button 
                   onClick={() => setActionType('RETURN')}
                   className={`p-4 rounded-xl border-2 flex flex-col items-center gap-2 transition-all ${
                     actionType === 'RETURN' 
                     ? 'border-orange-500 bg-orange-50 text-orange-700 shadow-md' 
                     : 'border-slate-100 bg-white text-slate-400 hover:border-slate-300'
                   }`}
                 >
                   <RotateCcw size={24} />
                   <span className="font-bold text-sm">반납 (출고)</span>
                 </button>
               </div>

               <button 
                 onClick={handleTaskComplete}
                 disabled={isProcessing || !actionType}
                 className={`w-full py-4 rounded-xl font-bold flex items-center justify-center gap-2 transition-all ${
                    actionType 
                    ? 'bg-blue-600 hover:bg-blue-700 text-white shadow-lg shadow-blue-200' 
                    : 'bg-slate-200 text-slate-400 cursor-not-allowed'
                  }`}
               >
                 {isProcessing ? '처리 중...' : <> <CheckCircle size={18} /> 작업 완료 및 도어 닫기 </>}
               </button>
            </div>
          )}

          {/* STEP 3: 충전 복귀 (✨ 수정됨) */}
          {step === 'READY_TO_CHARGE' && (
            <div className="text-center py-4">
               <motion.div 
                 initial={{ scale: 0.8, opacity: 0 }} animate={{ scale: 1, opacity: 1 }}
                 className="mb-6 bg-green-50 p-6 rounded-2xl border border-green-100"
               >
                 <div className="flex flex-col items-center gap-2 text-green-700">
                   <CheckCircle size={40} className="mb-2" />
                   <span className="font-bold text-lg">작업 처리 완료</span>
                   <p className="text-xs text-green-600 opacity-80">
                     {actionType === 'STORE' ? '물품이 사물함에 보관되었습니다.' : '물품이 사용자에게 반납되었습니다.'}
                   </p>
                 </div>
               </motion.div>

               {/* ✅ 버튼 상태 변화 로직 적용 */}
               <button 
                 onClick={handleGoCharge}
                 disabled={isProcessing || isSent} // 전송 중이거나, 전송 완료되면 클릭 방지
                 className={`
                   w-full py-4 rounded-xl font-bold flex items-center justify-center gap-2 transition-all shadow-lg
                   ${isSent 
                      ? 'bg-green-600 text-white scale-105' // 전송 완료 시 초록색
                      : 'bg-slate-900 text-white hover:bg-slate-800' // 기본 상태
                   }
                 `}
               >
                 {isProcessing ? (
                    '전송 중...' 
                 ) : isSent ? (
                    <> <CheckCircle size={18} /> 명령 전송 완료! </> // 완료 텍스트
                 ) : (
                    <> <BatteryCharging size={18} /> 충전 스테이션 복귀 </>
                 )}
               </button>
            </div>
          )}

        </div>
      </motion.div>
    </div>
  );
}