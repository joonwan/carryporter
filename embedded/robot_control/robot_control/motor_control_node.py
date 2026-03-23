#!/usr/bin/env python3
"""
ROS2 Motor Control Node
/cmd_vel 토픽을 구독하여 실제 모터를 제어
"""

import rclpy
from rclpy.node import Node
from geometry_msgs.msg import Twist
from std_msgs.msg import Bool, Float32
import sys
import os
import math

# 모터 컨트롤러 import (ros2_ws 내부)
from robot_control.motor_controller import create_motor_controller
from robot_control.config import MotorConfig


class MotorControlNode(Node):
    """
    ROS2 모터 제어 노드

    토픽:
        - 구독: /cmd_vel (geometry_msgs/Twist) - 속도 명령
        - 구독: /emergency_stop (std_msgs/Bool) - 긴급 정지 신호
        - 발행: /steering_angle (std_msgs/Float32) - 현재 조향각 (rad)
    """

    def __init__(self, use_mock=False):
        super().__init__('motor_control_node')

        # 설정
        self.motor_config = MotorConfig()

        # 서보 범위 오버라이드 (실제 테스트 결과)
        self.motor_config.servo_center = 110   # 실제 중앙
        self.motor_config.servo_min = 50      # 좌회전 최대
        self.motor_config.servo_max = 170     # 우회전 최대

        # 변환 파라미터
        # Twist.linear.x (m/s) → speed_ratio (0~1)
        self.max_linear_speed = 1.0  # 최대 속도 1.0 m/s

        # Twist.angular.z (rad/s) → steering_angle (-30~30)
        self.max_angular_speed = 1.0  # 최대 회전 속도 1.0 rad/s

        # 회전 전용 속도 추적 (teleop에서 설정한 speed 값)
        self.last_forward_speed = 0.5  # 마지막 전진 속도 (초기값 0.5)

        # 모터 컨트롤러 초기화
        self.motor = create_motor_controller(self.motor_config, use_mock=use_mock)
        if not self.motor.initialize():
            self.get_logger().error('모터 초기화 실패!')
            sys.exit(1)

        self.get_logger().info('모터 컨트롤러 초기화 완료')

        # 긴급 정지 상태
        self.emergency_stop = False
        self.last_cmd_vel = None  # 마지막 cmd_vel 저장 (복귀용)

        # /cmd_vel 구독
        self.cmd_vel_sub = self.create_subscription(
            Twist,
            '/cmd_vel',
            self.cmd_vel_callback,
            10
        )

        # /emergency_stop 구독 (YOLO 사람 감지 시 정지)
        self.emergency_stop_sub = self.create_subscription(
            Bool,
            '/emergency_stop',
            self.emergency_stop_callback,
            10
        )

        # /steering_angle 발행 (오도메트리 노드용)
        self.steering_pub = self.create_publisher(Float32, '/steering_angle', 10)

        # 조향각 변환용 상수 (최대 조향각 30도)
        self.max_steering_angle_deg = 30.0
        self.max_steering_angle_rad = math.radians(self.max_steering_angle_deg)

        # 안전 기능: cmd_vel 타임아웃
        self.cmd_vel_timeout = 1.0  # 1초간 메시지 없으면 자동 정지
        self.last_cmd_vel_time = self.get_clock().now()

        # 타임아웃 체크 타이머 (0.1초마다 체크)
        self.watchdog_timer = self.create_timer(0.1, self.watchdog_callback)

        self.get_logger().info('Motor Control Node 시작!')
        self.get_logger().info('  구독: /cmd_vel, /emergency_stop')
        self.get_logger().info('  발행: /steering_angle (오도메트리용)')
        self.get_logger().info(f'  안전: cmd_vel 타임아웃 {self.cmd_vel_timeout}초')

    def emergency_stop_callback(self, msg: Bool):
        """
        긴급 정지 신호 수신

        Args:
            msg: Bool 메시지 (True=정지, False=정상)
        """
        prev_state = self.emergency_stop
        self.emergency_stop = msg.data

        if self.emergency_stop and not prev_state:
            # 정지 활성화
            self.get_logger().warn('🛑 EMERGENCY STOP ACTIVATED - Vehicle stopped')
            # 즉시 정지: throttle 0 + 서보 중앙
            self.motor._set_dc_throttle(0.0)
            self.motor._set_servo_direct(self.motor_config.servo_center)
        elif not self.emergency_stop and prev_state:
            # 정지 해제 - 이전 명령 복귀
            self.get_logger().info('✅ Emergency stop deactivated - Resuming previous command')
            if self.last_cmd_vel is not None:
                # 마지막 cmd_vel을 다시 처리
                self.process_cmd_vel(self.last_cmd_vel)

    def watchdog_callback(self):
        """
        cmd_vel 타임아웃 체크 - 1초간 메시지 없으면 자동 정지
        """
        time_since_last_cmd = (self.get_clock().now() - self.last_cmd_vel_time).nanoseconds / 1e9

        if time_since_last_cmd > self.cmd_vel_timeout:
            # 타임아웃 - 모터 정지
            self.motor._set_dc_throttle(0.0)
            self.motor._set_servo_direct(self.motor_config.servo_center)

    def cmd_vel_callback(self, msg: Twist):
        """
        /cmd_vel 토픽 콜백

        Args:
            msg: Twist 메시지
                - linear.x: 전진/후진 속도 (m/s)
                - angular.z: 회전 속도 (rad/s)
        """
        # 타임스탬프 업데이트 (watchdog용 - 비활성화됨)
        self.last_cmd_vel_time = self.get_clock().now()

        # 마지막 cmd_vel 저장 (emergency_stop 해제 시 복귀용)
        self.last_cmd_vel = msg

        # 긴급 정지 중이면 모든 명령 무시하고 정지 유지
        if self.emergency_stop:
            self.motor._set_dc_throttle(0.0)
            self.motor._set_servo_direct(self.motor_config.servo_center)
            return

        # 실제 처리 (I2C 에러 시 노드 크래시 방지)
        try:
            self.process_cmd_vel(msg)
        except (TimeoutError, OSError) as e:
            self.get_logger().warn(f'I2C 통신 오류 (자동 복구): {e}')

    def process_cmd_vel(self, msg: Twist):
        """
        cmd_vel을 실제로 처리하여 모터 제어

        Args:
            msg: Twist 메시지
        """
        # Twist → 모터 명령 변환
        linear_x = msg.linear.x   # m/s
        angular_z = msg.angular.z  # rad/s

        # 1. 속도 변환: linear.x → speed_ratio (0~1), 방향 보존
        speed_ratio = abs(linear_x) / self.max_linear_speed
        speed_ratio = max(0.0, min(1.0, speed_ratio))  # 0~1 제한
        is_reverse = linear_x < 0  # 후진 여부
        
        # 전진/후진 시 속도 기록 (회전 전용 주행 시 사용)
        if abs(linear_x) > 0.01:
            self.last_forward_speed = speed_ratio

        # 2. 조향 변환: angular.z → steering_angle (-60~60)
        # ⚠️ 주의: 실제 테스트 결과 좌우가 반대였음
        # angular.z가 양수(+)일 때 좌회전(-), 음수(-)일 때 우회전(+)
        steering_angle = -(angular_z / self.max_angular_speed) * 60.0
        steering_angle = max(-60.0, min(60.0, steering_angle))  # -60~60 제한

        # 조향 오프셋 적용 (하드웨어 중앙값 보정)
        steering_angle += self.motor_config.steering_offset
        steering_angle = max(-60.0, min(60.0, steering_angle))  # 오프셋 적용 후 재제한

        # 3. 회전만 할 때 (j, l만) 마지막 설정 속도 사용
        rotation_only = abs(angular_z) > 0.01 and abs(linear_x) < 0.01
        if rotation_only:
            speed_ratio = self.last_forward_speed  # 설정한 speed와 동일

        # 4. 모터 제어
        # steering_angle을 직접 servo_angle로 변환 (전체 범위 사용)
        # steering_angle: -60 (좌) ~ 0 (중앙) ~ +60 (우)
        # servo_angle: 0 (좌 최대) ~ 60 (중앙) ~ 180 (우 최대)

        if steering_angle < 0:  # 좌회전
            # -60 → 0도, 0 → 60도
            left_range = self.motor_config.servo_center - self.motor_config.servo_min  # 60
            servo_angle = self.motor_config.servo_center + (steering_angle / 60.0) * left_range
        else:  # 우회전
            # 0 → 60도, +60 → 180도
            right_range = self.motor_config.servo_max - self.motor_config.servo_center  # 120
            servo_angle = self.motor_config.servo_center + (steering_angle / 60.0) * right_range

        # 서보 각도 직접 설정 (범위 제한)
        servo_angle = max(self.motor_config.servo_min,
                          min(self.motor_config.servo_max, servo_angle))

        # 모터 제어
        self.motor._set_servo_direct(servo_angle)

        # 조향각 발행 (오도메트리용, 라디안)
        # steering_angle: -60~+60 범위를 실제 물리적 조향각으로 변환
        # 내부 60도 = 실제 30도 (최대 조향각)
        actual_steering_rad = (steering_angle / 60.0) * self.max_steering_angle_rad
        steering_msg = Float32()
        steering_msg.data = actual_steering_rad
        self.steering_pub.publish(steering_msg)

        # 전진/후진 처리
        if is_reverse:
            # 후진: throttle을 양수로 설정
            throttle = speed_ratio * self.motor_config.max_throttle
        else:
            # 전진: throttle을 음수로 설정
            throttle = -speed_ratio * self.motor_config.max_throttle

        # throttle 안전 범위 제한 (-1.0 ~ 1.0)
        throttle = max(-1.0, min(1.0, throttle))

        self.motor._set_dc_throttle(throttle)

        # 디버그 로그 (회전 시만)
        if rotation_only:
            self.get_logger().info(
                f'[ROTATION] speed_ratio={speed_ratio:.2f}, throttle={throttle:.2f}, '
                f'servo_angle={servo_angle:.1f}, steering_angle={steering_angle:.1f}'
            )

    def destroy_node(self):
        """노드 종료 시 정리"""
        self.get_logger().info('Motor Control Node 종료 중...')
        # 안전: 모터 즉시 정지 (레이트 리미팅 우회)
        try:
            self.motor._last_motor_time = 0.0  # 레이트 리미팅 우회
            self.motor._last_servo_time = 0.0
            self.motor._set_dc_throttle(0.0)
            self.motor._set_servo_direct(self.motor_config.servo_center)
            self.get_logger().info('모터 정지 완료')
        except Exception as e:
            self.get_logger().error(f'모터 정지 실패: {e}')
        finally:
            self.motor.cleanup()
        super().destroy_node()


def main(args=None):
    """메인 함수"""
    rclpy.init(args=args)

    # 인자 파싱
    use_mock = '--mock' in sys.argv

    if use_mock:
        print('🤖 Mock 모드: 실제 모터 없이 시뮬레이션')
    else:
        print('⚙️  실제 모터 제어 모드')

    try:
        node = MotorControlNode(use_mock=use_mock)
        rclpy.spin(node)
    except KeyboardInterrupt:
        print('\n사용자 종료')
    except Exception as e:
        print(f'오류 발생: {e}')
        import traceback
        traceback.print_exc()
    finally:
        if rclpy.ok():
            node.destroy_node()
            rclpy.shutdown()


if __name__ == '__main__':
    main()
