#!/usr/bin/env python3
"""
ROS2 Line Follower Node with Color Stop
기능: 기본 라인트레이싱 + 빨간색(RED) 감지 시 정지
"""

import rclpy
from rclpy.node import Node
from geometry_msgs.msg import Twist
from std_msgs.msg import String
import time
import os

# === [I2C 및 컬러 센서 라이브러리 - GPIO보다 먼저 import] ===
os.environ['BLINKA_I2C_BUS'] = '7'  # TCS34725가 연결된 I2C 버스 번호
import board
import busio
import adafruit_tcs34725

import Jetson.GPIO as GPIO

# === [설정] ===
# 센서 핀 (Board 번호)
PIN_L = 12
PIN_R = 13

# ==========================================================
# ★ [키보드 시뮬레이션 설정]
# ==========================================================
VAL_I_SPEED = 0.45          # 직진 속도
VAL_TURN_ANGULAR = 0.80    # 회전 강도
VAL_TURN_SPEED = 0.75      # 회전 시 전진 속도
BOOST_SPEED = 0.45          # 출발 부스트 속도
BOOST_TIME  = 0.2          # 출발 부스트 시간


class LineFollowerNode(Node):
    def __init__(self, target_color="GREEN", initial_direction='CENTER'):
        super().__init__('line_follower_node')
        
        # 1. Publisher
        self.cmd_vel_pub = self.create_publisher(Twist, '/cmd_vel', 10)

        # 1-1. 상태 발행 (완료 알림)
        self.status_pub = self.create_publisher(String, '/line_trace/status', 10)
        
        # 2. I2C 및 컬러 센서(TCS34725) 초기화 (GPIO보다 먼저 - Blinka 충돌 방지)
        try:
            self.i2c = busio.I2C(board.SCL, board.SDA)
            self.color_sensor = adafruit_tcs34725.TCS34725(self.i2c)
            self.color_sensor.integration_time = 50  # 50ms (반응 속도 조절)
            self.color_sensor.gain = 4               # 감도 조절 (1, 4, 16, 60)
            self.get_logger().info("Color Sensor (TCS34725) Connected on I2C bus 7!")
        except Exception as e:
            self.get_logger().error(f"Color Sensor Connection Failed: {e}")
            self.color_sensor = None

        # 3. GPIO 설정 (Blinka가 설정한 GPIO 모드를 초기화 후 BOARD 모드로 재설정)
        GPIO.cleanup()
        GPIO.setmode(GPIO.BOARD)
        GPIO.setup(PIN_L, GPIO.IN)
        GPIO.setup(PIN_R, GPIO.IN)

        # 목표 색상 설정 (매개변수로 받음)
        self.target_color = target_color
        if self.target_color:
            self.get_logger().info(f"🎯 목표 색상: {self.target_color}")

        # 4. 상태 변수
        self.start_time = time.time()
        self.is_booting = True
        self.last_action = initial_direction  # 기억력 (CENTER, LEFT, RIGHT)
        self.is_completed = False  # 완료 플래그
        self._last_color_read_time = 0.0  # 컬러 센서 읽기 throttle
        self._last_color_result = None    # 마지막 컬러 센서 결과 캐시

        # 5. 타이머 (20Hz)
        self.timer = self.create_timer(0.05, self.timer_callback)
        
        self.get_logger().info("🚀 라인트레이서 시작 (Red Stop Mode)")

        # 🔥 2. 토픽 연결 대기 (motor_control_node가 구독할 때까지)
        self.get_logger().info("⏳ /cmd_vel 토픽 연결 대기 중...")
        connection_timeout = 5.0  # 최대 5초 대기
        start_wait = time.time()

        while (time.time() - start_wait) < connection_timeout:
            subscriber_count = self.cmd_vel_pub.get_subscription_count()
            if subscriber_count > 0:
                self.get_logger().info(f"✅ /cmd_vel 연결 완료! (구독자 {subscriber_count}개)")
                break
            time.sleep(0.1)
        else:
            self.get_logger().warn(f"⚠️ /cmd_vel 구독자를 찾지 못했습니다! (타임아웃)")

        # 3. 추가 안정화 대기
        time.sleep(0.5)

    def shutdown_callback(self):
        """목표 도착 후 자동 종료"""
        self.get_logger().info("✅ 목표 도착 완료, 종료합니다")
        raise SystemExit  # rclpy.spin()을 빠져나가기 위해

    def detect_color(self):
        """
        컬러 센서 값을 읽어 'RED', 'GREEN' 또는 None을 반환
        """
        if self.color_sensor is None:
            return None

        try:
            r, g, b = self.color_sensor.color_rgb_bytes

            # RGB 값 실시간 로깅 (0.5초마다)
            if not hasattr(self, '_last_color_log_time'):
                self._last_color_log_time = time.time()
            if time.time() - self._last_color_log_time > 0.5:
                self.get_logger().info(f"🎨 RGB 값: R={r}, G={g}, B={b}")
                self._last_color_log_time = time.time()

            detected_color = None

            # 너무 어두우면 판별 생략 (총 밝기 기준)
            if r < 70 and g < 30 and b < 25:
                detected_color = None
            # Red 감지 로직: R이 충분히 밝고(70 이상), G와 B보다 클 때
            elif r > 70 and r > (b*20) and r > (g*20):
                detected_color = "RED"
            # Green 감지 로직
            elif g > 30 and g > b and g > r:
                detected_color = "GREEN"
            # Blue 감지 로직
            elif b > 25 and b > r and b > g:
                detected_color = "BLUE"
            else:
                detected_color = None

            # 색상 감지 시 로그 출력 (상태 변경 시에만)
            if not hasattr(self, '_last_detected_color'):
                self._last_detected_color = None

            if detected_color != self._last_detected_color:
                if detected_color:
                    self.get_logger().info(f"🎯 색상 감지됨: {detected_color} (R={r}, G={g}, B={b})")
                else:
                    if self._last_detected_color:
                        self.get_logger().info(f"색상 감지 종료: {self._last_detected_color} → None (R={r}, G={g}, B={b})")
                self._last_detected_color = detected_color

            return detected_color

        except Exception as e:
            # I2C 읽기 에러 시 로그
            self.get_logger().warn(f"컬러 센서 읽기 에러: {e}")
            return None

    def timer_callback(self):
        msg = Twist()

        # --- [0. 완료 상태면 정지만 발행] ---
        if self.is_completed:
            msg.linear.x = 0.0
            msg.angular.z = 0.0
            self.cmd_vel_pub.publish(msg)
            return

        # --- [1. 킥 스타트] ---
        if self.is_booting:
            if time.time() - self.start_time < BOOST_TIME:
                msg.linear.x = BOOST_SPEED
                msg.angular.z = 0.0
                self.cmd_vel_pub.publish(msg)
                return
            else:
                self.is_booting = False

        # --- [2. 라인 센서 읽기] ---
        try:
            L = GPIO.input(PIN_L)
            R = GPIO.input(PIN_R)
            # 센서 값 디버깅 로그 (0.5초마다 출력)
            if not hasattr(self, '_last_log_time'):
                self._last_log_time = time.time()
            if time.time() - self._last_log_time > 0.5:
                self.get_logger().info(f"센서 값 - L(12번): {L}, R(13번): {R}")
                self._last_log_time = time.time()
        except Exception as e:
            self.get_logger().error(f"센서 읽기 실패: {e}")
            return

        # --- [3. 컬러 센서 읽기 (500ms 간격 throttle - I2C 버스 충돌 방지)] ---
        now = time.time()
        if now - self._last_color_read_time >= 0.5:
            self._last_color_result = self.detect_color()
            self._last_color_read_time = now
        current_color = self._last_color_result

        # --- [4. 주행 로직] ---

        # [우선순위 1] Target 색 감지 시 정지 (교차로 상태 L, R과 무관하게 멈춤)
        # return 모드(target_color=None)에서는 GREEN 감지 시 정지
        if self.target_color is None and current_color == "GREEN" and not self.is_completed:
            msg.linear.x = 0.0
            msg.angular.z = 0.0
            self.get_logger().info("🟢 GREEN Detected! - Return 완료, Stopping")
            status_msg = String()
            status_msg.data = "completed"

            print("=" * 60)
            print("🚀 [MQTT 전송 시작] 서버로 복귀 완료 메시지를 전송합니다...")
            print(f"   - 토픽: /line_trace/status")
            print(f"   - 데이터: {status_msg.data}")
            print(f"   - 모드: RETURN (복귀 모드)")
            print(f"   - 감지된 색상: {current_color}")
            print("=" * 60)

            self.status_pub.publish(status_msg)

            print("=" * 60)
            print("✅ [MQTT 전송 완료] 복귀 완료 메시지가 성공적으로 전송되었습니다!")
            print(f"   - 전송 시각: {time.strftime('%Y-%m-%d %H:%M:%S')}")
            print("=" * 60)

            self.get_logger().info("📡 완료 상태 발행: /line_trace/status 'completed'")
            self.is_completed = True
            self.create_timer(0.2, self.shutdown_callback)

        elif self.target_color and current_color == self.target_color and not self.is_completed:
            msg.linear.x = 0.0
            msg.angular.z = 0.0
            self.get_logger().info(f"🎯 {current_color} Detected! - Target Reached, Stopping")

            # 완료 상태 발행
            status_msg = String()
            status_msg.data = "completed"

            # 메시지 전송 전 로그
            print("=" * 60)
            print("🚀 [MQTT 전송 시작] 서버로 완료 메시지를 전송합니다...")
            print(f"   - 토픽: /line_trace/status")
            print(f"   - 데이터: {status_msg.data}")
            print(f"   - 감지된 색상: {current_color}")
            print("=" * 60)

            self.status_pub.publish(status_msg)

            # 메시지 전송 후 로그
            print("=" * 60)
            print("✅ [MQTT 전송 완료] 완료 메시지가 성공적으로 전송되었습니다!")
            print(f"   - 전송 시각: {time.strftime('%Y-%m-%d %H:%M:%S')}")
            print("=" * 60)

            self.get_logger().info("📡 완료 상태 발행: /line_trace/status 'completed'")

            # 완료 플래그 설정
            self.is_completed = True

            # 1초 후 자동 종료를 위한 타이머 생성
            self.create_timer(0.2, self.shutdown_callback)
        

        # [우선순위 2] 상황 A: 바닥만 보임 (0, 0) -> 직진 or 이전 상태 유지
        elif L == 0 and R == 0:
            if self.last_action == 'LEFT':
                msg.linear.x = VAL_TURN_SPEED
                msg.angular.z = VAL_TURN_ANGULAR
            elif self.last_action == 'RIGHT':
                msg.linear.x = VAL_TURN_SPEED
                msg.angular.z = -VAL_TURN_ANGULAR
            else:
                msg.linear.x = VAL_I_SPEED
                msg.angular.z = 0.0
                self.last_action = 'CENTER'
            # 바닥만 보임 로그 (2초마다)
            if not hasattr(self, '_last_floor_log'):
                self._last_floor_log = 0
            if time.time() - self._last_floor_log > 2.0:
                self.get_logger().info(f"➡️ 바닥만 보임 (L=0, R=0) - 이전 동작: {self.last_action}")
                self._last_floor_log = time.time()

        # [우선순위 3] 상황 B: 왼쪽 밟음 (1, 0) -> 좌회전
        elif L == 1 and R == 0:
            msg.linear.x = VAL_TURN_SPEED
            msg.angular.z = VAL_TURN_ANGULAR
            self.last_action = 'LEFT'
            # 좌회전 감지 로그
            if not hasattr(self, '_last_left_log'):
                self._last_left_log = 0
            if time.time() - self._last_left_log > 1.0:
                self.get_logger().info("🔄 좌회전 감지 - L=1, R=0")
                self._last_left_log = time.time()

        # [우선순위 4] 상황 C: 오른쪽 밟음 (0, 1) -> 우회전
        elif L == 0 and R == 1:
            msg.linear.x = VAL_TURN_SPEED
            msg.angular.z = -VAL_TURN_ANGULAR  
            self.last_action = 'RIGHT'
            # 우회전 감지 로그
            if not hasattr(self, '_last_right_log'):
                self._last_right_log = 0
            if time.time() - self._last_right_log > 1.0:
                self.get_logger().info("🔄 우회전 감지 - L=0, R=1")
                self._last_right_log = time.time()

        # [우선순위 6] 상황 D: 둘 다 밟음 (1, 1) → 직진 통과
        elif L == 1 and R == 1:
            msg.linear.x = VAL_I_SPEED
            msg.angular.z = 0.0
            if not hasattr(self, '_last_both_log'):
                self._last_both_log = 0
            if time.time() - self._last_both_log > 1.0:
                self.get_logger().info("➡️ 둘 다 밟음 (L=1, R=1) - 직진 통과")
                self._last_both_log = time.time()

        # 명령 발행
        self.cmd_vel_pub.publish(msg)

    def destroy_node(self):
        stop_msg = Twist()
        self.cmd_vel_pub.publish(stop_msg)
        # I2C 버스 7 해제 (motor_control_node와의 충돌 방지)
        try:
            if self.color_sensor is not None:
                self.color_sensor = None
            if hasattr(self, 'i2c') and self.i2c is not None:
                self.i2c.deinit()
                self.i2c = None
        except Exception:
            pass
        GPIO.cleanup()
        super().destroy_node()

def main(args=None):
    # 커맨드 라인 인자 파싱
    import argparse
    import sys

    parser = argparse.ArgumentParser(description='Line Follower Node with Color Detection')
    parser.add_argument('--target-color', type=str, default=None,
                        help='Target color to stop at (RED, GREEN, BLUE)')
    parser.add_argument('--initial-direction', type=str, default='CENTER',
                        choices=['CENTER', 'LEFT', 'RIGHT'],
                        help='Initial direction when both sensors see floor (CENTER, LEFT, RIGHT)')

    # ROS2 인자와 분리
    parsed_args, ros_args = parser.parse_known_args()

    print(f"[line_follower_node] 실행 시작 (target_color={parsed_args.target_color}, initial_direction={parsed_args.initial_direction})")

    rclpy.init(args=ros_args)
    node = LineFollowerNode(target_color=parsed_args.target_color, initial_direction=parsed_args.initial_direction)

    try:
        rclpy.spin(node)
    except KeyboardInterrupt:
        pass
    finally:
        node.destroy_node()
        rclpy.shutdown()

if __name__ == '__main__':
    main()