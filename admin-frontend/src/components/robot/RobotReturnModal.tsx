import { useState } from 'react';
import { motion } from 'framer-motion';
import { 
  X, Unlock, Box, CheckCircle, BatteryCharging, 
  Archive, RotateCcw, User 
} from 'lucide-react';
import { api } from '@/api/axiosConfig'; 
import { RobotReturnedAdminEvent } from '@/types/robotEvents';
import { toast } from 'react-toastify';

interface Props {
  data: RobotReturnedAdminEvent;
  onClose: () => void;
  onComplete: () => void;
}

// 단계 정의: 문열기 대기 -> 작업(보관/반납) 선택 -> 충전 복귀
type Step = 'WAIT_OPEN' | 'PROCESS_TASK' | 'READY_TO_CHARGE';
type ActionType = 'STORE' | 'RETURN' | null;

export default function MissionReturnModal({ data, onClose, onComplete }: Props) {
  
  const [step, setStep] = useState<Step>('WAIT_OPEN');
  const [actionType, setActionType] = useState<ActionType>(null);
  const [isProcessing, setIsProcessing] = useState(false);

  // 1️⃣ [API] 로봇 도어 잠금 해제 (UNLOCK) 요청
  const handleUnlockRobot = async () => {
    setIsProcessing(true);
    try {
      console.log(`📡 [API] 잠금 해제 요청: Mission=${data.missionId}`);
      await api.post(`api/admin/missions/${data.missionId}/unlock`, {});

      console.log("✅ 도어 개방 성공");
      setStep('PROCESS_TASK'); 

    } catch (err) {
      console.error("❌ 도어 개방 요청 실패:", err);
      toast.error("도어 개방 요청에 실패했습니다.");
    } finally {
      setIsProcessing(false);
    }
  };

  // 2️⃣ [API] 작업 완료 및 사물함 상태 업데이트
  // 보관(STORE) -> OCCUPIED (사용중)
  // 반납(RETURN) -> AVAILABLE (빈 상태)
  const handleTaskComplete = async () => {
    if (!actionType) {
        toast.warning("보관 또는 반납을 선택해주세요.");
        return;
    }

    setIsProcessing(true);
    
    try {
      // 1. 상태값 결정
      const targetStatus = actionType === 'STORE' ? 'OCCUPIED' : 'AVAILABLE';
      
      console.log(`📡 [API] 사물함 상태 변경 요청: ID=${data.lockerId}, Status=${targetStatus}`);

      // 2. API 호출 (PATCH /lockers/{lockerId}/status)
      // data.lockerId가 필요합니다. Event 객체에 포함되어 있다고 가정합니다.
      await api.patch(`api/admin/lockers/${data.lockerId}/status`, {
        status: targetStatus
      });

      console.log(`✅ 작업 완료 및 사물함 상태 변경 성공: ${targetStatus}`);
      
      // 3. 다음 단계로 이동
      setStep('READY_TO_CHARGE');

    } catch (err) {
      console.error("❌ 사물함 상태 변경 실패:", err);
      toast.error("사물함 상태 업데이트에 실패했습니다.");
    } finally {
      setIsProcessing(false);
    }
  };

  // 3️⃣ [API] 충전 복귀 요청
  const handleGoCharge = async () => {
    setIsProcessing(true);

    try {
      console.log(`📡 [API] 충전 복귀 요청: Mission=${data.missionId}`);
      
      // 실제 충전 복귀 API 엔드포인트 호출 (예시)
      // await api.post(`api/admin/missions/${data.missionId}/return`, {});
      
      // (임시 시뮬레이션)
      await new Promise(resolve => setTimeout(resolve, 800));

      toast.success(`${data.robotCode} 충전 스테이션으로 이동합니다.`);
      onComplete(); // 모달 닫기

    } catch (err) {
      console.error("❌ 충전 복귀 요청 실패:", err);
      toast.error("충전 복귀 요청 실패");
    } finally {
      setIsProcessing(false);
    }
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
          
          <h2 className="text-xl font-bold text-slate-800 mb-2">
             관리소 도착 완료
          </h2>
          <div className="flex items-center gap-4 text-sm text-slate-600">
             <div className="flex items-center gap-1"><User size={14}/> {data.userId}</div>
             <div className="flex items-center gap-1"><div className="w-2 h-2 rounded-full bg-green-500"/> {data.robotCode}</div>
          </div>
        </div>

        {/* 바디 */}
        <div className="overflow-y-auto flex-1 p-6 custom-scrollbar">
            
          {/* --- [STEP 1] 도어 개방 --- */}
          {step === 'WAIT_OPEN' && (
            <div className="text-center py-4">
               <div className="w-20 h-20 bg-slate-100 rounded-full flex items-center justify-center mx-auto mb-6 text-slate-400">
                 <Unlock size={36} />
               </div>
               <h3 className="text-lg font-bold text-slate-800">도어 개방이 필요합니다</h3>
               <p className="text-slate-500 mt-2 mb-8 text-sm">
                 로봇이 도착했습니다.<br/>내부 물품 확인을 위해 잠금을 해제해주세요.
               </p>

               <button 
                 onClick={handleUnlockRobot}
                 disabled={isProcessing}
                 className="w-full bg-slate-900 text-white py-4 rounded-xl font-bold flex items-center justify-center gap-2 hover:bg-slate-800 transition-all disabled:opacity-50"
               >
                 {isProcessing ? '통신 중...' : <> <Unlock size={18} /> 도어 잠금 해제 (UNLOCK) </>}
               </button>
            </div>
          )}

          {/* --- [STEP 2] 작업 선택 (보관 vs 반납) --- */}
          {step === 'PROCESS_TASK' && (
            <div>
               <div className="bg-purple-50 border border-purple-100 p-4 rounded-lg mb-6 text-center">
                  <p className="text-purple-800 text-xs font-bold mb-1">TARGET LOCKER</p>
                  <p className="text-3xl font-black text-purple-600 tracking-widest">{data.lockerCode}</p>
                  <p className="text-purple-400 text-[10px] mt-1">해당 사물함을 사용하세요</p>
               </div>

               <p className="text-sm font-bold text-slate-700 mb-3">작업 유형 선택</p>
               <div className="grid grid-cols-2 gap-3 mb-6">
                 {/* 보관 버튼 */}
                 <button 
                   onClick={() => setActionType('STORE')}
                   className={`p-4 rounded-xl border-2 flex flex-col items-center gap-2 transition-all ${
                     actionType === 'STORE' 
                     ? 'border-blue-500 bg-blue-50 text-blue-700 shadow-md ring-2 ring-blue-200' 
                     : 'border-slate-100 bg-white text-slate-400 hover:border-slate-300'
                   }`}
                 >
                   <Archive size={24} />
                   <span className="font-bold text-sm">보관 (입고)</span>
                   <span className="text-[10px] opacity-70">➔ 사용중 상태로 변경</span>
                 </button>

                 {/* 반납 버튼 */}
                 <button 
                   onClick={() => setActionType('RETURN')}
                   className={`p-4 rounded-xl border-2 flex flex-col items-center gap-2 transition-all ${
                     actionType === 'RETURN' 
                     ? 'border-orange-500 bg-orange-50 text-orange-700 shadow-md ring-2 ring-orange-200' 
                     : 'border-slate-100 bg-white text-slate-400 hover:border-slate-300'
                   }`}
                 >
                   <RotateCcw size={24} />
                   <span className="font-bold text-sm">반납 (출고)</span>
                   <span className="text-[10px] opacity-70">➔ 빈 상태로 변경</span>
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
                 {isProcessing ? '처리 중...' : <> <CheckCircle size={18} /> 상태 변경 및 완료 </>}
               </button>
            </div>
          )}

          {/* --- [STEP 3] 충전 복귀 --- */}
          {step === 'READY_TO_CHARGE' && (
            <div className="text-center py-4">
               <motion.div 
                 initial={{ scale: 0.8, opacity: 0 }} animate={{ scale: 1, opacity: 1 }}
                 className="mb-6 bg-green-50 p-6 rounded-2xl border border-green-100"
               >
                  <div className="flex flex-col items-center gap-2 text-green-700">
                    <CheckCircle size={40} className="mb-2" />
                    <span className="font-bold text-lg">상태 업데이트 완료</span>
                    <p className="text-xs text-green-600 opacity-80">
                      사물함 상태가 
                      <span className="font-bold mx-1">
                        {actionType === 'STORE' ? "'사용중'" : "'사용 가능'"}
                      </span>
                      으로 변경되었습니다.
                    </p>
                  </div>
               </motion.div>

               <button 
                 onClick={handleGoCharge}
                 disabled={isProcessing}
                 className="w-full bg-slate-900 text-white py-4 rounded-xl font-bold flex items-center justify-center gap-2 hover:bg-slate-800 transition-all shadow-lg"
               >
                 {isProcessing ? '전송 중...' : <> <BatteryCharging size={18} /> 충전 스테이션 복귀 </>}
               </button>
            </div>
          )}

        </div>
      </motion.div>
    </div>
  );
}