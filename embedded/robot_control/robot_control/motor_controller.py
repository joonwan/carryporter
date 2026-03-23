"""
모터 제어 모듈
- DC 모터 (속도) + 서보 모터 (조향) 통합 제어
- PCA9685 PWM 컨트롤러 사용
"""
import time
import threading
from typing import Optional
from dataclasses import dataclass


@dataclass
class MotorState:
    """모터 상태"""
    steering_angle: float = 0.0     # -30 ~ +30
    servo_angle: float = 100.0      # 80 ~ 120
    speed_ratio: float = 0.0        # 0.0 ~ 1.0
    throttle: float = 0.0           # -1.0 ~ 1.0
    is_emergency_stop: bool = False


class MotorController:
    """
    DC 모터 + 서보 모터 통합 제어기

    하드웨어 구성:
    - PCA9685 PWM 컨트롤러 (I2C 주소: 0x60)
    - 서보 모터: 채널 0 (조향)
    - DC 모터: 채널 3, 4, 5 (방향 + PWM)
    """

    def __init__(self, config):
        """
        Args:
            config: MotorConfig 객체
        """
        self.i2c_bus = config.i2c_bus
        self.pca_address = config.pca_address
        self.pwm_frequency = config.pwm_frequency

        # 서보 설정
        self.servo_channel = config.servo_channel
        self.servo_center = config.servo_center
        self.servo_min = config.servo_min
        self.servo_max = config.servo_max

        # DC 모터 설정
        self.dc_channel = config.dc_channel
        self.max_throttle = config.max_throttle

        # 상태
        self.state = MotorState()
        self._lock = threading.Lock()

        # I2C 보호: 최소 명령 간격 (초)
        self._min_cmd_interval = 0.02  # 20ms (최대 50Hz)
        self._last_servo_time = 0.0
        self._last_motor_time = 0.0
        self._i2c_retry_count = 3
        self._i2c_retry_delay = 0.01  # 10ms

        # 하드웨어 초기화 (지연)
        self._pca_servo = None      # 서보용 PCA9685 (0x60)
        self._pca_motor = None      # DC모터용 Motor HAT (0x40)
        self._servo_kit = None
        self._initialized = False

    def initialize(self) -> bool:
        """
        하드웨어 초기화 (재시도 포함)

        Returns:
            성공 여부
        """
        if self._initialized:
            return True

        max_retries = 3
        for attempt in range(max_retries):
            success = self._try_initialize()
            if success:
                return True
            print(f"[MotorController] 초기화 시도 {attempt + 1}/{max_retries} 실패, 재시도...")
            time.sleep(1.0)  # 1초 대기 후 재시도

        print("[MotorController] 모든 초기화 시도 실패!")
        return False

    def _try_initialize(self) -> bool:
        """단일 초기화 시도"""
        try:
            import os
            from adafruit_pca9685 import PCA9685
            from adafruit_servokit import ServoKit
            import busio
            import board

            # CRITICAL: Set BLINKA_I2C_BUS environment variable BEFORE creating I2C
            # This tells Blinka to use the specified I2C bus number
            os.environ['BLINKA_I2C_BUS'] = str(self.i2c_bus)

            # Create I2C bus using busio (which supports try_lock/unlock)
            # The environment variable will make it use bus 7
            i2c = busio.I2C(board.SCL, board.SDA)

            # 서보 모터용 PCA9685 초기화 (0x60)
            self._pca_servo = PCA9685(i2c, address=0x60)
            self._pca_servo.frequency = self.pwm_frequency

            # DC 모터용 Motor HAT 초기화 (0x40)
            self._pca_motor = PCA9685(i2c, address=0x40)
            self._pca_motor.frequency = self.pwm_frequency

            # 명시적으로 슬립 모드 해제 확인
            import time
            time.sleep(0.01)  # 안정화 대기

            # ServoKit 초기화 (서보용 0x60)
            self._servo_kit = ServoKit(
                channels=16,
                i2c=i2c,
                address=0x60
            )

            # 초기 위치
            self._set_servo_direct(self.servo_center)
            self._set_dc_throttle(0)

            self._initialized = True
            print(f"[MotorController] Initialized at I2C bus {self.i2c_bus}, address 0x{self.pca_address:02X}")
            return True

        except Exception as e:
            print(f"[MotorController] 초기화 오류: {e}")
            return False

    def _i2c_write_with_retry(self, func, *args):
        """I2C 쓰기를 재시도 로직으로 감싸기"""
        for attempt in range(self._i2c_retry_count):
            try:
                func(*args)
                return True
            except (TimeoutError, OSError) as e:
                if attempt < self._i2c_retry_count - 1:
                    time.sleep(self._i2c_retry_delay * (attempt + 1))
                else:
                    print(f"[MotorController] I2C failed after {self._i2c_retry_count} retries: {e}")
                    return False

    def _set_servo_direct(self, angle: float):
        """서보 각도 직접 설정 (레이트 리미팅 + 재시도)"""
        if self._servo_kit is None:
            return
        now = time.monotonic()
        if now - self._last_servo_time < self._min_cmd_interval:
            return  # 너무 빠른 명령 무시
        angle = max(self.servo_min, min(self.servo_max, angle))

        def _write():
            self._servo_kit.servo[self.servo_channel].angle = angle

        if self._i2c_write_with_retry(_write):
            self._last_servo_time = now

    def _set_dc_throttle(self, throttle: float):
        """
        DC 모터 스로틀 설정 (레이트 리미팅 + 재시도)

        Args:
            throttle: -1.0 ~ 1.0 (음수=전진, 양수=후진)
        """
        if self._pca_motor is None:
            return
        now = time.monotonic()
        if now - self._last_motor_time < self._min_cmd_interval:
            print(f"[RATE_LIMITED] throttle={throttle:.2f} skipped!")  # 이 로그 추가
            return

        ch = self.dc_channel
        pulse = int(0xFFFF * abs(throttle))

        def _write():
            if throttle < 0:  # 전진
                self._pca_motor.channels[ch + 5].duty_cycle = pulse
                self._pca_motor.channels[ch + 4].duty_cycle = 0
                self._pca_motor.channels[ch + 3].duty_cycle = 0xFFFF
            elif throttle > 0:  # 후진
                self._pca_motor.channels[ch + 5].duty_cycle = pulse
                self._pca_motor.channels[ch + 4].duty_cycle = 0xFFFF
                self._pca_motor.channels[ch + 3].duty_cycle = 0
            else:  # 정지
                self._pca_motor.channels[ch + 5].duty_cycle = 0
                self._pca_motor.channels[ch + 4].duty_cycle = 0
                self._pca_motor.channels[ch + 3].duty_cycle = 0

        if self._i2c_write_with_retry(_write):
            self._last_motor_time = now

    def set_steering(self, steering_angle: float):
        """
        조향 설정

        Args:
            steering_angle: 조향각 (음수=좌회전, 양수=우회전, 제한 없음)
        """
        if self.state.is_emergency_stop:
            return

        # 조향각을 서보 각도로 변환
        # steering_angle을 서보 각도로 매핑 (60도 기준)
        if steering_angle < 0:
            # 좌회전
            servo_angle = self.servo_center + (steering_angle / 60) * (self.servo_center - self.servo_min)
        else:
            # 우회전
            servo_angle = self.servo_center + (steering_angle / 60) * (self.servo_max - self.servo_center)

        # 서보 각도만 물리적 범위로 제한
        servo_angle = max(self.servo_min, min(self.servo_max, servo_angle))

        with self._lock:
            self.state.steering_angle = steering_angle
            self.state.servo_angle = servo_angle
            self._set_servo_direct(servo_angle)

    def set_speed(self, speed_ratio: float):
        """
        속도 설정

        Args:
            speed_ratio: 0.0 ~ 1.0 (정지 ~ 최대속도)
        """
        if self.state.is_emergency_stop:
            speed_ratio = 0

        speed_ratio = max(0, min(1, speed_ratio))
        throttle = -speed_ratio * self.max_throttle  # 음수가 전진

        with self._lock:
            self.state.speed_ratio = speed_ratio
            self.state.throttle = throttle
            self._set_dc_throttle(throttle)

    def set_control(self, steering_angle: float, speed_ratio: float):
        """
        조향과 속도 동시 설정

        Args:
            steering_angle: -30 ~ +30
            speed_ratio: 0.0 ~ 1.0
        """
        self.set_steering(steering_angle)
        self.set_speed(speed_ratio)

    def emergency_stop(self):
        """긴급 정지 (레이트 리미팅 우회)"""
        with self._lock:
            self.state.is_emergency_stop = True
            # 긴급 정지는 레이트 리미팅 무시
            self._last_motor_time = 0.0
            self._last_servo_time = 0.0
            self._set_dc_throttle(0)
            self._set_servo_direct(self.servo_center)
            self.state.speed_ratio = 0
            self.state.throttle = 0
            self.state.steering_angle = 0
            self.state.servo_angle = self.servo_center

        print("[MotorController] EMERGENCY STOP!")

    def release_emergency(self):
        """긴급 정지 해제"""
        with self._lock:
            self.state.is_emergency_stop = False
        print("[MotorController] Emergency released")

    def stop(self):
        """정지 (긴급 정지 아님)"""
        self.set_speed(0)

    def get_state(self) -> MotorState:
        """현재 상태 반환"""
        with self._lock:
            return MotorState(
                steering_angle=self.state.steering_angle,
                servo_angle=self.state.servo_angle,
                speed_ratio=self.state.speed_ratio,
                throttle=self.state.throttle,
                is_emergency_stop=self.state.is_emergency_stop
            )

    def cleanup(self):
        """리소스 정리"""
        self.emergency_stop()
        time.sleep(0.1)

        if self._pca_servo is not None:
            try:
                self._pca_servo.deinit()
            except:
                pass

        if self._pca_motor is not None:
            try:
                self._pca_motor.deinit()
            except:
                pass

        self._initialized = False
        print("[MotorController] Cleaned up")


class MockMotorController:
    """
    테스트용 모의 모터 컨트롤러

    실제 하드웨어 없이 테스트할 때 사용
    """

    def __init__(self, config):
        self.servo_center = config.servo_center
        self.servo_min = config.servo_min
        self.servo_max = config.servo_max
        self.max_throttle = config.max_throttle
        self.state = MotorState()
        self._initialized = False

    def initialize(self) -> bool:
        self._initialized = True
        print("[MockMotorController] Initialized (simulation mode)")
        return True

    def set_steering(self, steering_angle: float):
        if self.state.is_emergency_stop:
            return

        if steering_angle < 0:
            servo_angle = self.servo_center + (steering_angle / 60) * (self.servo_center - self.servo_min)
        else:
            servo_angle = self.servo_center + (steering_angle / 60) * (self.servo_max - self.servo_center)

        servo_angle = max(self.servo_min, min(self.servo_max, servo_angle))
        self.state.steering_angle = steering_angle
        self.state.servo_angle = servo_angle

    def set_speed(self, speed_ratio: float):
        if self.state.is_emergency_stop:
            speed_ratio = 0
        speed_ratio = max(0, min(1, speed_ratio))
        self.state.speed_ratio = speed_ratio
        self.state.throttle = -speed_ratio * self.max_throttle

    def set_control(self, steering_angle: float, speed_ratio: float):
        self.set_steering(steering_angle)
        self.set_speed(speed_ratio)

    def emergency_stop(self):
        self.state.is_emergency_stop = True
        self.state.speed_ratio = 0
        self.state.throttle = 0
        print("[MockMotorController] EMERGENCY STOP!")

    def release_emergency(self):
        self.state.is_emergency_stop = False

    def stop(self):
        self.set_speed(0)

    def get_state(self) -> MotorState:
        return self.state

    def cleanup(self):
        self.emergency_stop()
        print("[MockMotorController] Cleaned up")


def create_motor_controller(config, use_mock: bool = False):
    """
    모터 컨트롤러 생성

    Args:
        config: MotorConfig
        use_mock: True면 모의 컨트롤러 사용

    Returns:
        MotorController 또는 MockMotorController
    """
    if use_mock:
        return MockMotorController(config)
    return MotorController(config)
