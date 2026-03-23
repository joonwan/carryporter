//  import { useState, useRef, Suspense, useMemo, useEffect } from 'react' // useEffect 추가

// import { Canvas, useFrame } from '@react-three/fiber'

// import { OrbitControls, useGLTF, Clone, Html, Text, Environment } from '@react-three/drei' // Environment 추가

// import * as THREE from 'three'

// import { motion } from 'framer-motion'

// import { LayoutGrid, Box, Maximize2, AlertTriangle } from 'lucide-react'

// import RobotDetailModal from './RobotDetailModal'



// // ==============================================================================

// // 🛠️ 설정 (맵 레이아웃 재구성)

// // ==============================================================================

// const ROBOT_GLB_URL = "./models/carryporter.glb"

// const CHARACTER_IMAGE_URL = "/cat_icon.png"



// const MAP_WIDTH = 24

// const MAP_HEIGHT = 16



// const MAP_ZONES = [

//   { id: 'main', type: 'station', x: -8, y: 0, w: 6, h: 12, color: '#fbbf24', label: 'MAIN STATION' },

//   { id: 'stop2', type: 'stop', x: 0, y: -5, w: 3, h: 3, color: '#34d399', label: 'STOP-2' },

//   { id: 'gate1', type: 'gate', x: 8, y: -5, w: 4, h: 4, color: '#3b82f6', label: 'GATE-1' },

//   { id: 'stop1', type: 'stop', x: 0, y: 5, w: 3, h: 3, color: '#34d399', label: 'STOP-1' },

//   { id: 'obstacle', type: 'obstacle', x: 7, y: 4, w: 6, h: 4, color: '#f43f5e', label: 'RESTRICTED' },

// ]



// // ------------------------------------------------------------------

// // 🧊 [Component] GLB Robot 3D (밝기 강화 버전)

// // ------------------------------------------------------------------

// function GlbRobot3D({

//   position,

//   status,

//   robotCode,

//   onClick

// }: {

//   position: [number, number, number],

//   status: string,

//   robotCode: string,

//   onClick: () => void

// }) {

//   const { scene } = useGLTF(ROBOT_GLB_URL)

//   const groupRef = useRef<THREE.Group>(null)

//   const [hovered, setHovered] = useState(false)



//   // ✨ 로봇 재질 밝게 만들기 (모델 로드 시 1회 실행)

//   useEffect(() => {

//     scene.traverse((child) => {

//       if ((child as THREE.Mesh).isMesh) {

//         const mesh = child as THREE.Mesh;

//         const material = mesh.material as THREE.MeshStandardMaterial;

//         // 주변광에 더 잘 반응하도록 설정

//         material.envMapIntensity = 1.5;

//         // 자체 발광 약간 추가 (너무 어두운 텍스처일 경우 대비)

//         material.emissive = new THREE.Color(0x202020);

//         material.emissiveIntensity = 0.2;

//         material.needsUpdate = true;

//       }

//     });

//   }, [scene]);



//   useFrame((state) => {

//     const t = state.clock.getElapsedTime()

//     if (groupRef.current) {

//       groupRef.current.position.y = Math.sin(t * 2) * 0.1 + (hovered ? 0.3 : 0)

//     }

//     if (state.gl.domElement) {

//       state.gl.domElement.style.cursor = hovered ? 'pointer' : 'auto'

//     }

//   })



//   const isAvailable = status === 'available'

//   const statusColor = isAvailable ? '#10b981' : '#3b82f6'



//   return (

//     <group position={[position[0], 0, position[2]]}>

//       {/* ✨ 로봇 전용 하이라이트 조명 (로봇을 따라다님) */}

//       <spotLight

//         position={[0, 4, 0]}

//         intensity={5}   // 강도 높임

//         distance={8}

//         angle={0.6}

//         penumbra={0.5}

//         color="#ffffff"

//       />

//       <pointLight position={[0, 1, 1]} intensity={2} distance={3} color="#ffffff" />



//       <group

//         ref={groupRef}

//         onPointerOver={(e) => { e.stopPropagation(); setHovered(true); }}

//         onPointerOut={(e) => { e.stopPropagation(); setHovered(false); }}

//         onClick={(e) => { e.stopPropagation(); onClick(); }}

//       >

//         <Clone object={scene} scale={hovered ? 1.65 : 1.5} position={[0, 1.8, 0]} rotation={[0, 0, 0]} />

//       </group>



//       {/* 바닥 링 */}

//       <mesh rotation-x={-Math.PI / 2} position={[0, 0.05, 0]}>

//         <ringGeometry args={[0.5, hovered ? 1.0 : 0.8, 32]} />

//         <meshBasicMaterial color={statusColor} opacity={hovered ? 0.8 : 0.6} transparent />

//       </mesh>

     

//       {/* 로봇 아래 글로우 효과 */}

//       <pointLight position={[0, 1, 0]} color={statusColor} intensity={hovered ? 4 : 2} distance={3} decay={2} />



//       {/* 3D 라벨 */}

//       <Html position={[0, 3.5, 0]} center distanceFactor={12} zIndexRange={[0, 0]}>

//         <div className="flex flex-col items-center transform transition-transform hover:scale-110">

//           <div className={`

//             flex items-center gap-1 px-2 py-1 rounded-md border backdrop-blur-md shadow-lg transition-all

//             ${isAvailable

//               ? 'bg-slate-900/80 border-emerald-500/50 text-emerald-400'

//               : 'bg-slate-900/80 border-blue-500/50 text-blue-400'}

//             ${hovered ? 'scale-110 shadow-xl' : ''}

//           `}>

//             <span className="text-[10px] font-black tracking-tighter">{robotCode}</span>

//           </div>

//           <div className={`w-0.5 h-4 ${isAvailable ? 'bg-emerald-500/50' : 'bg-blue-500/50'}`} />

//         </div>

//       </Html>

//     </group>

//   )

// }



// useGLTF.preload(ROBOT_GLB_URL)



// // ------------------------------------------------------------------

// // 🧊 [Component] Zone 3D

// // ------------------------------------------------------------------

// function Zone3D({ data }: { data: typeof MAP_ZONES[0] }) {

//   const isObstacle = data.type === 'obstacle';

//   const isStation = data.type === 'station';



//   return (

//     <group position={[data.x, 0, data.y]}>

//       {isObstacle ? (

//         <group>

//           <mesh position={[0, 0.5, 0]}>

//             <boxGeometry args={[data.w, 1, data.h]} />

//             <meshStandardMaterial color={data.color} transparent opacity={0.3} wireframe />

//           </mesh>

//           <mesh position={[0, 0.5, 0]}>

//              <boxGeometry args={[data.w * 0.95, 0.9, data.h * 0.95]} />

//              <meshStandardMaterial color="#500000" transparent opacity={0.5} />

//           </mesh>

//           <mesh rotation-x={-Math.PI / 2} position={[0, 0.02, 0]}>

//             <planeGeometry args={[Math.min(data.w, data.h), Math.min(data.w, data.h)]} />

//             <meshBasicMaterial color={data.color} transparent opacity={0.2} alphaMap={null} />

//           </mesh>

//           <Text position={[0, 1.5, 0]} fontSize={2} color={data.color} rotation={[-Math.PI/2, 0, 0]}>X</Text>

//         </group>

//       ) : (

//         <group>

//           <mesh rotation-x={-Math.PI / 2} receiveShadow position={[0, 0.01, 0]}>

//             <planeGeometry args={[data.w, data.h]} />

//             <meshStandardMaterial color={data.color} transparent opacity={0.15} side={THREE.DoubleSide} />

//           </mesh>

//           <mesh position={[0, 0.05, 0]} rotation-x={-Math.PI / 2}>

//             <boxGeometry args={[data.w, 0.05, data.h]} />

//             <meshStandardMaterial color={data.color} opacity={0.4} transparent emissive={data.color} emissiveIntensity={0.5} />

//           </mesh>

//           {[[-1, -1], [-1, 1], [1, -1], [1, 1]].map((dir, i) => (

//             <mesh key={i} position={[dir[0] * data.w / 2, 0.25, dir[1] * data.h / 2]}>

//               <cylinderGeometry args={[0.05, 0.05, 0.5, 8]} />

//               <meshStandardMaterial color={data.color} emissive={data.color} emissiveIntensity={2} />

//             </mesh>

//           ))}

//           {isStation && (

//              <Text position={[0, 0.1, 0]} rotation={[-Math.PI/2, 0, 0]} fontSize={2} color={data.color} fillOpacity={0.2}>⚡</Text>

//           )}

//         </group>

//       )}

//       <Html position={[0, isObstacle ? 1.5 : 1, 0]} center transform sprite zIndexRange={[0, 0]}>

//         <div className={`

//           text-xs font-black tracking-widest whitespace-nowrap pointer-events-none select-none px-2 py-0.5 rounded

//           ${isObstacle ? 'bg-red-500/20 text-red-500 border border-red-500/50' : `text-${data.color} opacity-80`}

//         `} style={{ color: data.color, textShadow: `0 0 10px ${data.color}` }}>

//           {data.label}

//         </div>

//       </Html>

//     </group>

//   )

// }



// // ------------------------------------------------------------------

// // 🗺️ [Component] 2D Map View

// // ------------------------------------------------------------------

// function MapView2D({ robots, imageUrl, onRobotClick }: { robots: any[], imageUrl: string, onRobotClick: (robot: any) => void }) {

//   const toPercentX = (x: number) => ((x + MAP_WIDTH / 2) / MAP_WIDTH) * 100

//   const toPercentY = (y: number) => ((y + MAP_HEIGHT / 2) / MAP_HEIGHT) * 100



//   return (

//     <div className="w-full h-full relative bg-slate-950 overflow-hidden select-none font-sans group">

//       <div className="absolute inset-0 opacity-20" style={{

//         backgroundImage: 'linear-gradient(#334155 1px, transparent 1px), linear-gradient(90deg, #334155 1px, transparent 1px)',

//         backgroundSize: '40px 40px'

//       }} />



//       {MAP_ZONES.map((zone) => {

//          const isObstacle = zone.type === 'obstacle';

//          return (

//           <div key={zone.id}

//             className={`absolute border flex flex-col items-center justify-center text-xs font-bold rounded-md transition-opacity duration-300

//               ${isObstacle ? 'opacity-80' : 'opacity-60 hover:opacity-100'}

//             `}

//             style={{

//               left: `${toPercentX(zone.x - zone.w / 2)}%`,

//               top: `${toPercentY(zone.y - zone.h / 2)}%`,

//               width: `${(zone.w / MAP_WIDTH) * 100}%`,

//               height: `${(zone.h / MAP_HEIGHT) * 100}%`,

//               borderColor: zone.color,

//               backgroundColor: isObstacle

//                 ? `repeating-linear-gradient(45deg, ${zone.color}20, ${zone.color}20 10px, ${zone.color}40 10px, ${zone.color}40 20px)`

//                 : `${zone.color}10`,

//               color: zone.color,

//               boxShadow: `0 0 20px ${zone.color}15`

//             }}>

//             {isObstacle && <AlertTriangle className="w-6 h-6 mb-1 animate-pulse" />}

//             {zone.label}

//           </div>

//         )

//       })}



//       {robots.map((robot) => {

//         const xPos = robot.x ?? 0;

//         const yPos = robot.y ?? 0;

//         const isAvailable = robot.status === 'available';

//         return (

//           <button

//             key={robot.robotCode || robot.id}

//             onClick={() => onRobotClick(robot)}

//             className="absolute flex flex-col items-center justify-center transition-all duration-700 ease-out z-20 cursor-pointer hover:scale-110"

//             style={{

//               left: `${toPercentX(xPos / 10)}%`,

//               top: `${toPercentY(yPos / 10)}%`,

//               transform: 'translate(-50%, -50%)'

//             }}

//           >

//             <div className={`absolute w-16 h-16 rounded-full opacity-30 animate-ping ${isAvailable ? 'bg-emerald-500' : 'bg-blue-500'}`} />

//             <div className="relative transform hover:scale-110 transition-transform">

//               <div className={`w-8 h-8 rounded-full border-2 bg-slate-900 flex items-center justify-center overflow-hidden ${isAvailable ? 'border-emerald-400' : 'border-blue-400'}`}>

//                  <img src={imageUrl} alt="Bot" className="w-full h-full object-cover" onError={(e) => { (e.target as HTMLImageElement).style.display='none'; }}/>

//                  <span className="absolute text-lg">🤖</span>

//               </div>

//             </div>

//             <span className="text-[9px] font-bold text-white bg-slate-900/90 px-1.5 py-0.5 rounded shadow-sm mt-1 border border-slate-700 whitespace-nowrap">

//               {robot.robotCode || robot.id}

//             </span>

//           </button>

//         );

//       })}

//     </div>

//   )

// }



// // ------------------------------------------------------------------

// // 🚀 [Main Page] Robot Stage

// // ------------------------------------------------------------------

// export default function RobotStage({

//   robots = [],

//   showDummyIfEmpty = false

// }: {

//   robots: any[];

//   showDummyIfEmpty?: boolean;

// }) {

//   const [viewMode, setViewMode] = useState<'2d' | '3d'>('3d')

//   const [selectedRobot, setSelectedRobot] = useState<any | null>(null)

//   const activeCount = useMemo(() => robots.length, [robots]);



//   const handleRobotClick = (robot: any) => {

//     setSelectedRobot(robot)

//   }