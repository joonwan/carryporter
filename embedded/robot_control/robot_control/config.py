"""
자율주행 자동차 설정 파일
- 모터, 카메라, 인식 시스템 파라미터
"""
from dataclasses import dataclass, field
from typing import Tuple


@dataclass
class MotorConfig:
    """모터 제어 설정"""
    # PCA9685 설정
    i2c_bus: int = 7            # I2C 버스 번호
    pca_address: int = 0x60     # PCA9685 주소 (servo_motor.py와 동일)
    pwm_frequency: int = 50     # 서보모터 표준 주파수

    # 서보 모터 (조향)
    servo_channel: int = 0
    servo_center: float = 120     # 중앙 각도
    servo_min: int = 30       # 최대 좌회전
    servo_max: int = 150       # 최대 우회전

    # 조향 오프셋 (하드웨어 보정용)
    # 양수 = 우측 보정, 음수 = 좌측 보정
    steering_offset: float = 0.0

    # DC 모터 (속도)
    dc_channel: int = 0        # 채널 3,4,5 사용 (오프셋 적용)
    max_throttle: float = 0.40  # 최대 속도 비율 (95% - 최대 출력)


@dataclass
class CameraConfig:
    """카메라 설정"""
    camera_id: int = 0
    width: int = 640
    height: int = 480
    fps: int = 30


@dataclass
class LaneConfig:
    """차선 인식 설정 (흰바닥 + 검정 테이프)"""

    # ROI (Region of Interest) 설정
    roi_top_ratio: float = 0.5      # ROI 시작 위치 (화면 하단 30%만 - 바닥만 보이도록)
    roi_bottom_ratio: float = 1.0   # ROI 끝 위치

    # 이진화 설정 (적응형 이진화용)
    adaptive_block_size: int = 17   # 적응형 이진화 블록 크기 (큰 값 = 넓은 영역 평균)
    adaptive_c: int = 10            # 적응형 이진화 상수 (높을수록 어두운 것만)

    # 대체 고정 임계값 (단순 이진화용)
    dark_threshold: int = 60        # 검정 차선 임계값

    # 가우시안 블러
    blur_kernel: Tuple[int, int] = (5, 5)

    # 모폴로지 연산
    morph_kernel_size: int = 3
    dilate_iterations: int = 1

    # Hough 변환 설정
    hough_threshold: int = 30       # 얇은 테이프를 위해 낮춤
    min_line_length: int = 30       # 최소 선 길이
    max_line_gap: int = 50          # 최대 선 간격

    # 차선 분류
    min_slope: float = 0.3          # 최소 기울기 (수평선 제외)

    # 조향 설정
    steering_gain: float = 0.4      # 조향 게인 (0.15→0.3, 더 민감하게)
    max_steering_angle: float = 30  # 최대 조향각

    # 주행 모드
    single_lane_mode: bool = True   # True: 단일 테이프 추적, False: 양쪽 차선 중앙


@dataclass
class LaneConfigHSV:
    """HSV 기반 차선 인식 설정 (흰바닥 + 검정 테이프)"""
    # ROI (Region of Interest) 설정
    roi_top_ratio: float = 0.3      # ROI 시작 위치 (화면 상단 40% 아래부터)
    roi_bottom_ratio: float = 1.0   # ROI 끝 위치

    # HSV 검정색 필터링 범위
    hsv_h_min: int = 0              # Hue 최소 (0-180)
    hsv_h_max: int = 180            # Hue 최대
    hsv_s_min: int = 0              # Saturation 최소 (0-255)
    hsv_s_max: int = 255            # Saturation 최대
    hsv_v_max: int = 50             # Value 최대 (0-255, 검정색 = 낮은 V)

    # 모폴로지 연산 (노이즈 제거)
    morph_kernel_size: int = 3
    morph_open_iterations: int = 2  # Opening 반복 횟수
    morph_close_iterations: int = 2 # Closing 반복 횟수

    # Canny Edge 검출
    canny_low: int = 50             # Canny 하한 임계값
    canny_high: int = 150           # Canny 상한 임계값

    # Hough 변환 설정
    hough_threshold: int = 30       # 얇은 테이프를 위해 낮춤
    min_line_length: int = 40       # 최소 선 길이
    max_line_gap: int = 50          # 최대 선 간격

    # 차선 분류
    min_slope: float = 0.3          # 최소 기울기 (수평선 제외)

    # 조향 설정
    steering_gain: float = 0.3      # 조향 게인
    max_steering_angle: float = 30  # 최대 조향각


@dataclass
class YOLOConfig:
    """YOLOv8 객체 탐지 설정"""
    model_path: str = 'yolov8n.pt'
    img_size: int = 640
    conf_threshold: float = 0.5

    # 모든 YOLO 클래스 사용 (실내 트랙에서 모든 장애물 감지)
    use_all_classes: bool = True


@dataclass
class PerceptionConfig:
    """통합 인식 시스템 설정"""
    # 거리 임계값 (화면 비율, 0=상단, 1=하단)
    # distance_ratio가 낮을수록 가까움 (화면 하단)
    emergency_distance: float = 0.95    # 긴급 정지 (매우 가까이, 하단 95%)
    slow_distance: float = 0.80         # 감속 (하단 80%)

    # 경로 폭 설정
    path_width_ratio: float = 0.35      # 차선 중심 기준 좌우 폭 (35% = 좁은 경로)

    # 장애물 필터링
    min_obstacle_height: int = 30       # 최소 장애물 높이 (픽셀)
    consecutive_frames: int = 2         # 연속 감지 프레임 수 (오탐지 방지)

    # 속도 비율
    normal_speed: float = 1.0
    slow_speed: float = 0.7            # 감속 시 70%
    lane_lost_speed: float = 0.5       # 차선 유실 시 50%


@dataclass
class SafetyConfig:
    """안전 설정"""
    watchdog_timeout: float = 1.0       # 워치독 타임아웃 (초)
    perception_timeout: float = 0.5     # 인식 타임아웃 (초)
    obstacle_threshold: int = 5         # 연속 장애물 프레임 수

    # 제어 루프
    control_rate: int = 30              # Hz


@dataclass
class Config:
    """통합 설정"""
    motor: MotorConfig = field(default_factory=MotorConfig)
    camera: CameraConfig = field(default_factory=CameraConfig)
    lane: LaneConfig = field(default_factory=LaneConfig)
    lane_hsv: LaneConfigHSV = field(default_factory=LaneConfigHSV)
    yolo: YOLOConfig = field(default_factory=YOLOConfig)
    perception: PerceptionConfig = field(default_factory=PerceptionConfig)
    safety: SafetyConfig = field(default_factory=SafetyConfig)

    @classmethod
    def load_default(cls) -> 'Config':
        """기본 설정 로드"""
        return cls()

    def update_for_slow_test(self):
        """저속 테스트용 설정"""
        self.motor.max_throttle = 0.2
        self.perception.slow_speed = 0.15
        self.perception.lane_lost_speed = 0.1
