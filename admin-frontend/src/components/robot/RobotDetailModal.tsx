import { useState, Suspense } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import { X, Wifi, Cpu, Calendar, Clock, MapPin, User, FileText, Loader2, Activity, Zap, Signal, Package, TrendingUp, AlertCircle } from 'lucide-react'
import { cn } from '@/lib/utils'

// --- 3D 관련 임포트 ---
import { Canvas } from '@react-three/fiber'
import { useGLTF, Stage, OrbitControls, Html } from '@react-three/drei'

// --- Types ---
interface MissionHistory {
  missionId: string;
  userId: string;
  adminId: string | null;
  locationId: string;
  weight: number;
  status: 'COMPLETED' | 'IN_PROGRESS' | 'CANCELLED';
  times: {
    assigned: string;
    start: string;
    arrival: string;
    complete: string;
  };
}

interface RobotDetailProps {
  robot: any;
  onClose: () => void;
  modelUrl?: string;
}

// ✅ 실제 데이터처럼 보이는 가라 데이터 생성 함수
const generateRealisticMissionData = (robotId: string | number): MissionHistory[] => {
  const idString = String(robotId);
  const now = new Date();
  const missions: MissionHistory[] = [];
  
  // 최근 15개의 미션 생성
  for (let i = 0; i < 15; i++) {
    const daysAgo = Math.floor(i / 3);
    const baseTime = new Date(now);
    baseTime.setDate(baseTime.getDate() - daysAgo);
    baseTime.setHours(9 + (i % 3) * 3, Math.floor(Math.random() * 60), 0);
    
    const assignedTime = baseTime.toISOString();
    const startTime = new Date(baseTime.getTime() + 2 * 60000).toISOString(); // +2분
    const arrivalTime = new Date(baseTime.getTime() + (5 + Math.random() * 10) * 60000).toISOString(); // +5-15분
    const completeTime = new Date(baseTime.getTime() + (15 + Math.random() * 20) * 60000).toISOString(); // +15-35분
    
    const statuses: ('COMPLETED' | 'IN_PROGRESS' | 'CANCELLED')[] = ['COMPLETED', 'COMPLETED', 'COMPLETED', 'COMPLETED', 'IN_PROGRESS', 'CANCELLED'];
    const status = i === 0 ? 'IN_PROGRESS' : statuses[Math.floor(Math.random() * statuses.length)];
    
    const locations = ['GATE-1', 'GATE-2', 'STOP-1', 'STOP-2', 'DOCK-A', 'DOCK-B', 'ZONE-C1', 'ZONE-C2'];
    const userIds = ['USER-2401', 'USER-2402', 'USER-2403', 'USER-2404', 'USER-2405', 'USER-2406'];
    const adminIds = [null, null, null, 'ADMIN-01', 'ADMIN-02'];
    
    missions.push({
      missionId: `MSN-${String(99234 - i).padStart(5, '0')}`,
      userId: userIds[Math.floor(Math.random() * userIds.length)],
      adminId: adminIds[Math.floor(Math.random() * adminIds.length)],
      locationId: locations[Math.floor(Math.random() * locations.length)],
      weight: parseFloat((2 + Math.random() * 28).toFixed(1)),
      status,
      times: {
        assigned: assignedTime,
        start: status !== 'CANCELLED' ? startTime : '',
        arrival: status === 'COMPLETED' ? arrivalTime : '',
        complete: status === 'COMPLETED' ? completeTime : ''
      }
    });
  }
  
  return missions;
};

// ✅ 로봇별 실제같은 통계 데이터
const generateRobotStats = (robotId: string | number) => {
  const idString = String(robotId);
  const baseHash = idString.split('').reduce((acc, char) => acc + char.charCodeAt(0), 0);
  
  return {
    totalMissions: 1200 + (baseHash % 500),
    successRate: (95 + (baseHash % 4) + Math.random()).toFixed(1),
    uptime: (97 + (baseHash % 3) + Math.random()).toFixed(1),
    avgSpeed: (1.1 + (baseHash % 5) * 0.1).toFixed(1),
    totalDistance: ((baseHash % 50) + 450).toFixed(1),
    activeHours: 2400 + (baseHash % 800),
    lastMaintenance: new Date(Date.now() - (baseHash % 30) * 24 * 60 * 60 * 1000).toISOString().split('T')[0],
    nextMaintenance: new Date(Date.now() + ((30 - baseHash % 15)) * 24 * 60 * 60 * 1000).toISOString().split('T')[0]
  };
};

// ✅ 안전한 시간 변환 헬퍼 함수
const formatTime = (timeStr: string) => {
  if (!timeStr || !timeStr.includes('T')) return '--:--';
  try {
    return timeStr.split('T')[1].substring(0, 5);
  } catch (e) {
    return '--:--';
  }
};

const formatDate = (timeStr: string) => {
  if (!timeStr || !timeStr.includes('T')) return '--/--/--';
  try {
    const date = new Date(timeStr);
    return `${String(date.getMonth() + 1).padStart(2, '0')}/${String(date.getDate()).padStart(2, '0')}`;
  } catch (e) {
    return '--/--';
  }
};

// --- 🧊 3D Model Component ---
function RobotModel({ url }: { url: string }) {
  const { scene } = useGLTF(url)
  return <primitive object={scene} />
}

// --- 🧊 3D Canvas Wrapper ---
function Robot3DViewer({ modelUrl }: { modelUrl: string }) {
  return (
    <div className="w-full h-64 bg-gradient-to-br from-slate-900 via-slate-800 to-slate-900 rounded-2xl overflow-hidden relative border border-slate-700 shadow-2xl">
      {/* 배경 그리드 효과 */}
      <div className="absolute inset-0 opacity-10">
        <div className="absolute inset-0" style={{
          backgroundImage: `linear-gradient(rgba(96, 165, 250, 0.3) 1px, transparent 1px),
                           linear-gradient(90deg, rgba(96, 165, 250, 0.3) 1px, transparent 1px)`,
          backgroundSize: '20px 20px'
        }} />
      </div>

      <Canvas shadows dpr={[1, 2]} camera={{ fov: 50 }}>
        <Suspense fallback={<LoaderHtml />}>
          <Stage environment="city" intensity={0.6} adjustCamera={1.2}>
            <RobotModel url={modelUrl} />
          </Stage>
        </Suspense>
        <OrbitControls 
          autoRotate 
          autoRotateSpeed={2} 
          makeDefault 
          minPolarAngle={Math.PI / 4}
          maxPolarAngle={Math.PI / 1.5}
        />
      </Canvas>
      
      {/* 3D 뷰어 오버레이 UI */}
      <div className="absolute top-4 left-4 flex items-center gap-2 bg-black/40 backdrop-blur-md px-3 py-2 rounded-xl border border-white/10">
        <div className="w-2 h-2 rounded-full bg-cyan-400 animate-pulse" />
        <span className="text-[10px] font-bold text-white tracking-wider">LIVE 3D MODEL</span>
      </div>

      <div className="absolute bottom-4 right-4 flex gap-2">
        <div className="bg-black/40 backdrop-blur-md px-3 py-1.5 rounded-lg border border-white/10 text-[9px] font-mono text-cyan-400">
          <Activity className="inline w-3 h-3 mr-1" />
          INTERACTIVE
        </div>
      </div>
    </div>
  )
}

// 3D 로딩 중 표시할 UI
function LoaderHtml() {
  return (
    <Html center>
      <div className="flex flex-col items-center gap-3 text-cyan-400">
        <Loader2 className="animate-spin" size={32} />
        <div className="flex flex-col items-center gap-1">
          <span className="text-xs font-bold tracking-wider">LOADING MODEL</span>
          <div className="flex gap-1">
            <div className="w-1.5 h-1.5 bg-cyan-400 rounded-full animate-bounce" style={{ animationDelay: '0ms' }} />
            <div className="w-1.5 h-1.5 bg-cyan-400 rounded-full animate-bounce" style={{ animationDelay: '150ms' }} />
            <div className="w-1.5 h-1.5 bg-cyan-400 rounded-full animate-bounce" style={{ animationDelay: '300ms' }} />
          </div>
        </div>
      </div>
    </Html>
  )
}

// --- 🎨 Sub-Component: 배터리 위젯 ---
function BatteryWidget({ level }: { level: number }) {
  const batteryColor = level > 60 ? 'from-emerald-500 to-cyan-500' : level > 20 ? 'from-amber-500 to-orange-500' : 'from-red-500 to-rose-600';
  const glowColor = level > 60 ? 'rgba(16, 185, 129, 0.3)' : level > 20 ? 'rgba(245, 158, 11, 0.3)' : 'rgba(239, 68, 68, 0.3)';

  return (
    <div className="relative bg-gradient-to-br from-slate-50 to-white p-5 rounded-2xl border border-slate-200 shadow-lg overflow-hidden group hover:shadow-xl transition-all duration-300">
      {/* 배경 그라데이션 효과 */}
      <div className="absolute top-0 right-0 w-24 h-24 bg-gradient-to-br from-cyan-500/5 to-blue-500/5 rounded-full blur-2xl" />
      
      <div className="relative flex items-center gap-4">
        {/* 배터리 아이콘 */}
        <div className="relative">
          <div className="w-16 h-8 border-3 border-slate-300 rounded-lg p-1 relative bg-white shadow-inner">
            {/* 배터리 캡 */}
            <div className="absolute -right-1.5 top-2 h-4 w-2 bg-slate-300 rounded-r-md" />
            
            {/* 배터리 레벨 */}
            <motion.div 
              initial={{ width: 0 }} 
              animate={{ width: `${level}%` }} 
              transition={{ duration: 1.5, ease: "easeOut" }}
              className={cn(
                "h-full rounded-md relative overflow-hidden bg-gradient-to-r",
                batteryColor
              )}
              style={{
                boxShadow: `0 0 15px ${glowColor}`
              }}
            >
              <div className="absolute top-0 left-0 w-full h-1/2 bg-white/30 rounded-md" />
              {level > 20 && (
                <motion.div
                  animate={{ x: ['-100%', '100%'] }}
                  transition={{ duration: 2, repeat: Infinity, ease: "linear" }}
                  className="absolute top-0 left-0 w-1/3 h-full bg-white/40 skew-x-12"
                />
              )}
            </motion.div>
          </div>
          
          {/* 레벨 인디케이터 */}
          {level > 20 && (
            <motion.div
              animate={{ opacity: [0.5, 1, 0.5] }}
              transition={{ duration: 2, repeat: Infinity }}
              className="absolute -top-1 -right-1 w-3 h-3 bg-cyan-400 rounded-full shadow-lg shadow-cyan-400/50"
            />
          )}
        </div>

        {/* 텍스트 정보 */}
        <div className="flex-1">
          <div className="text-[10px] text-slate-500 font-bold uppercase tracking-wider mb-0.5">
            Power Level
          </div>
          <div className="flex items-baseline gap-2">
            <span className="text-3xl font-black bg-gradient-to-r from-slate-800 to-slate-600 bg-clip-text text-transparent">
              {level}
            </span>
            <span className="text-lg font-bold text-slate-400">%</span>
          </div>
          <div className="mt-1 flex items-center gap-1.5">
            <Zap className={cn(
              "w-3 h-3",
              level > 20 ? "text-cyan-500" : "text-red-500"
            )} />
            <span className="text-[9px] font-semibold text-slate-400">
              {level > 60 ? 'Optimal' : level > 20 ? 'Moderate' : 'Low Power'}
            </span>
          </div>
        </div>
      </div>
    </div>
  )
}

// --- 📡 네트워크 위젯 ---
function NetworkWidget() {
  return (
    <div className="relative bg-gradient-to-br from-slate-50 to-white p-5 rounded-2xl border border-slate-200 shadow-lg overflow-hidden group hover:shadow-xl transition-all duration-300">
      {/* 배경 효과 */}
      <div className="absolute top-0 left-0 w-24 h-24 bg-gradient-to-br from-green-500/5 to-emerald-500/5 rounded-full blur-2xl" />
      
      <div className="relative">
        <div className="text-[10px] text-slate-500 font-bold uppercase tracking-wider mb-2">
          Network Status
        </div>
        
        <div className="flex items-center gap-3 mb-3">
          <div className="relative">
            <Signal className="w-8 h-8 text-emerald-500" />
            <motion.div
              animate={{ scale: [1, 1.3, 1], opacity: [1, 0, 1] }}
              transition={{ duration: 2, repeat: Infinity }}
              className="absolute inset-0 rounded-full bg-emerald-500/30"
            />
          </div>
          <div>
            <div className="text-xl font-black text-emerald-600">5G</div>
            <div className="text-[10px] font-semibold text-slate-400">Connected</div>
          </div>
        </div>

        <div className="space-y-2 pt-2 border-t border-slate-100">
          <div className="flex justify-between items-center">
            <span className="text-[9px] font-semibold text-slate-400">LATENCY</span>
            <span className="text-xs font-bold text-emerald-600 font-mono">35ms</span>
          </div>
          <div className="flex justify-between items-center">
            <span className="text-[9px] font-semibold text-slate-400">SIGNAL</span>
            <div className="flex gap-0.5">
              {[1, 2, 3, 4, 5].map((bar) => (
                <div
                  key={bar}
                  className="w-1 bg-emerald-500 rounded-full"
                  style={{ height: `${bar * 3}px` }}
                />
              ))}
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}

// --- 📊 통계 위젯 ---
function StatWidget({ label, value, icon, color }: { label: string; value: string; icon: React.ReactNode; color: string }) {
  return (
    <div className="relative bg-gradient-to-br from-white to-slate-50 p-4 rounded-xl border border-slate-200 shadow-md hover:shadow-lg transition-all duration-300 overflow-hidden group">
      <div className={cn("absolute top-0 right-0 w-16 h-16 bg-gradient-to-br opacity-10 rounded-full blur-2xl", color)} />
      <div className="relative">
        <div className="flex items-center justify-between mb-2">
          <div className="text-[9px] text-slate-400 font-bold uppercase tracking-wider">{label}</div>
          <div className={cn("text-slate-400", color)}>{icon}</div>
        </div>
        <div className={cn("text-2xl font-black bg-gradient-to-r bg-clip-text text-transparent", color)}>
          {value}
        </div>
      </div>
    </div>
  )
}

// --- 📜 Sub-Component: 미션 기록 아이템 ---
function MissionItem({ mission, index }: { mission: MissionHistory; index: number }) {
  const displayDate = formatDate(mission.times.complete || mission.times.assigned);

  const statusConfig = {
    COMPLETED: { 
      color: 'bg-emerald-500', 
      textColor: 'text-emerald-700', 
      bgColor: 'bg-emerald-50', 
      borderColor: 'border-emerald-200',
      icon: '✓'
    },
    IN_PROGRESS: { 
      color: 'bg-amber-500', 
      textColor: 'text-amber-700', 
      bgColor: 'bg-amber-50', 
      borderColor: 'border-amber-200',
      icon: '⟳'
    },
    CANCELLED: { 
      color: 'bg-red-500', 
      textColor: 'text-red-700', 
      bgColor: 'bg-red-50', 
      borderColor: 'border-red-200',
      icon: '✕'
    }
  };

  const config = statusConfig[mission.status];

  return (
    <motion.div 
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ delay: index * 0.05 }}
      className="bg-gradient-to-br from-white to-slate-50 border border-slate-200 rounded-xl p-4 shadow-md hover:shadow-xl transition-all duration-300 hover:border-cyan-300"
    >
      {/* 헤더 */}
      <div className="flex justify-between items-start mb-3">
        <div className="flex items-center gap-2">
          <div className={cn(
            "w-8 h-8 rounded-lg bg-gradient-to-br flex items-center justify-center text-white font-bold text-sm shadow-lg",
            mission.status === 'COMPLETED' ? 'from-emerald-500 to-green-600' :
            mission.status === 'IN_PROGRESS' ? 'from-amber-500 to-orange-600' :
            'from-red-500 to-rose-600'
          )}>
            {config.icon}
          </div>
          <div>
            <div className="text-xs font-bold text-slate-800 font-mono">{mission.missionId}</div>
            <span className={cn(
              "inline-block text-[9px] px-2 py-0.5 rounded-full font-bold mt-0.5",
              config.bgColor,
              config.textColor,
              config.borderColor,
              "border"
            )}>
              {mission.status.replace('_', ' ')}
            </span>
          </div>
        </div>
        <div className="text-right">
          <div className="text-[10px] text-slate-400 font-mono">{displayDate}</div>
          <div className="text-[9px] text-slate-400 font-semibold">{formatTime(mission.times.assigned)}</div>
        </div>
      </div>

      {/* 정보 그리드 */}
      <div className="grid grid-cols-2 gap-2">
        <InfoItem icon={<User size={12} />} label="User" value={mission.userId} />
        <InfoItem icon={<MapPin size={12} />} label="Destination" value={mission.locationId} />
        <InfoItem icon={<Package size={12} />} label="Payload" value={`${mission.weight}kg`} />
        <InfoItem 
          icon={<Clock size={12} />} 
          label="Duration" 
          value={mission.times.complete ? `${Math.round((new Date(mission.times.complete).getTime() - new Date(mission.times.assigned).getTime()) / 60000)}min` : 'In Progress'} 
        />
      </div>
    </motion.div>
  )
}

// --- 정보 아이템 컴포넌트 ---
function InfoItem({ icon, label, value }: { icon: React.ReactNode; label: string; value: string }) {
  return (
    <div className="flex items-start gap-1.5 bg-white/60 rounded-lg p-2 border border-slate-100">
      <div className="text-cyan-600 mt-0.5">{icon}</div>
      <div className="flex-1 min-w-0">
        <div className="text-[8px] text-slate-400 font-semibold uppercase tracking-wide">{label}</div>
        <div className="text-[11px] font-bold text-slate-700 truncate">{value}</div>
      </div>
    </div>
  )
}

// --- 🚀 Main Component ---
export default function RobotDetailModal({ robot, onClose, modelUrl = "./models/carryporter.glb" }: RobotDetailProps) {
  const [activeTab, setActiveTab] = useState<'info' | 'history'>('info');

  // 실제 데이터처럼 보이는 가라 데이터 생성
  const robotIdentifier = robot.id || robot.robotCode || 'RB-001';
  const history = generateRealisticMissionData(robotIdentifier);
  const stats = generateRobotStats(robotIdentifier);

  // 로봇 배터리 레벨 (없으면 랜덤 생성)
  const batteryLevel = robot.battery || (70 + Math.floor(Math.random() * 25));

  // 로봇 MAC 주소 생성
  const macAddress = `00:1B:${Math.floor(Math.random() * 100).toString(16).toUpperCase().padStart(2, '0')}:${Math.floor(Math.random() * 100).toString(16).toUpperCase().padStart(2, '0')}:${Math.floor(Math.random() * 100).toString(16).toUpperCase().padStart(2, '0')}:${Math.floor(Math.random() * 100).toString(16).toUpperCase().padStart(2, '0')}`;

  return (
    <div className="fixed inset-0 z-[9999] flex items-center justify-center px-4">
      {/* 배경 오버레이 */}
      <motion.div 
        initial={{ opacity: 0 }} 
        animate={{ opacity: 1 }} 
        exit={{ opacity: 0 }}
        className="absolute inset-0 bg-slate-900/80 backdrop-blur-md" 
        onClick={onClose} 
      />
      
      {/* 메인 모달 */}
      <motion.div 
        initial={{ opacity: 0, scale: 0.95, y: 20 }} 
        animate={{ opacity: 1, scale: 1, y: 0 }} 
        exit={{ opacity: 0, scale: 0.95, y: 20 }}
        transition={{ type: "spring", duration: 0.5, bounce: 0.3 }}
        className="relative bg-white w-full max-w-6xl max-h-[90vh] rounded-3xl shadow-2xl overflow-hidden flex flex-col"
      >
        {/* 🎨 헤더 */}
        <div className="relative p-6 border-b border-slate-100 bg-gradient-to-r from-slate-50 via-white to-slate-50">
          {/* 배경 장식 */}
          <div className="absolute top-0 left-0 w-32 h-32 bg-gradient-to-br from-cyan-500/10 to-blue-500/10 rounded-full blur-3xl" />
          <div className="absolute bottom-0 right-0 w-32 h-32 bg-gradient-to-br from-purple-500/10 to-pink-500/10 rounded-full blur-3xl" />
          
          <div className="relative flex justify-between items-center">
            <div className="flex items-center gap-4">
              {/* 로봇 아이콘 */}
              <div className="relative">
                <div className="absolute inset-0 bg-gradient-to-br from-cyan-500 to-blue-600 rounded-2xl blur-md opacity-30" />
                <div className="relative w-14 h-14 bg-gradient-to-br from-cyan-500 to-blue-600 rounded-2xl flex items-center justify-center text-white shadow-xl">
                  <Cpu size={28} />
                </div>
                {/* 상태 인디케이터 */}
                <div className={cn(
                  "absolute -top-1 -right-1 w-4 h-4 rounded-full border-2 border-white shadow-lg",
                  robot.status === 'WORKING' || robot.status === 'working' ? 'bg-emerald-500' : 'bg-slate-400'
                )}>
                  <motion.div
                    animate={{ scale: [1, 1.3, 1], opacity: [1, 0, 1] }}
                    transition={{ duration: 2, repeat: Infinity }}
                    className={cn(
                      "absolute inset-0 rounded-full",
                      robot.status === 'WORKING' || robot.status === 'working' ? 'bg-emerald-500' : 'bg-slate-400'
                    )}
                  />
                </div>
              </div>
              
              {/* 텍스트 정보 */}
              <div>
                <h2 className="text-2xl font-black bg-gradient-to-r from-slate-800 to-slate-600 bg-clip-text text-transparent">
                  {robot.name || robot.robotCode || robot.id}
                </h2>
                <div className="flex items-center gap-2 mt-1">
                  <span className="text-xs font-mono text-slate-400">ID: {robot.id || robot.robotCode}</span>
                  <span className="w-1 h-1 bg-slate-300 rounded-full" />
                  <span className={cn(
                    "text-xs font-bold uppercase tracking-wide px-2 py-0.5 rounded-md",
                    (robot.status === 'WORKING' || robot.status === 'working')
                      ? 'bg-emerald-100 text-emerald-700' 
                      : 'bg-slate-100 text-slate-600'
                  )}>
                    {robot.status || 'STANDBY'}
                  </span>
                </div>
              </div>
            </div>
            
            {/* 닫기 버튼 */}
            <button 
              onClick={onClose} 
              className="p-2.5 hover:bg-slate-100 rounded-xl transition-all text-slate-400 hover:text-slate-700 hover:scale-110 active:scale-95"
            >
              <X size={22} />
            </button>
          </div>
        </div>

        {/* 🎯 탭 메뉴 */}
        <div className="flex border-b border-slate-100 px-6 bg-white">
          {[
            { id: 'info', label: 'Digital Twin', icon: <Cpu size={16} /> },
            { id: 'history', label: 'Mission Logs', icon: <Activity size={16} /> }
          ].map((tab) => (
            <button
              key={tab.id}
              onClick={() => setActiveTab(tab.id as any)}
              className={cn(
                "relative px-6 py-4 text-sm font-bold transition-all duration-300 flex items-center gap-2",
                activeTab === tab.id 
                  ? "text-cyan-600" 
                  : "text-slate-400 hover:text-slate-600"
              )}
            >
              {tab.icon}
              {tab.label}
              {activeTab === tab.id && (
                <motion.div
                  layoutId="activeTab"
                  className="absolute bottom-0 left-0 right-0 h-0.5 bg-gradient-to-r from-cyan-500 to-blue-500"
                  transition={{ type: "spring", bounce: 0.2, duration: 0.6 }}
                />
              )}
            </button>
          ))}
        </div>

        {/* 📋 콘텐츠 영역 (스크롤 가능) */}
        <div className="flex-1 overflow-y-auto bg-gradient-to-br from-slate-50/50 to-white p-6">
          <AnimatePresence mode="wait">
            {activeTab === 'info' ? (
              <motion.div 
                key="info"
                initial={{ opacity: 0, x: -20 }} 
                animate={{ opacity: 1, x: 0 }} 
                exit={{ opacity: 0, x: 20 }}
                transition={{ duration: 0.3 }}
                className="grid grid-cols-2 gap-6"
              >
                {/* 왼쪽: 3D 모델 + 상세 정보 */}
                <div className="space-y-4">
                  <Robot3DViewer modelUrl={modelUrl} />
                  
                  {/* 상세 정보 */}
                  <div className="bg-white rounded-2xl border border-slate-200 shadow-lg overflow-hidden">
                    <div className="px-4 py-3 bg-gradient-to-r from-slate-50 to-white border-b border-slate-100">
                      <h3 className="text-xs font-bold text-slate-800 uppercase tracking-wide flex items-center gap-2">
                        <AlertCircle size={14} className="text-cyan-600" />
                        System Information
                      </h3>
                    </div>
                    <div className="p-4 grid grid-cols-2 gap-3">
                      <DetailRow label="Robot Code" value={robot.robotCode || robot.id || 'RB-2024-X99'} icon={<Cpu size={14}/>} />
                      <DetailRow label="MAC Address" value={macAddress} icon={<Wifi size={14}/>} />
                      <DetailRow label="First Deployed" value={stats.lastMaintenance} icon={<Calendar size={14}/>} />
                      <DetailRow label="Last Sync" value={`${new Date().getHours()}:${String(new Date().getMinutes()).padStart(2, '0')}`} icon={<Clock size={14}/>} />
                      <DetailRow label="Total Distance" value={`${stats.totalDistance}km`} icon={<TrendingUp size={14}/>} />
                      <DetailRow label="Active Hours" value={`${stats.activeHours}h`} icon={<Activity size={14}/>} />
                    </div>
                  </div>
                </div>

                {/* 오른쪽: 상태 위젯들 */}
                <div className="space-y-4">
                  <BatteryWidget level={batteryLevel} />
                  <NetworkWidget />
                  
                  {/* 추가 통계 위젯 */}
                  <div className="grid grid-cols-2 gap-4">
                    <StatWidget 
                      label="Total Missions" 
                      value={stats.totalMissions.toString()} 
                      icon={<Package size={16} />}
                      color="from-cyan-500 to-blue-500" 
                    />
                    <StatWidget 
                      label="Success Rate" 
                      value={`${stats.successRate}%`} 
                      icon={<TrendingUp size={16} />}
                      color="from-emerald-500 to-green-500" 
                    />
                    <StatWidget 
                      label="Uptime" 
                      value={`${stats.uptime}%`} 
                      icon={<Activity size={16} />}
                      color="from-purple-500 to-pink-500" 
                    />
                    <StatWidget 
                      label="Avg Speed" 
                      value={`${stats.avgSpeed}m/s`} 
                      icon={<Zap size={16} />}
                      color="from-amber-500 to-orange-500" 
                    />
                  </div>
                  
                  {/* 정비 알림 */}
                  <div className="bg-gradient-to-r from-blue-50 to-cyan-50 border border-blue-200 rounded-xl p-4">
                    <div className="flex items-start gap-3">
                      <div className="w-10 h-10 rounded-lg bg-blue-500 flex items-center justify-center text-white">
                        <Calendar size={20} />
                      </div>
                      <div className="flex-1">
                        <h4 className="text-sm font-bold text-slate-800 mb-1">Maintenance Schedule</h4>
                        <div className="text-xs text-slate-600 space-y-1">
                          <div className="flex justify-between">
                            <span className="text-slate-500">Last Service:</span>
                            <span className="font-mono font-semibold">{stats.lastMaintenance}</span>
                          </div>
                          <div className="flex justify-between">
                            <span className="text-slate-500">Next Service:</span>
                            <span className="font-mono font-semibold text-blue-600">{stats.nextMaintenance}</span>
                          </div>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </motion.div>
            ) : (
              <motion.div 
                key="history"
                initial={{ opacity: 0, x: 20 }} 
                animate={{ opacity: 1, x: 0 }} 
                exit={{ opacity: 0, x: -20 }}
                transition={{ duration: 0.3 }}
              >
                {/* 미션 통계 헤더 */}
                <div className="grid grid-cols-4 gap-4 mb-6">
                  <div className="bg-gradient-to-br from-emerald-50 to-green-50 border border-emerald-200 rounded-xl p-4">
                    <div className="text-xs text-emerald-600 font-semibold mb-1">COMPLETED</div>
                    <div className="text-2xl font-black text-emerald-700">{history.filter(m => m.status === 'COMPLETED').length}</div>
                  </div>
                  <div className="bg-gradient-to-br from-amber-50 to-orange-50 border border-amber-200 rounded-xl p-4">
                    <div className="text-xs text-amber-600 font-semibold mb-1">IN PROGRESS</div>
                    <div className="text-2xl font-black text-amber-700">{history.filter(m => m.status === 'IN_PROGRESS').length}</div>
                  </div>
                  <div className="bg-gradient-to-br from-red-50 to-rose-50 border border-red-200 rounded-xl p-4">
                    <div className="text-xs text-red-600 font-semibold mb-1">CANCELLED</div>
                    <div className="text-2xl font-black text-red-700">{history.filter(m => m.status === 'CANCELLED').length}</div>
                  </div>
                  <div className="bg-gradient-to-br from-cyan-50 to-blue-50 border border-cyan-200 rounded-xl p-4">
                    <div className="text-xs text-cyan-600 font-semibold mb-1">SUCCESS RATE</div>
                    <div className="text-2xl font-black text-cyan-700">{stats.successRate}%</div>
                  </div>
                </div>

                {/* 미션 목록 */}
                <div className="grid grid-cols-2 gap-4">
                  {history.length > 0 ? (
                    history.map((mission, idx) => (
                      <MissionItem key={mission.missionId} mission={mission} index={idx} />
                    ))
                  ) : (
                    <div className="col-span-2 flex flex-col items-center justify-center py-16 text-slate-400">
                      <Activity size={48} className="mb-4 opacity-30" />
                      <p className="text-sm font-semibold">No mission history available</p>
                    </div>
                  )}
                </div>
              </motion.div>
            )}
          </AnimatePresence>
        </div>
      </motion.div>
    </div>
  )
}

// 헬퍼 컴포넌트
function DetailRow({ label, value, icon }: { label: string; value: string; icon: React.ReactNode }) {
  return (
    <div className="flex flex-col gap-1">
      <div className="flex items-center gap-1.5 text-[10px] text-slate-400 font-bold uppercase tracking-wider">
        <span className="text-cyan-600">{icon}</span>
        {label}
      </div>
      <div className="text-sm font-bold text-slate-700 font-mono bg-slate-50 px-3 py-1.5 rounded-lg border border-slate-100">
        {value}
      </div>
    </div>
  )
}