#!/usr/bin/env python3
"""
안전 제어 노드 (YOLO + LiDAR 융합)
- 사람 감지 시 LiDAR로 거리 측정
- 1m 이내일 때만 긴급 정지
- TF 보정 적용 (-90도 회전)
- Hysteresis로 깜빡임 방지
- 긴급 정지 시 LED 깜빡임 경고
"""
import rclpy
from rclpy.node import Node
from rclpy.qos import QoSProfile, ReliabilityPolicy, HistoryPolicy
from std_msgs.msg import Bool
from sensor_msgs.msg import LaserScan
import math
import threading

# --- LED GPIO 설정 (Jetson 전용) ---
LED_PIN = 15  # BOARD 모드 기준 15번 핀
GPIO = None

try:
    import Jetson.GPIO as GPIO
    GPIO.setwarnings(False)
    GPIO.setmode(GPIO.BOARD)
    try:
        GPIO.cleanup(LED_PIN)
    except:
        pass
    GPIO.setup(LED_PIN, GPIO.OUT, initial=GPIO.LOW)
    print("[GPIO] LED 핀 초기화 완료 (핀 15)")
except ImportError:
    print("[GPIO] Jetson.GPIO 모듈 없음 - LED 시뮬레이션 모드")
except Exception as e:
    print(f"[GPIO] LED 초기화 실패: {e}")
    GPIO = None

class SafetyControllerNode(Node):
    def __init__(self):
        super().__init__('safety_controller_node')

        # 파라미터
        self.declare_parameter('stop_on_person_detected', True)
        self.declare_parameter('safety_distance', 1.0)  # 긴급정지 거리
        self.declare_parameter('recovery_distance', 1.2)  # 복귀 거리 (hysteresis)
        self.declare_parameter('front_angle_range', 10.0)  # 정면 범위 (±도)
        self.declare_parameter('tf_rotation_offset', -90.0)  # TF 회전 보정

        self.stop_enabled = self.get_parameter('stop_on_person_detected').value
        self.safety_distance = self.get_parameter('safety_distance').value
        self.recovery_distance = self.get_parameter('recovery_distance').value
        self.front_angle_range = self.get_parameter('front_angle_range').value
        self.tf_offset = self.get_parameter('tf_rotation_offset').value

        # 상태
        self.person_detected = False
        self.front_distance = float('inf')
        self.emergency_stop = False  # 현재 긴급정지 상태
        self.lidar_received = False
        self._clear_start_time = None  # 미감지 지속 시작 시각
        self._clear_delay = 1.0  # 미감지 후 출발까지 대기 시간 (초)

        # LED 깜빡임 제어
        self._led_blink_active = False
        self._led_blink_thread = None

        # 퍼블리셔: 긴급 정지 신호
        self.stop_signal_pub = self.create_publisher(Bool, '/emergency_stop', 10)

        # 구독: 사람 감지 여부
        self.person_sub = self.create_subscription(
            Bool,
            '/person_detected',
            self.person_detected_callback,
            10
        )

        # 구독: LiDAR 스캔 데이터 (Best Effort QoS)
        qos_profile = QoSProfile(
            reliability=ReliabilityPolicy.BEST_EFFORT,
            history=HistoryPolicy.KEEP_LAST,
            depth=10
        )
        self.scan_sub = self.create_subscription(
            LaserScan,
            '/scan',
            self.scan_callback,
            qos_profile
        )

        # 1초마다 상태 로그 출력
        self.create_timer(1.0, self.log_status)

        self.get_logger().info('='*60)
        self.get_logger().info(f'Safety Controller started (YOLO + LiDAR Fusion)')
        self.get_logger().info(f'Emergency stop: {self.safety_distance}m, Recovery: {self.recovery_distance}m')
        self.get_logger().info(f'Front angle range: ±{self.front_angle_range}° (base_link)')
        self.get_logger().info(f'TF rotation offset: {self.tf_offset}°')
        self.get_logger().info('Subscribed to: /person_detected, /scan')
        self.get_logger().info('Publishing to: /emergency_stop')
        self.get_logger().info(f'LED warning: pin {LED_PIN} ({"GPIO active" if GPIO else "simulation"})')
        self.get_logger().info('='*60)

    def scan_callback(self, msg: LaserScan):
        """LiDAR 데이터 수신 - 정면 거리 측정 (TF 보정 적용)"""
        if not self.lidar_received:
            self.get_logger().info('✅ LiDAR data received!')
            self.lidar_received = True

        total_points = len(msg.ranges)
        center_index = total_points // 2

        # base_link 정면 ±10도를 laser_frame 각도로 변환
        front_angle_min_base = -self.front_angle_range
        front_angle_max_base = +self.front_angle_range

        # base_link → laser_frame 변환
        front_angle_min_laser = front_angle_min_base - self.tf_offset
        front_angle_max_laser = front_angle_max_base - self.tf_offset

        # laser_frame 각도 → 인덱스 변환
        index_min = center_index + int(front_angle_min_laser * total_points / 360.0)
        index_max = center_index + int(front_angle_max_laser * total_points / 360.0)

        # 인덱스 범위 제한
        index_min = max(0, min(index_min, total_points - 1))
        index_max = max(0, min(index_max, total_points - 1))

        if index_min > index_max:
            index_min, index_max = index_max, index_min

        # 정면 범위에서 유효한 거리 중 최소값 찾기
        front_ranges = []
        for i in range(index_min, index_max + 1):
            r = msg.ranges[i]
            if not math.isinf(r) and not math.isnan(r) and 0.1 <= r <= 10.0:
                front_ranges.append(r)

        # 유효한 거리가 있으면 최소값, 없으면 충분히 먼 거리
        if front_ranges:
            self.front_distance = min(front_ranges)
        else:
            self.front_distance = 10.0

        # ✅ LiDAR 거리 업데이트 후 즉시 긴급정지 재평가 (사람 감지 중일 때)
        if self.person_detected:
            self._evaluate_emergency_stop()

    def person_detected_callback(self, msg: Bool):
        """사람 감지 여부 업데이트 및 긴급정지 재평가"""
        self.person_detected = msg.data

        if not self.stop_enabled:
            return

        # 긴급정지 재평가
        self._evaluate_emergency_stop()

    def _evaluate_emergency_stop(self):
        """긴급정지 조건 평가 및 신호 발행 (Hysteresis 적용)"""
        if not self.stop_enabled:
            return

        # Hysteresis 적용
        prev_stop = self.emergency_stop

        if self.emergency_stop:
            # 긴급정지 상태 → 복귀: recovery_distance 이상이거나 사람 미감지
            if not self.person_detected or self.front_distance >= self.recovery_distance:
                # 미감지 지속 시간 체크 (1초 이상 연속 미감지 시에만 해제)
                import time
                if self._clear_start_time is None:
                    self._clear_start_time = time.time()
                    self.get_logger().info(
                        f'⏳ 미감지 시작, {self._clear_delay}초 대기 중...'
                    )
                    return  # 아직 해제하지 않음
                elif time.time() - self._clear_start_time < self._clear_delay:
                    return  # 아직 대기 시간이 안 지남
                # 대기 시간 경과 → 해제
                self._clear_start_time = None
                self.emergency_stop = False
                self.get_logger().info(
                    f'✅ Normal operation resumed (distance: {self.front_distance:.2f}m)'
                )
            else:
                # 다시 감지되면 타이머 리셋
                self._clear_start_time = None
        else:
            # 정상 상태 → 긴급정지: 사람 감지 AND safety_distance 미만
            if self.person_detected and self.front_distance < self.safety_distance:
                self.emergency_stop = True
                self.get_logger().warn(
                    f'🛑 EMERGENCY STOP - Person at {self.front_distance:.2f}m'
                )

        # 긴급 정지 신호 발행
        stop_msg = Bool()
        stop_msg.data = self.emergency_stop
        self.stop_signal_pub.publish(stop_msg)

        # LED 깜빡임 제어: 상태 전환 시에만 시작/중지
        if self.emergency_stop and not prev_stop:
            self._start_led_blink()
        elif not self.emergency_stop and prev_stop:
            self._stop_led_blink()

    # --- LED 깜빡임 제어 ---
    def _start_led_blink(self):
        """LED 깜빡임 시작 (별도 스레드)"""
        if self._led_blink_active:
            return
        self._led_blink_active = True
        self._led_blink_thread = threading.Thread(target=self._led_blink_loop, daemon=True)
        self._led_blink_thread.start()
        self.get_logger().info('💡 LED 깜빡임 시작')

    def _stop_led_blink(self):
        """LED 깜빡임 중지 + LED OFF"""
        if not self._led_blink_active:
            return
        self._led_blink_active = False
        if self._led_blink_thread is not None:
            self._led_blink_thread.join(timeout=1.0)
            self._led_blink_thread = None
        # LED 확실히 끄기
        if GPIO is not None:
            GPIO.output(LED_PIN, GPIO.LOW)
        self.get_logger().info('💡 LED 꺼짐')

    def _led_blink_loop(self):
        """LED 깜빡임 루프 (0.3초 간격)"""
        import time
        while self._led_blink_active:
            if GPIO is not None:
                GPIO.output(LED_PIN, GPIO.HIGH)
            time.sleep(0.3)
            if not self._led_blink_active:
                break
            if GPIO is not None:
                GPIO.output(LED_PIN, GPIO.LOW)
            time.sleep(0.3)

    def destroy_node(self):
        """노드 종료 시 LED 끄고 GPIO 정리"""
        self._stop_led_blink()
        if GPIO is not None:
            try:
                GPIO.cleanup(LED_PIN)
            except:
                pass
        super().destroy_node()

    def log_status(self):
        """1초마다 현재 상태 로그 출력 (긴급 정지 상태일 때만)"""
        # 긴급 정지 상태일 때만 로그 출력
        if self.emergency_stop:
            self.get_logger().warn(
                f'🛑 EMERGENCY STOP | Person detected at {self.front_distance:.2f}m'
            )
        # 정상 상태에서는 로그 생략

def main(args=None):
    rclpy.init(args=args)
    node = SafetyControllerNode()
    try:
        rclpy.spin(node)
    except KeyboardInterrupt:
        pass
    finally:
        node.destroy_node()
        rclpy.shutdown()

if __name__ == '__main__':
    main()
