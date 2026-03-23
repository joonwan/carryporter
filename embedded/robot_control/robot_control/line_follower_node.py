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
VAL_TURN_ANGULAR = 0.85    # 회전 강도
VAL_TURN_SPEED = 0.60      # 회전 시 전진 속도
BOOST_SPEED = 0.45          # 출발 부스트 속도
BOOST_TIME  = 0.2          # 출발 부스트 시간


class LineFollowerNode(Node):
    def __init__(self, target_color=None):
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
        self.last_action = 'CENTER' # 기억력 (CENTER, LEFT, RIGHT)
        self.is_completed = False  # 완료 플래그
        
        # 5. 타이머 (20Hz)
        self.timer = self.create_timer(0.05, self.timer_callback)
        
        self.get_logger().info("🚀 라인트레이서 시작 (Red Stop Mode)")

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
            
            # 너무 어두우면 판별 생략 (총 밝기 기준)
            if r < 50 and g < 40 and b < 25:
                return None

            # Red 감지 로직: R이 충분히 밝고(50 이상), G와 B보다 1.5배 이상 클 때
            elif r > 70:
                return "RED"

            # Green 감지 로직
            elif g > 40:
                return "GREEN"

            elif b > 25:
                return "BLUE"
                
            return None
        except Exception as e:
            # I2C 읽기 에러 시 무시
            return None

    def timer_callback(self):
        msg = Twist()
        
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
        except:
            return

        # --- [3. 컬러 센서 읽기] ---
        current_color = self.detect_color()

        # --- [4. 주행 로직] ---

        # [우선순위 1] Target 색 감지 시 정지 (교차로 상태 L, R과 무관하게 멈춤)
        if self.target_color and current_color == self.target_color and not self.is_completed:
            msg.linear.x = 0.0
            msg.angular.z = 0.0
            self.get_logger().info(f"🎯 {current_color} Detected! - Target Reached, Stopping")

            # 완료 상태 발행
            status_msg = String()
            status_msg.data = "completed"
            self.status_pub.publish(status_msg)
            self.get_logger().info("📡 완료 상태 발행: /line_trace/status 'completed'")

            # 완료 플래그 설정
            self.is_completed = True

            # 1초 후 자동 종료를 위한 타이머 생성
            self.create_timer(1.0, self.shutdown_callback)
        

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

        # [우선순위 3] 상황 B: 왼쪽 밟음 (1, 0) -> 좌회전
        elif L == 1 and R == 0:
            msg.linear.x = VAL_TURN_SPEED
            msg.angular.z = VAL_TURN_ANGULAR
            self.last_action = 'LEFT'

        # [우선순위 4] 상황 C: 오른쪽 밟음 (0, 1) -> 우회전
        elif L == 0 and R == 1:
            msg.linear.x = VAL_TURN_SPEED
            msg.angular.z = -VAL_TURN_ANGULAR
            self.last_action = 'RIGHT'

        # [우선순위 5] 상황 D: 둘 다 밟음 (1, 1)
        elif L == 1 and R == 1:
            msg.linear.x = 0.0
            msg.angular.z = 0.0
            self.get_logger().info("return area, Stopping")

            # return 모드 (target_color=None): 복귀 완료 → completed 발행
            if self.target_color is None and not self.is_completed:
                status_msg = String()
                status_msg.data = "completed"
                self.status_pub.publish(status_msg)
                self.get_logger().info("완료 상태 발행: /line_trace/status 'completed'")
                self.is_completed = True
                self.create_timer(1.0, self.shutdown_callback)

        # 명령 발행
        self.cmd_vel_pub.publish(msg)

    def destroy_node(self):
        stop_msg = Twist()
        self.cmd_vel_pub.publish(stop_msg)
        GPIO.cleanup()
        super().destroy_node()

def main(args=None):
    # 커맨드 라인 인자 파싱
    import argparse
    import sys

    parser = argparse.ArgumentParser(description='Line Follower Node with Color Detection')
    parser.add_argument('--target-color', type=str, default=None,
                        help='Target color to stop at (RED, GREEN, BLUE)')

    # ROS2 인자와 분리
    parsed_args, ros_args = parser.parse_known_args()

    rclpy.init(args=ros_args)
    node = LineFollowerNode(target_color=parsed_args.target_color)

    try:
        rclpy.spin(node)
    except KeyboardInterrupt:
        pass
    finally:
        node.destroy_node()
        rclpy.shutdown()

if __name__ == '__main__':
    main()
