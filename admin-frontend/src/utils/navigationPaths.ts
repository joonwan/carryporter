// ==============================================================================
// 🗺️ 삼각형 네비게이션 경로 시스템 (원형 호 이동 & 좌회전)
// ==============================================================================

export interface PathPoint {
  x: number;
  y: number;
  delay?: number;
}

export interface NavigationPath {
  name: string;
  destination: string;
  waypoints: PathPoint[];
  totalDuration: number;
}

// ==============================================================================
// 📍 정삼각형 배치 좌표 (원에 내접, 반지름 70, 중심 0,0)
// 3D 좌표계: x는 좌우, y(z)는 상하 (-가 위쪽, +가 아래쪽)
// ==============================================================================
const CIRCLE_RADIUS = 70;
const CIRCLE_CENTER = { x: 0, y: 0 };

// 각 스테이션의 원 위 각도 (CW 순서로 120°씩 감소)
const STATION_ANGLES: Record<string, number> = {
  'MAIN STATION': -Math.PI / 2,                       // -90° (상단)
  'STOP1': -Math.PI / 2 - (2 * Math.PI) / 3,          // -210° ≡ 150° (좌하)
  'GATE': -Math.PI / 2 - (4 * Math.PI) / 3,           // -330° ≡ 30° (우하)
};

export const DESTINATIONS = {
  'MAIN STATION': {
    x: Math.round(CIRCLE_CENTER.x + CIRCLE_RADIUS * Math.cos(STATION_ANGLES['MAIN STATION'])),
    y: Math.round(CIRCLE_CENTER.y + CIRCLE_RADIUS * Math.sin(STATION_ANGLES['MAIN STATION'])),
  },
  'STOP1': {
    x: Math.round(CIRCLE_CENTER.x + CIRCLE_RADIUS * Math.cos(STATION_ANGLES['STOP1'])),
    y: Math.round(CIRCLE_CENTER.y + CIRCLE_RADIUS * Math.sin(STATION_ANGLES['STOP1'])),
  },
  'GATE': {
    x: Math.round(CIRCLE_CENTER.x + CIRCLE_RADIUS * Math.cos(STATION_ANGLES['GATE'])),
    y: Math.round(CIRCLE_CENTER.y + CIRCLE_RADIUS * Math.sin(STATION_ANGLES['GATE'])),
  },
} as const;

// 🔄 시계 방향(CW) 순서 정의 = 좌회전 경로
const CW_ORDER: (keyof typeof DESTINATIONS)[] = [
  'MAIN STATION',
  'STOP1',
  'GATE'
];

/**
 * 원호를 따라 점을 보간 (startAngle → endAngle, CW 방향)
 */
const interpolateArc = (
  startAngle: number,
  endAngle: number,
  steps: number
): PathPoint[] => {
  const points: PathPoint[] = [];
  for (let i = 1; i <= steps; i++) {
    const t = i / steps;
    const angle = startAngle + (endAngle - startAngle) * t;
    points.push({
      x: CIRCLE_CENTER.x + CIRCLE_RADIUS * Math.cos(angle),
      y: CIRCLE_CENTER.y + CIRCLE_RADIUS * Math.sin(angle),
    });
  }
  return points;
};

/**
 * 시계 방향(CW = 좌회전)으로 원호를 타고 이동하는 경로 생성
 */
const createCircularPath = (
  fromName: keyof typeof DESTINATIONS,
  toName: keyof typeof DESTINATIONS,
  stepsPerEdge: number = 60
): PathPoint[] => {
  let waypoints: PathPoint[] = [];

  let currentIndex = CW_ORDER.indexOf(fromName);

  // CW 방향으로 다음 스테이션을 하나씩 거쳐감
  while (CW_ORDER[currentIndex] !== toName) {
    const fromStop = CW_ORDER[currentIndex];

    // 다음 인덱스 (순환)
    currentIndex = (currentIndex + 1) % CW_ORDER.length;

    // 원호 보간: CW 방향이므로 각도가 감소 (-2π/3씩)
    const fromAngle = STATION_ANGLES[fromStop];
    const toAngle = fromAngle - (2 * Math.PI) / 3; // 항상 120° CW

    const arcSegment = interpolateArc(fromAngle, toAngle, stepsPerEdge);
    waypoints = [...waypoints, ...arcSegment];
  }

  return waypoints;
};

// ==============================================================================
// 📍 경로 자동 생성 및 헬퍼 함수
// ==============================================================================

export const NAVIGATION_PATHS: Record<string, NavigationPath> = {};

const destinationKeys = Object.keys(DESTINATIONS) as Array<keyof typeof DESTINATIONS>;

// 모든 가능한 출발-도착 조합에 대해 경로 미리 계산
destinationKeys.forEach(from => {
  destinationKeys.forEach(to => {
    if (from !== to) {
      const key = `${from}-${to}`;
      const waypoints = createCircularPath(from, to);

      NAVIGATION_PATHS[key] = {
        name: `${from} to ${to}`,
        destination: to,
        waypoints: waypoints,
        totalDuration: (waypoints.length / 60) * 3,
      };
    }
  });
});

/**
 * 현재 위치(좌표)에서 가장 가까운 정거장 이름 찾기
 */
export const findNearestDestination = (x: number, y: number): string => {
  let nearest = 'MAIN STATION';
  let minDistance = Infinity;

  Object.entries(DESTINATIONS).forEach(([name, coords]) => {
    const distance = Math.sqrt(Math.pow(coords.x - x, 2) + Math.pow(coords.y - y, 2));
    if (distance < minDistance) {
      minDistance = distance;
      nearest = name;
    }
  });

  return nearest;
};

/**
 * 두 지점 이름으로 경로 데이터 가져오기
 */
export const getNavigationPath = (
  fromDestination: string,
  toDestination: string
): NavigationPath | null => {
  const key = `${fromDestination.toUpperCase()}-${toDestination.toUpperCase()}`;
  return NAVIGATION_PATHS[key] || null;
};
