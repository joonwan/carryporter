#!/usr/bin/env python3
"""
모션 자가 테스트 스크립트
- odometry_node + precise_motion_node를 자동 실행
- ROS2 /motion_command 토픽으로 실제 로봇 이동 테스트
- 백엔드 없이 이 파일 하나만 실행하면 됨

사용법:
  source ~/ros2_ws/install/setup.bash
  python3 test_motion.py
"""

import subprocess
import signal
import sys
import time
import math
import threading
import atexit

import rclpy
from rclpy.node import Node
from std_msgs.msg import String
from nav_msgs.msg import Odometry


# 백그라운드 ROS2 노드 프로세스 관리
bg_processes = []

def cleanup():
    """종료 시 백그라운드 노드 정리"""
    for p in bg_processes:
        try:
            p.terminate()
            p.wait(timeout=3)
        except:
            p.kill()
    print("[정리] 백그라운드 노드 종료 완료")

atexit.register(cleanup)


def start_background_nodes():
    """odometry_node, precise_motion_node를 백그라운드로 실행"""
    nodes = [
        ["ros2", "run", "time_based_control", "odometry_node"],
        ["ros2", "run", "time_based_control", "precise_motion_node"],
    ]

    for cmd in nodes:
        name = cmd[-1]
        print(f"[시작] {name} 실행 중...")
        p = subprocess.Popen(
            cmd,
            stdout=subprocess.DEVNULL,
            stderr=subprocess.DEVNULL,
        )
        bg_processes.append(p)

    # 노드 초기화 대기
    print("[대기] 노드 초기화 중 (3초)...")
    time.sleep(3)
    print("[완료] 백그라운드 노드 준비 완료\n")


class MotionTestNode(Node):
    def __init__(self):
        super().__init__('motion_test_node')

        # 이동 거리 설정
        self.move_distance = 0.5  # 50cm

        # 상태
        self.state = "IDLE"
        self.motion_completed = threading.Event()
        self.last_status = ""

        # 오도메트리
        self.current_x = 0.0
        self.current_y = 0.0
        self.current_theta = 0.0
        self.odom_received = False

        # 발행자
        self.motion_cmd_pub = self.create_publisher(String, '/motion_command', 10)

        # 구독자
        self.motion_status_sub = self.create_subscription(
            String, '/motion_status', self.motion_status_callback, 10
        )
        self.odom_sub = self.create_subscription(
            Odometry, '/odom', self.odom_callback, 10
        )

        self.get_logger().info('모션 테스트 노드 시작')

    def motion_status_callback(self, msg: String):
        self.last_status = msg.data
        if msg.data == 'completed':
            self.get_logger().info('[완료] 이동 완료!')
            self.motion_completed.set()
        elif 'error' in msg.data:
            self.get_logger().error(f'[에러] {msg.data}')
            self.motion_completed.set()
        else:
            self.get_logger().info(f'[상태] {msg.data}')

    def odom_callback(self, msg: Odometry):
        self.current_x = msg.pose.pose.position.x
        self.current_y = msg.pose.pose.position.y
        q = msg.pose.pose.orientation
        siny_cosp = 2.0 * (q.w * q.z + q.x * q.y)
        cosy_cosp = 1.0 - 2.0 * (q.y * q.y + q.z * q.z)
        self.current_theta = math.atan2(siny_cosp, cosy_cosp)
        self.odom_received = True

    def send_motion(self, action: str, value: float) -> bool:
        """모션 명령 전송 및 완료 대기"""
        self.motion_completed.clear()

        cmd = f"{action} {value}"
        msg = String()
        msg.data = cmd
        self.motion_cmd_pub.publish(msg)
        self.get_logger().info(f'[전송] {cmd}')

        if not self.motion_completed.wait(timeout=30.0):
            self.get_logger().error('[타임아웃] 30초 초과')
            return False

        return 'error' not in self.last_status

    def test_dispatch(self):
        """dispatch 테스트: 50cm 전진"""
        if self.state != "IDLE":
            print(f"[에러] 현재 상태 {self.state} - IDLE에서만 가능")
            return

        self.state = "MOVING"
        print(f"\n[dispatch] forward {self.move_distance}m 전진 시작...")

        if self.send_motion("forward", self.move_distance):
            self.state = "AT_DESTINATION"
            print(f"[성공] 도착 완료 (위치: x={self.current_x:.3f}, y={self.current_y:.3f})")
        else:
            self.state = "IDLE"
            print("[실패] 이동 실패")

    def test_return(self):
        """return 테스트: 50cm 후진으로 복귀"""
        if self.state != "AT_DESTINATION":
            print(f"[에러] 현재 상태 {self.state} - AT_DESTINATION에서만 가능")
            return

        self.state = "RETURNING"
        print(f"\n[return] backward {self.move_distance}m 후진 시작...")

        if self.send_motion("backward", self.move_distance):
            self.state = "IDLE"
            print(f"[성공] 시작점 복귀 완료 (위치: x={self.current_x:.3f}, y={self.current_y:.3f})")
        else:
            self.state = "AT_DESTINATION"
            print("[실패] 복귀 실패")


def main():
    # 1. 백그라운드 노드 자동 실행
    start_background_nodes()

    # 2. ROS2 초기화
    rclpy.init()
    node = MotionTestNode()

    spin_thread = threading.Thread(target=rclpy.spin, args=(node,), daemon=True)
    spin_thread.start()

    # 3. odom 수신 대기
    print("[대기] 오도메트리 수신 확인 중...")
    wait = 0
    while not node.odom_received and wait < 10:
        time.sleep(0.5)
        wait += 0.5

    if not node.odom_received:
        print("[경고] 오도메트리 미수신 - 노드가 정상 동작하는지 확인하세요")
    else:
        print("[확인] 오도메트리 수신 정상\n")

    # 4. 대화형 테스트
    print("=" * 50)
    print("모션 자가 테스트")
    print(f"이동 거리: {node.move_distance}m (50cm)")
    print("=" * 50)
    print("명령어:")
    print("  forward  - 50cm 전진 (dispatch)")
    print("  backward - 50cm 후진 (return)")
    print("  status   - 현재 상태/위치 확인")
    print("  quit     - 종료")
    print("=" * 50 + "\n")

    try:
        while True:
            cmd = input("테스트> ").strip().lower()

            if cmd in ("quit", "q"):
                break
            elif cmd in ("forward", "f", "dispatch"):
                node.test_dispatch()
            elif cmd in ("backward", "b", "return"):
                node.test_return()
            elif cmd in ("status", "s"):
                print(f"  상태: {node.state}")
                print(f"  위치: x={node.current_x:.3f}, y={node.current_y:.3f}")
                print(f"  방향: {math.degrees(node.current_theta):.1f}도")
                print(f"  odom: {'수신 중' if node.odom_received else '미수신'}")
            else:
                print("알 수 없는 명령")

    except KeyboardInterrupt:
        print("\n종료...")
    finally:
        node.destroy_node()
        rclpy.shutdown()


if __name__ == '__main__':
    main()
