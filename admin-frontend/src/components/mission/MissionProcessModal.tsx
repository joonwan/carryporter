import { useState, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import {
  User, Box, Lock, Send, CheckCircle, AlertCircle, RefreshCw, ChevronRight, MapPin
} from 'lucide-react';
import { api } from '@/api/axiosConfig';
import { RobotAssignedEvent } from '@/types/robotEvents';

interface LockerResponse {
  lockerId: number;
  lockerCode: string;
  status: 'AVAILABLE' | 'OCCUPIED';
}

interface MissionProcessModalProps {
  data: RobotAssignedEvent;
  onClose: () => void;
  onMissionStart: (missionId: number) => void;
}

type ProcessStep = 'LOCKER_SELECT' | 'LOCK_ROBOT' | 'READY_TO_START';

// 🎯 호출 위치별 목적지 매핑
const LOCATION_DESTINATIONS: Record<string, { name: string; color: string; bgColor: string; textColor: string }> = {
  'a': { name: 'Stop1', color: 'bg-red-500', bgColor: 'bg-red-50', textColor: 'text-red-600' },
  'c': { name: 'Gate', color: 'bg-blue-500', bgColor: 'bg-blue-50', textColor: 'text-blue-600' },
};

export default function MissionProcessModal({ data, onClose, onMissionStart }: MissionProcessModalProps) {
  const [step, setStep] = useState<ProcessStep>(
    data.requestType === 'RECALL' ? 'READY_TO_START' : 'LOCKER_SELECT'
  );

  const [selectedLockerCode, setSelectedLockerCode] = useState<string | null>(data.locker_code);
  const [tempSelectedLocker, setTempSelectedLocker] = useState<LockerResponse | null>(null);
  
  const [isProcessing, setIsProcessing] = useState(false);
  const [lockers, setLockers] = useState<LockerResponse[]>([]);
  const [isLoadingLockers, setIsLoadingLockers] = useState(false);

  // 🎯 callLocationName으로 목적지 정보 가져오기
  const destination = LOCATION_DESTINATIONS[data.callLocationName.toLowerCase()] || { 
    name: 'Unknown', 
    color: 'bg-gray-500',
    bgColor: 'bg-gray-50',
    textColor: 'text-gray-600'
  };

  useEffect(() => {
    if (data.requestType === 'FIRST') {
      fetchLockers();
    }
  }, [data.requestType]);

  const fetchLockers = async () => {
    setIsLoadingLockers(true);
    try {
      const res = await api.get<LockerResponse[]>('/api/admin/lockers'); 
      if (Array.isArray(res.data)) {
        setLockers(res.data);
      }
    } catch (err) {
      console.error("사물함 목록 로딩 실패:", err);
      setLockers([]);
    } finally {
      setIsLoadingLockers(false);
    }
  };

  const handleAssignLocker = async () => {
    if (!tempSelectedLocker) return;
    setIsProcessing(true);
    try {
      await api.post(`/api/admin/missions/${data.missionId}/lockers/${tempSelectedLocker.lockerId}`);
      console.log("✅ 배정 성공");
      setSelectedLockerCode(tempSelectedLocker.lockerCode);
      setStep('READY_TO_START'); 
    } catch (err) {
      console.error("❌ 사물함 배정 실패:", err);
      alert("사물함 배정에 실패했습니다. 다시 시도해주세요.");
    } finally {
      setIsProcessing(false);
    }
  };

  const handleStartMission = async () => {
    setIsProcessing(true);
    try {
      await api.post(`/api/admin/missions/${data.missionId}/dispatch`, {});
      onMissionStart(data.missionId);
    } catch (err) {
      console.error("❌ 출발 요청 실패:", err);
    } finally {
      setIsProcessing(false);
    }
  };

  return (
    <div className="fixed inset-0 z-[9999] flex items-center justify-center px-4">
      <motion.div 
        initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}
        className="absolute inset-0 bg-slate-900/70 backdrop-blur-sm"
        onClick={onClose}
      />

      <motion.div 
        initial={{ scale: 0.9, y: 20 }} animate={{ scale: 1, y: 0 }}
        className="relative bg-white w-full max-w-lg rounded-2xl shadow-2xl overflow-hidden flex flex-col max-h-[90vh]"
      >
        {/* 헤더 부분 */}
        <div className="bg-slate-50 p-6 border-b border-slate-200 flex-none">
          <div className="flex items-center justify-between mb-4">
            <span className={`px-2 py-1 rounded text-xs font-bold ${
              data.requestType === 'FIRST' ? 'bg-blue-100 text-blue-700' : 'bg-orange-100 text-orange-700'
            }`}>
              {data.requestType === 'FIRST' ? '📦 신규 보관 요청' : '🔄 물품 찾기(Recall)'}
            </span>
            <span className="text-slate-400 font-mono text-xs">Mission #{data.missionId}</span>
          </div>
          
          <h2 className="text-xl font-bold text-slate-800 mb-3">
            <span className={`${destination.textColor} font-bold`}>{destination.name}</span>
            <span className="font-normal text-slate-500"> 호출 처리</span>
          </h2>

          {/* 🎯 목적지 표시 */}
          <div className={`flex items-center gap-2 mt-3 p-3 ${destination.bgColor} rounded-lg border border-slate-200`}>
            <MapPin size={18} className={destination.textColor} />
            <span className="text-slate-600 text-sm font-medium">목적지:</span>
            <span className={`${destination.textColor} font-bold text-lg`}>{destination.name}</span>
          </div>
        </div>

        <div className="overflow-y-auto flex-1 p-6">
          {/* --- [STEP 1] 사물함 선택 --- */}
          {step === 'LOCKER_SELECT' && (
            <div className="space-y-6">
              <div className="bg-blue-50 border border-blue-100 p-4 rounded-lg">
                <p className="text-blue-800 text-sm font-bold flex items-center gap-2">
                  <AlertCircle size={16}/> 사물함 선택
                </p>
                <p className="text-blue-600 text-xs mt-1">
                  배정할 빈 사물함을 선택한 후 하단의 '배정 확정' 버튼을 눌러주세요.
                </p>
              </div>

              <div className="grid grid-cols-4 gap-2">
                {isLoadingLockers ? (
                  <div className="col-span-4 py-10 text-center text-slate-400 text-sm">로딩 중...</div>
                ) : (
                  lockers.map((locker) => {
                    const isAvailable = locker.status === 'AVAILABLE';
                    const isSelected = tempSelectedLocker?.lockerId === locker.lockerId;
                    return (
                      <button 
                        key={locker.lockerId}
                        onClick={() => isAvailable && setTempSelectedLocker(locker)}
                        disabled={!isAvailable || isProcessing}
                        className={`
                          py-3 border rounded-lg font-mono font-bold transition-all
                          ${isSelected 
                            ? 'border-blue-500 bg-blue-500 text-white shadow-md scale-105' 
                            : isAvailable 
                              ? 'border-green-200 bg-green-50 text-green-700 hover:bg-green-100' 
                              : 'border-slate-100 bg-slate-100 text-slate-400 opacity-50 cursor-not-allowed'}
                        `}
                      >
                        {locker.lockerCode}
                      </button>
                    );
                  })
                )}
              </div>

              {/* 배정 확정 버튼 */}
              <AnimatePresence>
                {tempSelectedLocker && (
                  <motion.div initial={{ y: 20, opacity: 0 }} animate={{ y: 0, opacity: 1 }} exit={{ y: 20, opacity: 0 }}>
                    <button 
                      onClick={handleAssignLocker}
                      disabled={isProcessing}
                      className="w-full bg-blue-600 text-white py-4 rounded-xl font-bold flex items-center justify-center gap-2 hover:bg-blue-700 shadow-lg"
                    >
                      {isProcessing ? '배정 중...' : <>{tempSelectedLocker.lockerCode}번 사물함 배정 확정 <ChevronRight size={18}/></>}
                    </button>
                  </motion.div>
                )}
              </AnimatePresence>
            </div>
          )}

          {/* --- [STEP 3] 최종 출발 --- */}
          {step === 'READY_TO_START' && (
            <div className="text-center py-4 space-y-6">
              <div className="bg-green-50 p-6 rounded-2xl border border-green-100">
                <div className="flex flex-col items-center gap-2 text-green-700">
                  <CheckCircle size={40} className="mb-2" />
                  <span className="font-bold text-lg">준비 완료</span>
                </div>
              </div>

              {/* 🎯 출발 전 목적지 재확인 */}
              <div className={`${destination.bgColor} p-4 rounded-xl border border-slate-200`}>
                <div className="flex items-center justify-center gap-3">
                  <MapPin size={20} className={destination.textColor} />
                  <span className="text-slate-600 font-medium">출발 목적지:</span>
                  <span className={`${destination.textColor} font-bold text-xl`}>{destination.name}</span>
                </div>
              </div>

              <button 
                onClick={handleStartMission}
                disabled={isProcessing}
                className="w-full bg-blue-600 text-white py-4 rounded-xl font-bold flex items-center justify-center gap-2 hover:bg-blue-500 shadow-lg shadow-blue-200"
              >
                {isProcessing ? '전송 중...' : <><Send size={18} /> 미션 출발 (START)</>}
              </button>
            </div>
          )}
        </div>
      </motion.div>
    </div>
  );
}