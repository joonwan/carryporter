#!/usr/bin/env python3
"""
공항 로봇 세션 컨트롤러 노드
- 대화형 메뉴로 로봇 제어
- 기존 time_based_control 및 YOLO 감지 기능 활용
"""

import rclpy
from rclpy.node import Node
from std_msgs.msg import String, Bool
from geometry_msgs.msg import Twist
from nav_msgs.msg import Odometry
import threading
import sys
import time
import math

from airport_robot_session.config import (
    RobotState, Location, LOCATIONS, DEFAULT_CONFIG, SessionConfig
)


class SessionControllerNode(Node):
    """
    공항 로봇 세션 컨트롤러

    구독:
        - /motion_status (std_msgs/String): 이동 상태
        - /emergency_stop (std_msgs/Bool): 긴급 정지 신호

    발행:
        - /motion_command (std_msgs/String): 이동 명령
    """

    def __init__(self):
        super().__init__('session_controller_node')

        # 설정
        self.config = DEFAULT_CONFIG

        # 상태
        self.state = RobotState.IDLE
        self.current_location = Location.START
        self.target_location = None
        self.door_locked = True
        self.emergency_stopped = False

        # 이동 상태 추적
        self.motion_completed = threading.Event()
        self.current_motion_index = 0
        self.current_path = []

        # 오도메트리 추적 (곡선 이동용)
        self.current_x = 0.0
        self.current_y = 0.0
        self.current_theta = 0.0

        # 발행자
        self.motion_cmd_pub = self.create_publisher(
            String, '/motion_command', 10
        )
        self.cmd_vel_pub = self.create_publisher(
            Twist, '/cmd_vel', 10
        )

        # 구독자
        self.motion_status_sub = self.create_subscription(
            String,
            '/motion_status',
            self.motion_status_callback,
            10
        )

        self.emergency_stop_sub = self.create_subscription(
            Bool,
            '/emergency_stop',
            self.emergency_stop_callback,
            10
        )

        self.odom_sub = self.create_subscription(
            Odometry,
            '/odom',
            self.odom_callback,
            10
        )

        self.get_logger().info('=' * 50)
        self.get_logger().info('공항 로봇 세션 컨트롤러 시작')
        self.get_logger().info('=' * 50)

        # 대화형 메뉴 스레드 시작
        self.running = True
        self.menu_thread = threading.Thread(target=self.interactive_menu)
        self.menu_thread.daemon = True
        self.menu_thread.start()

    def motion_status_callback(self, msg: String):
        """이동 상태 콜백"""
        status = msg.data

        if status == 'completed':
            self.get_logger().info('이동 완료 신호 수신')
            self.motion_completed.set()
        elif status == 'busy':
            self.get_logger().warn('이전 이동이 아직 진행 중...')
        elif 'error' in status:
            self.get_logger().error(f'이동 오류: {status}')
            self.motion_completed.set()

    def emergency_stop_callback(self, msg: Bool):
        """긴급 정지 콜백"""
        prev_state = self.emergency_stopped
        self.emergency_stopped = msg.data

        if self.emergency_stopped and not prev_state:
            self.get_logger().warn('긴급 정지 활성화 - 1m 내 사람 감지!')
        elif not self.emergency_stopped and prev_state:
            self.get_logger().info('긴급 정지 해제 - 안전 거리 확보')

    def odom_callback(self, msg: Odometry):
        """오도메트리 콜백 (위치 추적)"""
        self.current_x = msg.pose.pose.position.x
        self.current_y = msg.pose.pose.position.y

        # 쿼터니언 → 오일러 각도 변환 (yaw만 필요)
        quat = msg.pose.pose.orientation
        siny_cosp = 2.0 * (quat.w * quat.z + quat.x * quat.y)
        cosy_cosp = 1.0 - 2.0 * (quat.y * quat.y + quat.z * quat.z)
        self.current_theta = math.atan2(siny_cosp, cosy_cosp)

    def send_motion_command(self, action: str, value: float) -> bool:
        """
        이동 명령 전송 및 완료 대기

        Args:
            action: 동작 (forward, backward, rotate)
            value: 값 (거리 또는 각도)

        Returns:
            bool: 성공 여부
        """
        # 이벤트 초기화
        self.motion_completed.clear()

        # 명령 전송
        cmd = f"{action} {value}"
        msg = String()
        msg.data = cmd
        self.motion_cmd_pub.publish(msg)

        self.get_logger().info(f'명령 전송: {cmd}')

        # 완료 대기 (타임아웃 적용)
        if not self.motion_completed.wait(timeout=self.config.motion_timeout):
            self.get_logger().error('이동 타임아웃!')
            return False

        # 긴급 정지 중이면 해제될 때까지 대기
        while self.emergency_stopped:
            self.get_logger().info('긴급 정지 중... 해제 대기')
            time.sleep(0.5)

        return True

    def execute_curve_backward(self, distance: float, angle: float) -> bool:
        """
        곡선 후진 실행 (후진하면서 회전)

        Args:
            distance: 후진 거리 (m, 양수)
            angle: 회전 각도 (도, 음수=우회전)

        Returns:
            bool: 성공 여부
        """
        self.get_logger().info(f'곡선 후진 시작: {distance}m 후진, {angle}도 회전')

        # 시작 위치/각도 저장
        start_x = self.current_x
        start_y = self.current_y
        start_theta = self.current_theta

        # 목표값 계산
        target_distance = abs(distance)
        target_angle_rad = math.radians(angle)

        # 속도 설정 (time_based_control config에서 가져온 값)
        linear_speed = -0.147  # 후진 (음수)
        angular_speed = -0.65  # 우회전 (음수, max_angular_speed와 동일)

        # 예상 소요 시간 계산 (더 긴 시간 기준)
        time_for_distance = target_distance / abs(linear_speed)
        time_for_angle = abs(target_angle_rad) / abs(angular_speed)
        estimated_time = max(time_for_distance, time_for_angle)

        self.get_logger().info(f'예상 소요 시간: {estimated_time:.2f}초')

        # Twist 메시지 생성
        twist = Twist()
        twist.linear.x = linear_speed
        twist.angular.z = angular_speed

        # 곡선 이동 실행
        start_time = time.time()
        rate = self.create_rate(20)  # 20Hz

        while True:
            elapsed = time.time() - start_time

            # 긴급 정지 확인
            if self.emergency_stopped:
                self.get_logger().warn('긴급 정지로 인한 일시 정지')
                twist_stop = Twist()
                self.cmd_vel_pub.publish(twist_stop)
                while self.emergency_stopped:
                    time.sleep(0.1)
                start_time = time.time()  # 시간 리셋
                continue

            # 이동 거리 계산
            dx = self.current_x - start_x
            dy = self.current_y - start_y
            traveled_distance = math.sqrt(dx*dx + dy*dy)

            # 회전 각도 계산
            angle_diff = self.current_theta - start_theta
            # -π ~ π 범위로 정규화
            while angle_diff > math.pi:
                angle_diff -= 2 * math.pi
            while angle_diff < -math.pi:
                angle_diff += 2 * math.pi

            # 진행상황 로그
            if int(elapsed * 10) % 10 == 0:  # 1초마다
                self.get_logger().info(
                    f'곡선 후진 중: {traveled_distance:.3f}m / {target_distance:.3f}m, '
                    f'{math.degrees(angle_diff):.1f}도 / {angle:.1f}도'
                )

            # 완료 조건 확인 (거리와 각도 모두 달성 or 시간 초과)
            if (traveled_distance >= target_distance * 0.95 and
                abs(angle_diff - target_angle_rad) <= math.radians(5)) or \
               elapsed > estimated_time * 1.5:
                self.get_logger().info('곡선 후진 완료')
                break

            # 계속 이동
            self.cmd_vel_pub.publish(twist)
            rate.sleep()

        # 정지
        twist_stop = Twist()
        self.cmd_vel_pub.publish(twist_stop)
        time.sleep(0.2)

        return True

    def execute_path(self, path) -> bool:
        """
        경로 실행 (명령 리스트 순차 실행)

        Args:
            path: MotionCommand 리스트

        Returns:
            bool: 성공 여부
        """
        for i, cmd in enumerate(path):
            self.get_logger().info(f'경로 {i+1}/{len(path)}: {cmd.action} {cmd.value}')

            # 곡선 후진 명령은 특수 처리
            if cmd.action == "curve_backward":
                if not self.execute_curve_backward(cmd.value, cmd.angle):
                    self.get_logger().error(f'경로 실행 실패: 단계 {i+1}')
                    return False
            else:
                # 일반 명령은 기존 방식 사용
                if not self.send_motion_command(cmd.action, cmd.value):
                    self.get_logger().error(f'경로 실행 실패: 단계 {i+1}')
                    return False

            # 다음 명령 전 짧은 대기
            time.sleep(0.3)

        return True

    def move_to_location(self, location: Location) -> bool:
        """
        특정 위치로 이동

        Args:
            location: 목표 위치

        Returns:
            bool: 성공 여부
        """
        if location == self.current_location:
            self.get_logger().info('이미 해당 위치에 있습니다.')
            return True

        loc_config = LOCATIONS[location]
        self.get_logger().info(f'{loc_config.display_name}(으)로 이동 시작...')

        # 현재 시작지점에서 출발하는 경우
        if self.current_location == Location.START:
            path = loc_config.path_from_start
        else:
            # 다른 위치에서 출발하는 경우 - 먼저 시작지점으로 복귀
            self.get_logger().info('먼저 시작지점으로 복귀합니다...')
            current_config = LOCATIONS[self.current_location]
            if not self.execute_path(current_config.path_to_start):
                return False
            self.current_location = Location.START
            path = loc_config.path_from_start

        if self.execute_path(path):
            self.current_location = location
            return True

        return False

    def return_to_start(self) -> bool:
        """시작지점으로 복귀"""
        if self.current_location == Location.START:
            self.get_logger().info('이미 시작지점에 있습니다.')
            return True

        loc_config = LOCATIONS[self.current_location]
        self.get_logger().info(self.config.msg_returning)

        if self.execute_path(loc_config.path_to_start):
            self.current_location = Location.START
            return True

        return False

    def print_status(self):
        """현재 상태 출력"""
        loc_name = LOCATIONS[self.current_location].display_name
        state_names = {
            RobotState.IDLE: "대기 중",
            RobotState.MOVING_TO_DEST: "이동 중",
            RobotState.AT_DESTINATION: "목적지 도착",
            RobotState.DOOR_UNLOCKED: "문 열림",
            RobotState.DOOR_LOCKED: "문 잠김",
            RobotState.RETURNING: "복귀 중"
        }

        print("\n" + "=" * 40)
        print(f"현재 위치: {loc_name}")
        print(f"로봇 상태: {state_names.get(self.state, '알 수 없음')}")
        print(f"문 상태: {'잠김' if self.door_locked else '열림'}")
        print(f"긴급정지: {'활성화' if self.emergency_stopped else '비활성화'}")
        print("=" * 40)

    def print_menu(self):
        """메뉴 출력"""
        print("\n" + "-" * 40)
        print("         공항 로봇 제어 메뉴")
        print("-" * 40)

        if self.state == RobotState.IDLE:
            print("1. 정류장1로 호출")
            print("2. 게이트로 호출")
        elif self.state == RobotState.AT_DESTINATION:
            if self.door_locked:
                print("3. 문 열기 (잠금 해제)")
            else:
                print("4. 문 닫기 (잠금)")
                print("5. 보관 완료 (시작지점으로 복귀)")

        print("-" * 40)
        print("0. 종료")
        print("-" * 40)

    def interactive_menu(self):
        """대화형 질문-응답 루프"""
        print("\n노드 초기화 대기 중...")
        time.sleep(3)  # 노드 초기화 대기 (오도메트리, 정밀 이동 노드)

        print("\n" + "=" * 50)
        print("      공항 짐 보관 로봇 세션 시작")
        print("=" * 50)
        print("(종료하려면 언제든지 '종료' 또는 Ctrl+C)")
        print("")

        while self.running and rclpy.ok():
            try:
                # 상태별 대화형 질문
                if self.state == RobotState.IDLE:
                    # 1. 호출 단계
                    print("\n" + "-" * 50)
                    print(f"로봇 상태: 시작지점에서 대기 중 (문 잠김)")
                    print("-" * 50)
                    response = input("\n사용자가 로봇을 호출합니다. 어디로 호출하시겠습니까?\n(정류장1 / 게이트): ").strip()

                    if response == '종료':
                        print("\n세션을 종료합니다.")
                        self.running = False
                        break
                    elif response in ['정류장1', '정류장 1', '1']:
                        # 정류장1로 이동
                        print(f"\n{self.config.msg_moving}")
                        self.state = RobotState.MOVING_TO_DEST
                        self.target_location = Location.STATION1

                        if self.move_to_location(Location.STATION1):
                            print(f"\n{self.config.msg_arrived}")
                            self.state = RobotState.AT_DESTINATION
                            self.door_locked = True
                        else:
                            print("\n이동 실패!")
                            self.state = RobotState.IDLE

                    elif response in ['게이트', '2']:
                        # 게이트로 이동
                        print(f"\n{self.config.msg_moving}")
                        self.state = RobotState.MOVING_TO_DEST
                        self.target_location = Location.GATE

                        if self.move_to_location(Location.GATE):
                            print(f"\n{self.config.msg_arrived}")
                            self.state = RobotState.AT_DESTINATION
                            self.door_locked = True
                        else:
                            print("\n이동 실패!")
                            self.state = RobotState.IDLE
                    else:
                        print("\n잘못된 입력입니다. '정류장1' 또는 '게이트'를 입력하세요.")

                elif self.state == RobotState.AT_DESTINATION and self.door_locked:
                    # 2. 문 열기 단계
                    loc_name = LOCATIONS[self.current_location].display_name
                    print("\n" + "-" * 50)
                    print(f"로봇 상태: {loc_name}에 도착 (문 잠김)")
                    print("-" * 50)
                    response = input("\n문을 여시겠습니까?\n(예 / 아니오): ").strip()

                    if response == '종료':
                        print("\n세션을 종료합니다.")
                        self.running = False
                        break
                    elif response in ['예', 'ㅇ', 'y', 'yes', '네', '열기']:
                        self.door_locked = False
                        print(f"\n{self.config.msg_door_unlocked}")
                    else:
                        print("\n문이 잠긴 상태를 유지합니다.")

                elif self.state == RobotState.AT_DESTINATION and not self.door_locked:
                    # 3. 문 닫기 및 보관 단계
                    loc_name = LOCATIONS[self.current_location].display_name
                    print("\n" + "-" * 50)
                    print(f"로봇 상태: {loc_name}에 도착 (문 열림)")
                    print("-" * 50)
                    response = input("\n짐을 실으셨나요? 문을 닫고 보관하시겠습니까?\n(예 / 아니오): ").strip()

                    if response == '종료':
                        print("\n세션을 종료합니다.")
                        self.running = False
                        break
                    elif response in ['예', 'ㅇ', 'y', 'yes', '네', '보관']:
                        # 문 잠금 후 시작지점으로 복귀
                        self.door_locked = True
                        print(f"\n{self.config.msg_door_locked}")
                        print(f"{self.config.msg_returning}")
                        self.state = RobotState.RETURNING

                        if self.return_to_start():
                            print(f"\n{self.config.msg_returned}")
                            print("\n" + "=" * 50)
                            print("세션이 완료되었습니다. 새로운 호출을 기다립니다.")
                            print("=" * 50)
                            self.state = RobotState.IDLE
                        else:
                            print("\n복귀 실패!")
                            self.state = RobotState.AT_DESTINATION
                    else:
                        print("\n문이 열린 상태를 유지합니다.")

                else:
                    print("\n알 수 없는 상태입니다. 초기화합니다.")
                    self.state = RobotState.IDLE

            except EOFError:
                break
            except KeyboardInterrupt:
                print("\n\n사용자 인터럽트")
                self.running = False
                break

    def destroy_node(self):
        """노드 종료"""
        self.running = False
        super().destroy_node()


def main(args=None):
    rclpy.init(args=args)

    try:
        node = SessionControllerNode()

        # 메뉴 스레드가 종료될 때까지 스핀
        while node.running and rclpy.ok():
            rclpy.spin_once(node, timeout_sec=0.1)

    except KeyboardInterrupt:
        print('\n사용자 종료')
    finally:
        if rclpy.ok():
            node.destroy_node()
            rclpy.shutdown()


if __name__ == '__main__':
    main()
