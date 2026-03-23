import { useState, useRef, Suspense, useMemo, useEffect } from 'react'
import { Canvas, useFrame, useThree } from '@react-three/fiber'
import { OrbitControls, useGLTF, Clone, Html, Text, Environment, Line } from '@react-three/drei'
import * as THREE from 'three'
import { motion, AnimatePresence } from 'framer-motion'
import { LayoutGrid, Box, Maximize2, AlertTriangle, Compass, X } from 'lucide-react'
import RobotDetailModal from './RobotDetailModal'
import { getNavigationPath, findNearestDestination, PathPoint, DESTINATIONS } from '@/utils/navigationPaths';

// ==============================================================================
// 🛠️ 설정 (맵 레이아웃 재구성)
// ==============================================================================
const ROBOT_GLB_URL = "./models/carryporter.glb"
const CHARACTER_IMAGE_URL = "/cat_icon.png"

const MAP_WIDTH = 24
const MAP_HEIGHT = 16

const MAP_ZONES = [
  { id: 'main', type: 'station', x: 0, y: -7, w: 4, h: 3, color: '#10b981', label: 'Main station' },
  { id: 'stop1', type: 'stop', x: -6.1, y: 3.5, w: 4, h: 3, color: '#ef4444', label: 'STOP1' },
  { id: 'gate', type: 'gate', x: 6.1, y: 3.5, w: 4, h: 3, color: '#3b82f6', label: 'GATE' },
]

// Zone ID를 좌표로 변환하는 헬퍼 함수
const getZonePosition = (zoneId: string): { x: number, y: number } => {
  const zone = MAP_ZONES.find(z => z.id === zoneId);
  if (zone) {
    return { x: zone.x * 10, y: zone.y * 10 }; // 10배 스케일
  }
  return { x: 0, y: -70 }; // 기본값: Main station (상단)
}

// ------------------------------------------------------------------
// 🧊 [Component] GLB Robot 3D (부드러운 이동 + 경로 그리기)
// ------------------------------------------------------------------
function GlbRobot3D({ 
  position, 
  targetPosition,
  status, 
  robotCode, 
  onClick,
  isGroup = false,
  groupCount = 1,
  showPath = false,
  activePath,
  onMoveEnd
}: { 
  position: [number, number, number], 
  targetPosition?: [number, number, number],
  status: string, 
  robotCode: string,
  onClick: () => void,
  isGroup?: boolean,
  groupCount?: number,
  showPath?: boolean,
  activePath?: any[],
  onMoveEnd?: () => void
}) {
  const { scene } = useGLTF(ROBOT_GLB_URL)
  const mainGroupRef = useRef<THREE.Group>(null)
  const robotGroupRef = useRef<THREE.Group>(null)
  const [hovered, setHovered] = useState(false)
  const [pathHistory, setPathHistory] = useState<[number, number, number][]>([])
  const [currentPointIndex, setCurrentPointIndex] = useState(0)
  
  // activePath가 변경되면 인덱스 초기화 + 2초 출발 딜레이
  const [moveReady, setMoveReady] = useState(true);
  useEffect(() => {
    setCurrentPointIndex(0);
    if (activePath && activePath.length > 0) {
      setMoveReady(false);
      const timer = setTimeout(() => setMoveReady(true), 3500);
      return () => clearTimeout(timer);
    }
  }, [activePath]);
  // 이동 경로 기록
  useEffect(() => {
    if (mainGroupRef.current && showPath) {
      const currentPos: [number, number, number] = [
        mainGroupRef.current.position.x,
        0.1,
        mainGroupRef.current.position.z
      ]
      
      setPathHistory(prev => {
        const newPath = [...prev, currentPos]
        // 최대 50개 포인트만 유지
        return newPath.slice(-50)
      })
    } else if (!showPath) {
      // 이동이 끝나면 경로 초기화
      setPathHistory([])
    }
  }, [showPath, mainGroupRef.current?.position.x, mainGroupRef.current?.position.z])

  // 타겟 위치로 부드럽게 이동
  useFrame((state, delta) => {
    const t = state.clock.getElapsedTime()
    
    // 1. 부유 애니메이션 (로봇만)
    if (robotGroupRef.current) {
      robotGroupRef.current.position.y = Math.sin(t * 2) * 0.1 + (hovered ? 0.3 : 0)
    }

    // 2. 웨이포인트 기반 경로 이동 (activePath 사용, 2초 딜레이 후)
    if (mainGroupRef.current && activePath && activePath.length > 0 && moveReady) {
      if (currentPointIndex < activePath.length) {
        // 목표 웨이포인트 (1/10 스케일)
        const targetPoint = activePath[currentPointIndex];
        const targetX = targetPoint.x / 10;
        const targetZ = targetPoint.y / 10;

        const currentPos = mainGroupRef.current.position;
        const dx = targetX - currentPos.x;
        const dz = targetZ - currentPos.z;
        const distance = Math.sqrt(dx * dx + dz * dz);
        const MOVE_SPEED = 1.25;
        
        if (distance > 0.05) {
          const moveX = (dx / distance) * MOVE_SPEED * delta;
          const moveZ = (dz / distance) * MOVE_SPEED * delta;
          
          if (Math.abs(moveX) > Math.abs(dx)) currentPos.x = targetX;
          else currentPos.x += moveX;
          if (Math.abs(moveZ) > Math.abs(dz)) currentPos.z = targetZ;
          else currentPos.z += moveZ;

          const targetRotation = Math.atan2(dx, dz);
          let rotDiff = targetRotation - mainGroupRef.current.rotation.y;
          while (rotDiff > Math.PI) rotDiff -= Math.PI * 2;
          while (rotDiff < -Math.PI) rotDiff += Math.PI * 2;
          mainGroupRef.current.rotation.y += rotDiff * 8 * delta;
        } else {
          setCurrentPointIndex(prev => prev + 1);
        }
      } else {
        if (onMoveEnd) onMoveEnd();
      }
    }
    // 3. 기존 targetPosition 방식 (하위 호환성)
    else if (mainGroupRef.current && targetPosition) {
      const lerpFactor = 0.02;
      const currentX = mainGroupRef.current.position.x
      const currentZ = mainGroupRef.current.position.z
      
      mainGroupRef.current.position.x += (targetPosition[0] - currentX) * lerpFactor
      mainGroupRef.current.position.z += (targetPosition[2] - currentZ) * lerpFactor
      
      const dx = targetPosition[0] - currentX
      const dz = targetPosition[2] - currentZ
      if (Math.abs(dx) > 0.01 || Math.abs(dz) > 0.01) {
        const targetRotation = Math.atan2(dx, dz)
        mainGroupRef.current.rotation.y += (targetRotation - mainGroupRef.current.rotation.y) * 0.1
      }
    }
    
    if (state.gl.domElement) {
      state.gl.domElement.style.cursor = hovered ? 'pointer' : 'auto'
    }
  })

  const isAvailable = status === 'available'
  const statusColor = isAvailable ? '#10b981' : '#3b82f6'

  // 남은 경로 계산
  const remainingPathPoints = useMemo(() => {
    if (!activePath || currentPointIndex >= activePath.length || !mainGroupRef.current) return null;
    const points: [number, number, number][] = [];
    points.push([mainGroupRef.current.position.x, 0.1, mainGroupRef.current.position.z]);
    for (let i = currentPointIndex; i < activePath.length; i++) {
      points.push([activePath[i].x / 10, 0.1, activePath[i].y / 10]);
    }
    return points;
  }, [activePath, currentPointIndex, mainGroupRef.current?.position.x, mainGroupRef.current?.position.z]);

  return (
    <>
      {/* activePath 경로 시각화 */}
      {remainingPathPoints && remainingPathPoints.length > 1 && (
        <group>
          <Line points={remainingPathPoints} color={statusColor} lineWidth={4} opacity={0.6} transparent />
          <Line points={remainingPathPoints} color="#ffffff" lineWidth={2} opacity={0.3} transparent dashed dashScale={1} position={[0, 0.01, 0]} />
        </group>
      )}

      {/* 이동한 경로 표시 (Trail) */}
      {showPath && pathHistory.length > 1 && (
        <Line
          points={pathHistory}
          color={statusColor}
          lineWidth={2}
          transparent
          opacity={0.4}
          dashed={false}
        />
      )}

      {/* 목표 지점까지의 직선 경로 (targetPosition 사용 시) */}
      {showPath && targetPosition && mainGroupRef.current && (
        <group>
          <Line
            points={[
              [mainGroupRef.current.position.x, 0.1, mainGroupRef.current.position.z],
              [targetPosition[0], 0.1, targetPosition[2]]
            ]}
            color={statusColor}
            lineWidth={3}
            transparent
            opacity={0.7}
            dashed
            dashScale={2}
            dashSize={0.3}
            gapSize={0.2}
          />
          
          {/* 목표 지점 마커 */}
          <mesh position={[targetPosition[0], 0.05, targetPosition[2]]} rotation-x={-Math.PI / 2}>
            <ringGeometry args={[0.3, 0.5, 32]} />
            <meshBasicMaterial color={statusColor} transparent opacity={0.6} side={THREE.DoubleSide} />
          </mesh>
          <mesh position={[targetPosition[0], 0.06, targetPosition[2]]} rotation-x={-Math.PI / 2}>
            <circleGeometry args={[0.3, 32]} />
            <meshBasicMaterial color={statusColor} transparent opacity={0.4} />
          </mesh>
        </group>
      )}

      {/* 메인 그룹: 로봇 + 그림자 + 이름표가 함께 이동 */}
      <group ref={mainGroupRef} position={[position[0], 0, position[2]]}>
        
        {/* ✨ 로봇 전용 강력한 조명 (로봇을 매우 밝게) */}
        <spotLight 
          position={[0, 5, 0]} 
          intensity={10}
          distance={10} 
          angle={0.7} 
          penumbra={0.4} 
          color="#ffffff" 
          castShadow
          shadow-mapSize={[1024, 1024]}
        />
        <pointLight position={[0, 2, 0]} intensity={5} distance={5} color="#ffffff" />
        <pointLight position={[1, 1, 1]} intensity={4} distance={4} color="#aaddff" />
        <pointLight position={[-1, 1, -1]} intensity={4} distance={4} color="#ffddaa" />

        {/* 로봇 그룹 (위아래 떠다님) */}
        <group 
          ref={robotGroupRef}
          onPointerOver={(e) => { e.stopPropagation(); setHovered(true); }}
          onPointerOut={(e) => { e.stopPropagation(); setHovered(false); }}
          onClick={(e) => { e.stopPropagation(); onClick(); }}
        >
          {/* 메인 로봇 */}
          <Clone object={scene} scale={hovered ? 1.65 : 1.5} position={[0, 1.8, 0]} rotation={[0, 0, 0]} castShadow receiveShadow />
          
          {/* 그룹일 경우 추가 로봇들 표시 */}
          {isGroup && (
            <>
              <group position={[-0.3, 1.6, -0.3]} rotation={[0, Math.PI / 6, 0]}>
                <Clone object={scene} scale={1.3} castShadow />
              </group>
              <group position={[0.3, 1.5, -0.4]} rotation={[0, -Math.PI / 6, 0]}>
                <Clone object={scene} scale={1.2} castShadow />
              </group>
            </>
          )}
        </group>

        {/* 바닥 그림자 (원형) */}
        <mesh rotation-x={-Math.PI / 2} position={[0, 0.02, 0]}>
          <circleGeometry args={[hovered ? 1.0 : 0.8, 32]} />
          <meshBasicMaterial color="#000000" opacity={0.3} transparent />
        </mesh>

        {/* 바닥 링 - 더 밝고 눈에 띄게 */}
        <mesh rotation-x={-Math.PI / 2} position={[0, 0.05, 0]}>
          <ringGeometry args={[0.5, hovered ? 1.2 : (isGroup ? 1.0 : 0.8), 32]} />
          <meshBasicMaterial color={statusColor} opacity={hovered ? 0.9 : 0.7} transparent />
        </mesh>
        
        {/* 로봇 아래 강력한 글로우 효과 */}
        <pointLight position={[0, 1, 0]} color={statusColor} intensity={hovered ? 8 : 6} distance={4} decay={2} />

        {/* 3D 이름표 (항상 표시) */}
        <Html position={[0, 4.2, 0]} center distanceFactor={12} zIndexRange={[0, 0]}>
          <div className="flex flex-col items-center transform transition-all hover:scale-110">
            <div className={`
              flex items-center gap-1.5 px-3 py-1.5 rounded-lg border-2 backdrop-blur-md shadow-2xl transition-all
              ${isAvailable 
                ? 'bg-emerald-500/95 border-emerald-300 text-white' 
                : 'bg-blue-500/95 border-blue-300 text-white'}
              ${hovered ? 'scale-110 shadow-xl' : ''}
            `}>
              <span className="text-xs font-black tracking-tight drop-shadow-lg">
                {isGroup ? `${groupCount}대` : robotCode}
              </span>
            </div>
            <div className={`w-0.5 h-5 ${isAvailable ? 'bg-emerald-400/60' : 'bg-blue-400/60'}`} />
          </div>
        </Html>
      </group>
    </>
  )
}

useGLTF.preload(ROBOT_GLB_URL)

// ------------------------------------------------------------------
// 🧊 [Component] Zone 3D (선명도 개선)
// ------------------------------------------------------------------
function Zone3D({ data }: { data: typeof MAP_ZONES[0] }) {
  const isObstacle = data.type === 'obstacle';
  const isStation = data.type === 'station';
  const wallHeight = 0.5;

  return (
    <group position={[data.x, 0, data.y]}>
      {isObstacle ? (
        <group>
          <mesh position={[0, wallHeight / 2, 0]}>
            <boxGeometry args={[data.w, wallHeight, data.h]} />
            <meshStandardMaterial 
              color={data.color} 
              transparent 
              opacity={0.4} 
              wireframe 
              emissive={data.color}
              emissiveIntensity={0.3}
            />
          </mesh>
          <mesh position={[0, wallHeight / 2, 0]}>
             <boxGeometry args={[data.w * 0.95, wallHeight * 0.9, data.h * 0.95]} />
             <meshStandardMaterial 
               color="#500000" 
               transparent 
               opacity={0.6}
               emissive="#ff0000"
               emissiveIntensity={0.2}
             />
          </mesh>
          <mesh rotation-x={-Math.PI / 2} position={[0, 0.02, 0]}>
            <planeGeometry args={[Math.min(data.w, data.h), Math.min(data.w, data.h)]} />
            <meshBasicMaterial color={data.color} transparent opacity={0.3} />
          </mesh>
          <Text position={[0, wallHeight + 0.5, 0]} fontSize={1.5} color={data.color} rotation={[-Math.PI/2, 0, 0]}>X</Text>
        </group>
      ) : (
        <group>
          {/* 바닥 평면 */}
          <mesh rotation-x={-Math.PI / 2} receiveShadow position={[0, 0.01, 0]}>
            <planeGeometry args={[data.w, data.h]} />
            <meshStandardMaterial 
              color={data.color} 
              transparent 
              opacity={0.2} 
              side={THREE.DoubleSide}
              emissive={data.color}
              emissiveIntensity={0.1}
            />
          </mesh>
          
          {/* 테두리 벽 */}
          <mesh position={[0, wallHeight / 2, 0]}>
            <boxGeometry args={[data.w, wallHeight, data.h]} />
            <meshStandardMaterial 
              color={data.color} 
              opacity={0.4} 
              transparent 
              emissive={data.color} 
              emissiveIntensity={0.5}
              wireframe={!isStation}
            />
          </mesh>
          
          {/* 모서리 기둥 (선명하게) */}
          {[[-1, -1], [-1, 1], [1, -1], [1, 1]].map((dir, i) => (
            <mesh key={i} position={[dir[0] * data.w / 2, wallHeight / 2, dir[1] * data.h / 2]}>
              <cylinderGeometry args={[0.06, 0.06, wallHeight, 8]} />
              <meshStandardMaterial 
                color={data.color} 
                emissive={data.color} 
                emissiveIntensity={2.5}
                metalness={0.8}
                roughness={0.2}
              />
            </mesh>
          ))}
          
          {isStation && (
             <Text position={[0, 0.1, 0]} rotation={[-Math.PI/2, 0, 0]} fontSize={2} color={data.color} fillOpacity={0.4}>⚡</Text>
          )}
        </group>
      )}
      <Html position={[0, isObstacle ? wallHeight + 0.5 : wallHeight + 0.3, 0]} center transform sprite zIndexRange={[0, 0]}>
        <div className={`
          text-xs font-black tracking-widest whitespace-nowrap pointer-events-none select-none px-2 py-0.5 rounded
          ${isObstacle ? 'bg-red-500/20 text-red-500 border border-red-500/50' : `text-${data.color} opacity-80`}
        `} style={{ color: data.color, textShadow: `0 0 10px ${data.color}` }}>
          {data.label}
        </div>
      </Html>
    </group>
  )
}

// ------------------------------------------------------------------
// 🗺️ [Component] 2D Map View
// ------------------------------------------------------------------
function MapView2D({ robots, imageUrl, onRobotClick }: { robots: any[], imageUrl: string, onRobotClick: (robot: any) => void }) {
  const toPercentX = (x: number) => ((x + MAP_WIDTH / 2) / MAP_WIDTH) * 100
  const toPercentY = (y: number) => ((y + MAP_HEIGHT / 2) / MAP_HEIGHT) * 100

  return (
    <div className="w-full h-full relative bg-slate-950 overflow-hidden select-none font-sans group">
      <div className="absolute inset-0 opacity-20" style={{
        backgroundImage: 'linear-gradient(#334155 1px, transparent 1px), linear-gradient(90deg, #334155 1px, transparent 1px)',
        backgroundSize: '40px 40px'
      }} />

      {MAP_ZONES.map((zone) => {
         const isObstacle = zone.type === 'obstacle';
         return (
          <div key={zone.id} 
            className={`absolute border flex flex-col items-center justify-center text-xs font-bold rounded-md transition-opacity duration-300
              ${isObstacle ? 'opacity-80' : 'opacity-60 hover:opacity-100'}
            `}
            style={{
              left: `${toPercentX(zone.x - zone.w / 2)}%`,
              top: `${toPercentY(zone.y - zone.h / 2)}%`,
              width: `${(zone.w / MAP_WIDTH) * 100}%`,
              height: `${(zone.h / MAP_HEIGHT) * 100}%`,
              borderColor: zone.color,
              backgroundColor: isObstacle 
                ? `repeating-linear-gradient(45deg, ${zone.color}20, ${zone.color}20 10px, ${zone.color}40 10px, ${zone.color}40 20px)`
                : `${zone.color}10`,
              color: zone.color,
              boxShadow: `0 0 20px ${zone.color}15`
            }}>
            {isObstacle && <AlertTriangle className="w-6 h-6 mb-1 animate-pulse" />}
            {zone.label}
          </div>
        )
      })}

      {robots.map((robot) => {
        const xPos = robot.x ?? -80;
        const yPos = robot.y ?? 0;
        const isAvailable = robot.status === 'available';
        return (
          <button
            key={robot.robotCode || robot.id}
            onClick={() => onRobotClick(robot)}
            className="absolute flex flex-col items-center justify-center transition-all duration-700 ease-out z-20 cursor-pointer hover:scale-110"
            style={{
              left: `${toPercentX(xPos / 10)}%`,
              top: `${toPercentY(yPos / 10)}%`,
              transform: 'translate(-50%, -50%)'
            }}
          >
            <div className={`absolute w-16 h-16 rounded-full opacity-30 animate-ping ${isAvailable ? 'bg-emerald-500' : 'bg-blue-500'}`} />
            <div className="relative transform hover:scale-110 transition-transform">
              <div className={`w-8 h-8 rounded-full border-2 bg-slate-900 flex items-center justify-center overflow-hidden ${isAvailable ? 'border-emerald-400' : 'border-blue-400'}`}>
                 <img src={imageUrl} alt="Bot" className="w-full h-full object-cover" onError={(e) => { (e.target as HTMLImageElement).style.display='none'; }}/>
                 <span className="absolute text-lg">🤖</span>
              </div>
            </div>
            <span className="text-[9px] font-bold text-white bg-slate-900/90 px-1.5 py-0.5 rounded shadow-sm mt-1 border border-slate-700 whitespace-nowrap">
              {robot.robotCode || robot.id}
            </span>
          </button>
        );
      })}
    </div>
  )
}

// ------------------------------------------------------------------
// 🖼️ [Component] Full Screen Modal
// ------------------------------------------------------------------
function FullScreenModal({ 
  viewMode, 
  robots, 
  groupedRobots,
  robotPaths,
  imageUrl, 
  onClose, 
  onRobotClick,
  onMoveEnd
}: { 
  viewMode: '2d' | '3d', 
  robots: any[],
  groupedRobots: any[],
  robotPaths: Map<string, PathPoint[]>,
  imageUrl: string, 
  onClose: () => void,
  onRobotClick: (robot: any) => void,
  onMoveEnd: (robotId: string) => void
}) {
  return (
    <AnimatePresence>
      <motion.div
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        exit={{ opacity: 0 }}
        className="fixed inset-0 z-[100] bg-black/95 backdrop-blur-sm"
        onClick={onClose}
      >
        <motion.div
          initial={{ scale: 0.95, opacity: 0 }}
          animate={{ scale: 1, opacity: 1 }}
          exit={{ scale: 0.95, opacity: 0 }}
          transition={{ type: "spring", damping: 25, stiffness: 300 }}
          className="absolute inset-4 bg-slate-950 rounded-2xl border border-slate-700 shadow-2xl overflow-hidden"
          onClick={(e) => e.stopPropagation()}
        >
          <button
            onClick={onClose}
            className="absolute top-4 right-4 z-50 p-3 bg-slate-800/90 backdrop-blur text-slate-300 rounded-lg hover:bg-red-500 hover:text-white transition-all shadow-xl border border-slate-700 hover:border-red-400"
          >
            <X className="w-6 h-6" />
          </button>

          <div className="absolute top-4 left-4 z-50 bg-slate-900/90 backdrop-blur-md px-4 py-2 rounded-lg border border-slate-700">
            <h3 className="text-lg font-black text-white tracking-tight">전체 맵 뷰</h3>
          </div>

          <div className="w-full h-full">
            {viewMode === '2d' ? (
              <MapView2D robots={robots} imageUrl={imageUrl} onRobotClick={onRobotClick} />
            ) : (
              <Canvas 
                shadows 
                dpr={[1.5, 2]} 
                camera={{ position: [0, 22, 18], fov: 40 }}
                gl={{ antialias: true, alpha: false }}
              >
                <color attach="background" args={['#0b1121']} />
                <fog attach="fog" args={['#0b1121', 20, 55]} />
                <Environment preset="city" />
                <ambientLight intensity={0.6} />
                <pointLight position={[-10, 10, -10]} intensity={1.5} color="#fbbf24" distance={20} />
                <pointLight position={[10, 10, 10]} intensity={1.5} color="#3b82f6" distance={20} />
                <directionalLight position={[5, 20, 5]} intensity={2.5} castShadow shadow-mapSize={[2048, 2048]} />
                
                <gridHelper args={[60, 60, 0x1e293b, 0x111827]} position={[0, -0.01, 0]} />
                
                <mesh rotation-x={-Math.PI / 2} receiveShadow position={[0, -0.02, 0]}>
                  <planeGeometry args={[100, 100]} />
                  <meshStandardMaterial color="#0f172a" roughness={0.4} metalness={0.6} />
                </mesh>

                {MAP_ZONES.map(zone => <Zone3D key={zone.id} data={zone} />)}

                <Suspense fallback={null}>
                  {groupedRobots.map((group, idx) => {
                    const robotId = group.representativeRobot.robotCode || group.representativeRobot.id;
                    const path = robotPaths.get(robotId);
                    
                    return (
                      <GlbRobot3D
                        key={`robot-group-modal-${idx}`}
                        robotCode={group.isGroup ? `${group.count}대` : (group.representativeRobot.robotCode || group.representativeRobot.id)}
                        position={[group.x / 10, 0, group.y / 10]}
                        activePath={path}
                        status={group.representativeRobot.status}
                        onClick={() => onRobotClick(group.representativeRobot)}
                        onMoveEnd={() => onMoveEnd(robotId)}
                        isGroup={group.isGroup}
                        groupCount={group.count}
                        showPath={!!path}
                      />
                    );
                  })}
                </Suspense>

                <OrbitControls 
                  maxPolarAngle={Math.PI / 2.1} 
                  minDistance={10} 
                  maxDistance={40} 
                  enablePan={true}
                  target={[0, 0, 0]}
                />
              </Canvas>
            )}
          </div>
        </motion.div>
      </motion.div>
    </AnimatePresence>
  );
}

// ------------------------------------------------------------------
// 🚀 [Main Page] Robot Stage
// ------------------------------------------------------------------
export default function RobotStage({
  robots = [],
  showDummyIfEmpty = false,
  moveCommands = [],
  sseMovements = []
}: {
  robots: any[];
  showDummyIfEmpty?: boolean;
  moveCommands?: Array<{ robotId: string; from: string; to: string }>;
  sseMovements?: Array<{ 
    robotCode: string; 
    eventName?: string;  // 'MissionStartedEvent' | 'ReturnStartedEvent'
    callLocationName?: string;  // 'a' | 'b' | 'c'
    x?: number; 
    y?: number;
  }>;
}) {
  const [viewMode, setViewMode] = useState<'2d' | '3d'>('3d')
  const [selectedRobot, setSelectedRobot] = useState<any | null>(null)
  const [isFullScreen, setIsFullScreen] = useState(false)
  const activeCount = useMemo(() => robots.length, [robots]);
  const canvasRef = useRef<any>(null);

  const [robotPositions, setRobotPositions] = useState<Map<string, { x: number; y: number }>>(new Map());
  
  // 🔧 경로 시스템 추가
  const [robotPaths, setRobotPaths] = useState<Map<string, PathPoint[]>>(new Map());
  const [robotCurrentCoords, setRobotCurrentCoords] = useState<Map<string, {x: number, y: number}>>(() => {
    // localStorage에서 저장된 위치 복원
    if (typeof window !== 'undefined') {
      try {
        const saved = localStorage.getItem('robotCurrentCoords');
        if (saved) {
          const parsed = JSON.parse(saved);
          console.log('💾 저장된 로봇 위치 복원:', parsed);
          return new Map(Object.entries(parsed));
        }
      } catch (error) {
        console.error('로봇 위치 복원 실패:', error);
      }
    }
    return new Map();
  });

  // robotCurrentCoords가 변경될 때마다 localStorage에 저장
  useEffect(() => {
    if (robotCurrentCoords.size > 0) {
      const coordsObj = Object.fromEntries(robotCurrentCoords);
      localStorage.setItem('robotCurrentCoords', JSON.stringify(coordsObj));
      console.log('💾 로봇 위치 저장됨:', coordsObj);
    }
  }, [robotCurrentCoords]);

  // 🔧 로봇 초기화: 모든 로봇을 MAIN STATION에 배치 (저장된 위치가 없을 때만)
  useEffect(() => {
    const mainStationPos = DESTINATIONS['MAIN STATION'];
    console.log(`🏭 MAIN STATION 좌표: (${mainStationPos.x}, ${mainStationPos.y})`);
    
    const initialCoords = new Map(robotCurrentCoords); // 기존 좌표 유지
    let hasNewRobots = false;
    
    robots.forEach(robot => {
      const robotId = robot.robotCode || robot.id;
      // 저장된 위치가 없는 로봇만 MAIN STATION으로 초기화
      if (!initialCoords.has(robotId)) {
        initialCoords.set(robotId, { x: mainStationPos.x, y: mainStationPos.y });
        console.log(`🔧 [${robotId}] 초기화: MAIN STATION (${mainStationPos.x}, ${mainStationPos.y})`);
        hasNewRobots = true;
      } else {
        const pos = initialCoords.get(robotId)!;
        console.log(`✅ [${robotId}] 저장된 위치 유지: (${pos.x}, ${pos.y})`);
      }
    });

    if (hasNewRobots) {
      setRobotCurrentCoords(initialCoords);
    }
  }, [robots]);

  // SSE 이벤트 처리 - 경로 시스템 사용
  useEffect(() => {
    if (sseMovements.length > 0) {
      const newPaths = new Map(robotPaths);

      sseMovements.forEach(movement => {
        const robotId = movement.robotCode;
        
        let startNode = '';
        let targetNode = '';

        // 1. 이벤트 타입에 따라 출발지와 목적지 결정
        if (movement.eventName === 'MissionStartedEvent' || movement.callLocationName) {
          // 🚀 미션 시작: 무조건 MAIN STATION에서 출발
          startNode = 'MAIN STATION';
          
          if (movement.callLocationName) {
            const map: Record<string, string> = {
              'a': 'STOP1',
              'c': 'GATE'
            };
            const mapped = map[movement.callLocationName.toLowerCase()];
            if (!mapped) {
              console.warn(`⚠️ 알 수 없는 callLocationName: '${movement.callLocationName}' (무시됨)`);
              return;
            }
            targetNode = mapped;
          }
          console.log(`🚀 미션 시작: [${robotId}] MAIN STATION -> ${targetNode}`);
          
        } else if (movement.eventName === 'ReturnStartedEvent') {
          // 🔙 복귀: 현재 위치에서 MAIN STATION으로
          const mainStationPos = DESTINATIONS['MAIN STATION'];
          const currentPos = robotCurrentCoords.get(robotId) || { x: mainStationPos.x, y: mainStationPos.y };
          startNode = findNearestDestination(currentPos.x, currentPos.y);
          targetNode = 'MAIN STATION';
          console.log(`🔙 복귀 시작: [${robotId}] ${startNode} -> MAIN STATION`);
        }

        // 2. 경로 생성
        if (targetNode && startNode) {
          const startUpper = startNode.toUpperCase();
          const targetUpper = targetNode.toUpperCase();
          
          if (startUpper !== targetUpper) {
            const pathData = getNavigationPath(startUpper, targetUpper);
            
            if (pathData) {
              console.log(`✅ 경로 생성: [${robotId}] ${startUpper} -> ${targetUpper}, 웨이포인트: ${pathData.waypoints.length}개`);
              newPaths.set(robotId, pathData.waypoints);
            } else {
              console.error(`❌ 경로 생성 실패: ${startUpper} -> ${targetUpper}`);
            }
          } else {
            console.log(`⚠️ 이미 목적지에 있음: [${robotId}] ${startUpper}`);
          }
        }
      });
      
      setRobotPaths(newPaths);
    }
  }, [sseMovements]);

  // 이동 완료 핸들러
  const handleMoveEnd = (robotId: string) => {
    setRobotPaths(prev => {
      const path = prev.get(robotId);
      if (path && path.length > 0) {
        // 경로의 마지막 지점을 현재 좌표로 저장
        const lastPoint = path[path.length - 1];
        setRobotCurrentCoords(coords => {
          const updated = new Map(coords);
          updated.set(robotId, { x: lastPoint.x, y: lastPoint.y });
          console.log(`✅ [${robotId}] 이동 완료: (${lastPoint.x}, ${lastPoint.y})`);
          return updated;
        });
      }
      
      const next = new Map(prev);
      next.delete(robotId);
      return next;
    });
  };

  const getRobotPosition = (robot: any) => {
    const robotId = robot.robotCode || robot.id;
    
    // robotCurrentCoords에서 현재 위치 가져오기
    const currentPos = robotCurrentCoords.get(robotId);
    if (currentPos) {
      console.log(`🤖 [${robotId}] getRobotPosition: (${currentPos.x}, ${currentPos.y}) from robotCurrentCoords`);
      return { x: currentPos.x, y: currentPos.y };
    }
    
    // 기본 대기 위치: Main Station (상단)
    console.log(`🤖 [${robotId}] getRobotPosition: (0, -70) DEFAULT MAIN STATION`);
    return {
      x: 0,
      y: -70
    };
  };

  const groupedRobots = useMemo(() => {
    const groups = new Map<string, any[]>();
    
    robots.forEach(robot => {
      const pos = getRobotPosition(robot);
      const key = `${Math.round(pos.x / 10)},${Math.round(pos.y / 10)}`;
      
      if (!groups.has(key)) {
        groups.set(key, []);
      }
      groups.get(key)!.push({ ...robot, x: pos.x, y: pos.y });
    });

    return Array.from(groups.values()).map(group => ({
      robots: group,
      x: group[0].x,
      y: group[0].y,
      count: group.length,
      isGroup: group.length >= 3,
      representativeRobot: group[0]
    }));
  }, [robots, robotCurrentCoords]);

  const handleRobotClick = (robot: any) => {
    setSelectedRobot(robot)
  }

  const handleCameraReset = () => {
    if (canvasRef.current) {
      const { camera, controls } = canvasRef.current;
      if (camera && controls) {
        camera.position.set(0, 22, 18);
        controls.target.set(0, 0, 0);
        controls.update();
      }
    }
  }

  return (
    <>
      <div className="relative w-full h-full bg-slate-950 overflow-hidden font-sans rounded-xl border border-slate-800 shadow-2xl group">
        
        <div className="absolute inset-0 rounded-xl opacity-0 group-hover:opacity-100 transition-opacity duration-700 pointer-events-none z-10">
           <div className="absolute inset-0 rounded-xl border border-cyan-500/20 shadow-[inset_0_0_20px_rgba(6,182,212,0.1)]" />
        </div>

        <div className="absolute top-4 left-4 flex flex-col gap-2 pointer-events-none z-[60]">
          <div className="flex items-center gap-2">
             <motion.div animate={{ opacity: [0.5, 1, 0.5] }} transition={{ duration: 2, repeat: Infinity }} className="w-2 h-2 rounded-full bg-green-500 shadow-[0_0_8px_#22c55e]" />
             <span className="text-[10px] font-bold text-slate-400 tracking-widest">LIVE MONITORING</span>
          </div>
          <h2 className="text-xl font-black text-white italic tracking-tighter">SECTOR <span className="text-cyan-400">A-1</span></h2>
        </div>

        <div className="absolute top-4 right-4 flex gap-2 z-[60]">
          {/* 🧪 테스트 네비게이션 버튼 */}
          {/* <motion.button 
            whileHover={{ scale: 1.05 }} 
            whileTap={{ scale: 0.95 }}
            onClick={() => {
              if (robots.length > 0) {
                const testRobot = robots[0];
                const robotId = testRobot.robotCode || testRobot.id;
                console.log('🧪 테스트: MAIN STATION에서 STOP1(a)로 이동');
                
                // 무조건 MAIN STATION에서 출발
                const startNode = 'MAIN STATION';
                const targetNode = 'STOP1';
                
                const pathData = getNavigationPath(startNode, targetNode);
                if (pathData) {
                  console.log(`✅ 테스트 경로: ${startNode} -> ${targetNode}`);
                  setRobotPaths(prev => new Map(prev).set(robotId, pathData.waypoints));
                } else {
                  console.error('경로 생성 실패');
                }
              } else {
                console.warn('로봇이 없습니다');
              }
            }}
            className="px-3 py-1.5 bg-gradient-to-r from-purple-600 to-pink-600 text-white rounded-md text-[10px] font-bold shadow-md hover:shadow-lg transition-all flex items-center gap-1 pointer-events-auto"
          >
            🧪 TEST → A
          </motion.button> */}

          {/* 🔙 복귀 테스트 버튼 */}
          {/* <motion.button 
            whileHover={{ scale: 1.05 }} 
            whileTap={{ scale: 0.95 }}
            onClick={() => {
              if (robots.length > 0) {
                const testRobot = robots[0];
                const robotId = testRobot.robotCode || testRobot.id;
                console.log('🔙 테스트: 복귀 - 현재 위치에서 MAIN STATION으로');
                
                // 현재 위치 확인
                const mainStationPos = DESTINATIONS['MAIN STATION'];
                const currentPos = robotCurrentCoords.get(robotId) || { x: mainStationPos.x, y: mainStationPos.y };
                const startNode = findNearestDestination(currentPos.x, currentPos.y);
                const targetNode = 'MAIN STATION';
                
                console.log(`📍 현재 위치: ${startNode} (${currentPos.x}, ${currentPos.y})`);
                
                if (startNode !== targetNode) {
                  const pathData = getNavigationPath(startNode, targetNode);
                  if (pathData) {
                    console.log(`✅ 복귀 경로: ${startNode} -> ${targetNode}`);
                    setRobotPaths(prev => new Map(prev).set(robotId, pathData.waypoints));
                  } else {
                    console.error('복귀 경로 생성 실패');
                  }
                } else {
                  console.log('⚠️ 이미 MAIN STATION에 있습니다');
                }
              } else {
                console.warn('로봇이 없습니다');
              }
            }}
            className="px-3 py-1.5 bg-gradient-to-r from-green-600 to-emerald-600 text-white rounded-md text-[10px] font-bold shadow-md hover:shadow-lg transition-all flex items-center gap-1 pointer-events-auto"
          >
            🔙 복귀
          </motion.button> */}

          {/* 🔄 위치 초기화 버튼 */}
          <motion.button 
            whileHover={{ scale: 1.05 }} 
            whileTap={{ scale: 0.95 }}
            onClick={() => {
              if (confirm('모든 로봇을 MAIN STATION으로 초기화하시겠습니까?')) {
                const mainStationPos = DESTINATIONS['MAIN STATION'];
                const resetCoords = new Map<string, {x: number, y: number}>();
                
                robots.forEach(robot => {
                  const robotId = robot.robotCode || robot.id;
                  resetCoords.set(robotId, { x: mainStationPos.x, y: mainStationPos.y });
                });
                
                setRobotCurrentCoords(resetCoords);
                setRobotPaths(new Map()); // 모든 경로 삭제
                localStorage.removeItem('robotCurrentCoords');
                console.log('🔄 모든 로봇 위치 초기화 완료');
              }
            }}
            className="px-3 py-1.5 bg-gradient-to-r from-orange-600 to-red-600 text-white rounded-md text-[10px] font-bold shadow-md hover:shadow-lg transition-all flex items-center gap-1 pointer-events-auto"
          >
            🔄 초기화
          </motion.button>

          <div className="bg-slate-900/90 backdrop-blur-md p-1 rounded-lg border border-slate-700 shadow-xl flex">
            <button onClick={() => setViewMode('2d')} className={`flex items-center gap-1.5 px-3 py-1.5 rounded-md text-[10px] font-bold transition-all ${viewMode === '2d' ? 'bg-cyan-500 text-white shadow-lg' : 'text-slate-400 hover:text-white'}`}>
              <LayoutGrid size={12} /> 2D
            </button>
            <div className="w-[1px] bg-slate-700 mx-1 my-1" />
            <button onClick={() => setViewMode('3d')} className={`flex items-center gap-1.5 px-3 py-1.5 rounded-md text-[10px] font-bold transition-all ${viewMode === '3d' ? 'bg-cyan-500 text-white shadow-lg' : 'text-slate-400 hover:text-white'}`}>
              <Box size={12} /> 3D
            </button>
          </div>
        </div>

        <div className="absolute bottom-4 left-4 right-4 flex justify-between items-end pointer-events-none z-[60]">
            <div className="flex gap-2">
               <div className="px-3 py-1.5 bg-slate-900/80 backdrop-blur-md rounded border border-slate-800 text-[10px] text-slate-400 font-mono">ROBOTS: <span className="text-white font-bold">{activeCount}</span></div>
               <div className="px-3 py-1.5 bg-slate-900/80 backdrop-blur-md rounded border border-slate-800 text-[10px] text-slate-400 font-mono">STATUS: <span className="text-emerald-400 font-bold">NORMAL</span></div>
            </div>
            <div className="flex flex-col gap-2 pointer-events-auto z-[60]">
              {viewMode === '3d' && (
                <button 
                  onClick={handleCameraReset}
                  className="p-2 bg-slate-800/80 backdrop-blur text-slate-300 rounded hover:bg-cyan-500 hover:text-white transition-all group"
                  title="원점으로 복귀"
                >
                  <Compass className="w-4 h-4 group-hover:rotate-180 transition-transform duration-500" />
                </button>
              )}
              <button 
                onClick={() => setIsFullScreen(true)}
                className="p-2 bg-slate-800/80 backdrop-blur text-slate-300 rounded hover:bg-cyan-500 hover:text-white transition-colors"
              >
                <Maximize2 className="w-4 h-4" />
              </button>
            </div>
        </div>

        <div className="absolute inset-0 w-full h-full bg-[#0b1121]">
          {viewMode === '2d' ? (
            <MapView2D robots={robots} imageUrl={CHARACTER_IMAGE_URL} onRobotClick={handleRobotClick} />
          ) : (
            <Canvas 
              shadows 
              dpr={[1.5, 2]} 
              camera={{ position: [0, 22, 18], fov: 40 }}
              onCreated={(state) => { canvasRef.current = state; }}
              gl={{ antialias: true, alpha: false }}
            >
              <color attach="background" args={['#0b1121']} />
              <fog attach="fog" args={['#0b1121', 20, 55]} />
              <Environment preset="city" />
              <ambientLight intensity={0.6} />
              <pointLight position={[-10, 10, -10]} intensity={1.5} color="#fbbf24" distance={20} />
              <pointLight position={[10, 10, 10]} intensity={1.5} color="#3b82f6" distance={20} />
              <directionalLight position={[5, 20, 5]} intensity={2.5} castShadow shadow-mapSize={[2048, 2048]} />
              
              <gridHelper args={[60, 60, 0x1e293b, 0x111827]} position={[0, -0.01, 0]} />
              
              <mesh rotation-x={-Math.PI / 2} receiveShadow position={[0, -0.02, 0]}>
                <planeGeometry args={[100, 100]} />
                <meshStandardMaterial color="#0f172a" roughness={0.4} metalness={0.6} />
              </mesh>

              {MAP_ZONES.map(zone => <Zone3D key={zone.id} data={zone} />)}

              <Suspense fallback={null}>
                {groupedRobots.map((group, idx) => {
                  const robotId = group.representativeRobot.robotCode || group.representativeRobot.id;
                  const path = robotPaths.get(robotId);
                  
                  return (
                    <GlbRobot3D
                      key={`robot-group-${idx}`}
                      robotCode={group.isGroup ? `${group.count}대` : (group.representativeRobot.robotCode || group.representativeRobot.id)}
                      position={[group.x / 10, 0, group.y / 10]}
                      activePath={path}
                      status={group.representativeRobot.status}
                      onClick={() => handleRobotClick(group.representativeRobot)}
                      onMoveEnd={() => handleMoveEnd(robotId)}
                      isGroup={group.isGroup}
                      groupCount={group.count}
                      showPath={!!path}
                    />
                  );
                })}
              </Suspense>

              <OrbitControls 
                maxPolarAngle={Math.PI / 2.1} 
                minDistance={10} 
                maxDistance={40} 
                enablePan={true}
                target={[0, 0, 0]}
              />
            </Canvas>
          )}
        </div>

        {selectedRobot && <RobotDetailModal robot={selectedRobot} onClose={() => setSelectedRobot(null)} />}
      </div>

      {isFullScreen && (
        <FullScreenModal
          viewMode={viewMode}
          robots={robots}
          groupedRobots={groupedRobots}
          robotPaths={robotPaths}
          imageUrl={CHARACTER_IMAGE_URL}
          onClose={() => setIsFullScreen(false)}
          onRobotClick={handleRobotClick}
          onMoveEnd={handleMoveEnd}
        />
      )}
    </>
  )
}