import type { Location } from "../types/mission.types";

/**
 * 픽업 장소 목록 (3개)
 *
 * 각 장소는 고유한 SVG 아이콘을 가지며, 백엔드 API와 통신 시 code 값을 사용합니다.
 * MAIN은 중앙 사물함이므로 픽업 장소에서 제외됩니다.
 */
export const PICKUP_LOCATIONS: Location[] = [
    {
        id: 2,
        name: "STOP 1",
        code: "2",
        type: "pickup",
        icon: "/images/STOP1.svg"
    },
    {
        id: 3,
        name: "STOP 2",
        code: "3",
        type: "pickup",
        icon: "/images/STOP2.svg"
    },
    {
        id: 4,
        name: "GATE 1",
        code: "4",
        type: "pickup",
        icon: "/images/GATE1.svg"
    },
];

/**
 * 전체 위치 목록 (픽업 장소)
 */
export const ALL_LOCATIONS: Location[] = PICKUP_LOCATIONS;

/**
 * [백업] 기존 정류장 목록 (12개 → 4개로 단순화)
 *
 * 향후 필요 시 복구할 수 있도록 주석으로 보관
 */
// export const STATIONS: Location[] = [
//     { id: 1, name: "1번 정류장", code: "STATION_1", type: "station", icon: "🚉" },
//     { id: 2, name: "2번 정류장", code: "STATION_2", type: "station", icon: "🚉" },
//     { id: 3, name: "3번 정류장", code: "STATION_3", type: "station", icon: "🚉" },
//     { id: 4, name: "4번 정류장", code: "STATION_4", type: "station", icon: "🚉" },
//     { id: 5, name: "5번 정류장", code: "STATION_5", type: "station", icon: "🚉" },
//     { id: 6, name: "6번 정류장", code: "STATION_6", type: "station", icon: "🚉" },
// ];

// export const BOARDING_GATES: Location[] = [
//     { id: 2, name: "GATE 1", code: "2", type: "gate", icon: "🚪" },
//     { id: 3, name: "GATE 2", code: "3", type: "gate", icon: "🚪" },
//     { id: 4, name: "GATE 3", code: "4", type: "gate", icon: "🚪" },
//     { id: 10, name: "GATE 4", code: "GATE_4", type: "gate", icon: "🚪" },
//     { id: 11, name: "GATE 5", code: "GATE_5", type: "gate", icon: "🚪" },
//     { id: 12, name: "GATE 6", code: "GATE_6", type: "gate", icon: "🚪" },
// ];
