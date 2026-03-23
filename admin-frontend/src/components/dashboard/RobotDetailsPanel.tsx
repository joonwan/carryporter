/**
 * 로봇 상세 정보 패널
 * 지도에서 로봇 클릭 시 상세 정보 표시
 */
export default function RobotDetailsPanel() {
    return (
        <div className="card p-6 text-center">
            <div className="flex flex-col items-center gap-2">
                <span className="text-5xl text-text-muted">🤖</span>
                <p className="text-text-secondary text-sm">
                    Click on a robot in the map to view details
                </p>
            </div>
        </div>
    )
}
