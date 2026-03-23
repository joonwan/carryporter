import { Button } from "@/components/ui/button";

interface CameraErrorViewProps {
    onRetry: () => void;
    onClose: () => void;
}

/**
 * 카메라 접근 실패 시 표시되는 에러 뷰
 */
const CameraErrorView = ({ onRetry, onClose }: CameraErrorViewProps) => (
    <div className="fixed inset-0 bg-toss-gradient flex items-center justify-center px-4">
        <div className="bg-white rounded-2xl p-8 max-w-md w-full text-center shadow-xl animate-fade-in-scale">
            <div className="w-20 h-20 mx-auto mb-6 rounded-full bg-red-50 flex items-center justify-center">
                <svg
                    className="w-10 h-10 text-red-500"
                    fill="none"
                    viewBox="0 0 24 24"
                    stroke="currentColor"
                >
                    <path
                        strokeLinecap="round"
                        strokeLinejoin="round"
                        strokeWidth={2}
                        d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"
                    />
                </svg>
            </div>

            <h2 className="text-2xl font-bold text-gray-900 mb-3">
                카메라 접근 실패
            </h2>
            <p className="text-gray-500 mb-8 leading-relaxed">
                카메라 권한을 허용해주세요.
                <br />
                설정에서 카메라 접근을 활성화할 수 있습니다.
            </p>

            <div className="space-y-3">
                <Button
                    onClick={onRetry}
                    className="w-full h-14 text-lg font-semibold bg-toss-blue-500 hover:bg-toss-blue-600 text-white rounded-xl"
                >
                    다시 시도
                </Button>
                <Button
                    onClick={onClose}
                    variant="outline"
                    className="w-full h-14 text-lg font-semibold rounded-xl"
                >
                    홈으로 돌아가기
                </Button>
            </div>
        </div>
    </div>
);

export default CameraErrorView;
