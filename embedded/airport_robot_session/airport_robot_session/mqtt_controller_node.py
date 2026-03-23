#!/usr/bin/env python3
"""
공항 로봇 MQTT 컨트롤러 노드
- MQTT를 통해 백엔드 서버와 통신
- 명령 수신 및 상태 전송
- ROS2 노드와 통합
"""

import rclpy
from rclpy.node import Node
from std_msgs.msg import String, Bool
from geometry_msgs.msg import Twist
from nav_msgs.msg import Odometry
import threading
import time
import math
import json

try:
    import paho.mqtt.client as mqtt
    MQTT_AVAILABLE = True
except ImportError:
    MQTT_AVAILABLE = False
    print("경고: paho-mqtt가 설치되지 않았습니다. 'pip install paho-mqtt'로 설치하세요.")

from airport_robot_session.config import (
    RobotState, Location, LOCATIONS, DEFAULT_CONFIG, SessionConfig
)


class MQTTControllerNode(Node):
    """
    공항 로봇 MQTT 컨트롤러

    MQTT 메시지 형식:
    수신 (서버 → 로봇):
    {
        "robot_id": "carry1",
        "command": "call",  // call, unlock, lock, store
        "location": "station1"  // station1, gate1
    }

    송신 (로봇 → 서버):
    {
        "robot_id": "carry1",
        "status": "arrived",  // arrived, lock_changed
        "location": "station1",
        "lock_status": true,
        "arrival_status": true
    }
    """

    def __init__(self):
        super().__init__('mqtt_controller_node')

        # 로봇 설정
        self.robot_id = "carry1"

        # MQTT 설정 (파라미터로 받을 수 있음)
        self.declare_parameter('mqtt_broker', 'localhost')
        self.declare_parameter('mqtt_port', 1883)
        self.declare_parameter('mqtt_topic_cmd', f'robot/{self.robot_id}/command')
        self.declare_parameter('mqtt_topic_status', f'robot/{self.robot_id}/status')

        self.mqtt_broker = self.get_parameter('mqtt_broker').value
        self.mqtt_port = self.get_parameter('mqtt_port').value
        self.topic_cmd = self.get_parameter('mqtt_topic_cmd').value
        self.topic_status = self.get_parameter('mqtt_topic_status').value

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

        # 오도메트리 추적
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

        # MQTT 클라이언트 설정
        self.mqtt_client = None
        if MQTT_AVAILABLE:
            self.setup_mqtt()

        self.get_logger().info('=' * 50)
        self.get_logger().info(f'공항 로봇 MQTT 컨트롤러 시작 (로봇 ID: {self.robot_id})')
        self.get_logger().info(f'MQTT Broker: {self.mqtt_broker}:{self.mqtt_port}')
        self.get_logger().info(f'Command Topic: {self.topic_cmd}')
        self.get_logger().info(f'Status Topic: {self.topic_status}')
        self.get_logger().info('=' * 50)

        self.running = True

    def setup_mqtt(self):
        """MQTT 클라이언트 설정"""
        self.mqtt_client = mqtt.Client(client_id=self.robot_id)
        self.mqtt_client.on_connect = self.on_mqtt_connect
        self.mqtt_client.on_message = self.on_mqtt_message
        self.mqtt_client.on_disconnect = self.on_mqtt_disconnect

        try:
            self.mqtt_client.connect(self.mqtt_broker, self.mqtt_port, 60)
            self.mqtt_client.loop_start()
            self.get_logger().info('MQTT 연결 시도 중...')
        except Exception as e:
            self.get_logger().error(f'MQTT 연결 실패: {e}')

    def on_mqtt_connect(self, client, userdata, flags, rc):
        """MQTT 연결 콜백"""
        if rc == 0:
            self.get_logger().info('✅ MQTT 연결 성공')
            client.subscribe(self.topic_cmd)
            self.get_logger().info(f'📥 구독: {self.topic_cmd}')

            # 초기 상태 전송
            self.send_status("ready", lock_changed=True)
        else:
            self.get_logger().error(f'MQTT 연결 실패 (코드: {rc})')

    def on_mqtt_disconnect(self, client, userdata, rc):
        """MQTT 연결 해제 콜백"""
        if rc != 0:
            self.get_logger().warn(f'MQTT 연결 끊김 (코드: {rc}), 재연결 시도 중...')

    def on_mqtt_message(self, client, userdata, msg):
        """MQTT 메시지 수신 콜백"""
        try:
            payload = json.loads(msg.payload.decode())
            self.get_logger().info(f'📥 명령 수신: {payload}')

            # 로봇 ID 확인
            if payload.get('robot_id') != self.robot_id:
                self.get_logger().warn(f'다른 로봇 ID: {payload.get("robot_id")}')
                return

            command = payload.get('command')
            location = payload.get('location')

            # 명령 처리
            if command == 'call':
                self.handle_call_command(location)
            elif command == 'unlock':
                self.handle_unlock_command()
            elif command == 'lock':
                self.handle_lock_command()
            elif command == 'store':
                self.handle_store_command()
            else:
                self.get_logger().error(f'알 수 없는 명령: {command}')

        except json.JSONDecodeError:
            self.get_logger().error('JSON 파싱 실패')
        except Exception as e:
            self.get_logger().error(f'메시지 처리 오류: {e}')

    def send_status(self, status_type, lock_changed=False, arrival=False):
        """
        상태를 MQTT로 전송

        Args:
            status_type: 상태 타입 ("ready", "moving", "arrived", "lock_changed")
            lock_changed: 잠금 상태 변경 여부
            arrival: 도착 신호 여부
        """
        if not MQTT_AVAILABLE or not self.mqtt_client:
            return

        message = {
            "robot_id": self.robot_id,
            "status": status_type,
            "location": self.current_location.value,
            "lock_status": self.door_locked,
            "arrival_status": arrival
        }

        # 잠금 상태 변경 시에만 전송 또는 도착 시에만 전송
        if lock_changed or arrival or status_type == "ready":
            try:
                self.mqtt_client.publish(
                    self.topic_status,
                    json.dumps(message),
                    qos=1
                )
                self.get_logger().info(f'📤 상태 전송: {message}')
            except Exception as e:
                self.get_logger().error(f'상태 전송 실패: {e}')

    def handle_call_command(self, location_str):
        """호출 명령 처리"""
        if self.state != RobotState.IDLE:
            self.get_logger().warn('로봇이 이미 작업 중입니다')
            return

        # 위치 변환
        location_map = {
            "station1": Location.STATION1,
            "gate1": Location.GATE1
        }

        if location_str not in location_map:
            self.get_logger().error(f'잘못된 위치: {location_str}')
            return

        target = location_map[location_str]
        self.get_logger().info(f'{LOCATIONS[target].display_name}(으)로 이동 시작')

        self.state = RobotState.MOVING_TO_DEST
        self.target_location = target

        # 비동기 이동
        thread = threading.Thread(target=self._move_to_location, args=(target,))
        thread.daemon = True
        thread.start()

    def handle_unlock_command(self):
        """문 열기 명령 처리"""
        if self.state != RobotState.AT_DESTINATION or not self.door_locked:
            self.get_logger().warn('문을 열 수 없는 상태입니다')
            return

        self.door_locked = False
        self.get_logger().info(self.config.msg_door_unlocked)

        # 잠금 상태 변경 전송
        self.send_status("lock_changed", lock_changed=True)

    def handle_lock_command(self):
        """문 닫기 명령 처리"""
        if self.state != RobotState.AT_DESTINATION or self.door_locked:
            self.get_logger().warn('문을 닫을 수 없는 상태입니다')
            return

        self.door_locked = True
        self.get_logger().info(self.config.msg_door_locked)

        # 잠금 상태 변경 전송
        self.send_status("lock_changed", lock_changed=True)

    def handle_store_command(self):
        """보관 명령 처리 (시작지점으로 복귀)"""
        if self.state != RobotState.AT_DESTINATION:
            self.get_logger().warn('보관할 수 없는 상태입니다')
            return

        # 문이 열려있으면 닫기
        if not self.door_locked:
            self.door_locked = True
            self.get_logger().info(self.config.msg_door_locked)
            self.send_status("lock_changed", lock_changed=True)

        self.get_logger().info(self.config.msg_returning)
        self.state = RobotState.RETURNING

        # 비동기 복귀
        thread = threading.Thread(target=self._return_to_start)
        thread.daemon = True
        thread.start()

    def _move_to_location(self, location: Location):
        """특정 위치로 이동 (별도 스레드)"""
        if self.move_to_location(location):
            self.get_logger().info(self.config.msg_arrived)
            self.state = RobotState.AT_DESTINATION
            self.door_locked = True

            # 도착 신호 전송 (arrival_status True)
            self.send_status("arrived", arrival=True)
        else:
            self.get_logger().error('이동 실패!')
            self.state = RobotState.IDLE

    def _return_to_start(self):
        """시작지점으로 복귀 (별도 스레드)"""
        if self.return_to_start():
            self.get_logger().info(self.config.msg_returned)
            self.state = RobotState.IDLE

            # 복귀 완료 후 도착 신호 전송
            self.send_status("arrived", arrival=True)
        else:
            self.get_logger().error('복귀 실패!')
            self.state = RobotState.AT_DESTINATION

    def motion_status_callback(self, msg: String):
        """이동 상태 콜백"""
        status = msg.data

        if status == 'completed':
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
        """오도메트리 콜백"""
        self.current_x = msg.pose.pose.position.x
        self.current_y = msg.pose.pose.position.y

        quat = msg.pose.pose.orientation
        siny_cosp = 2.0 * (quat.w * quat.z + quat.x * quat.y)
        cosy_cosp = 1.0 - 2.0 * (quat.y * quat.y + quat.z * quat.z)
        self.current_theta = math.atan2(siny_cosp, cosy_cosp)

    def send_motion_command(self, action: str, value: float) -> bool:
        """이동 명령 전송 및 완료 대기"""
        self.motion_completed.clear()

        cmd = f"{action} {value}"
        msg = String()
        msg.data = cmd
        self.motion_cmd_pub.publish(msg)

        self.get_logger().info(f'명령 전송: {cmd}')

        if not self.motion_completed.wait(timeout=self.config.motion_timeout):
            self.get_logger().error('이동 타임아웃!')
            return False

        while self.emergency_stopped:
            self.get_logger().info('긴급 정지 중... 해제 대기')
            time.sleep(0.5)

        return True

    def execute_path(self, path) -> bool:
        """경로 실행"""
        for i, cmd in enumerate(path):
            self.get_logger().info(f'경로 {i+1}/{len(path)}: {cmd.action} {cmd.value}')

            if not self.send_motion_command(cmd.action, cmd.value):
                self.get_logger().error(f'경로 실행 실패: 단계 {i+1}')
                return False

            time.sleep(0.3)

        return True

    def move_to_location(self, location: Location) -> bool:
        """특정 위치로 이동"""
        if location == self.current_location:
            self.get_logger().info('이미 해당 위치에 있습니다.')
            return True

        loc_config = LOCATIONS[location]
        self.get_logger().info(f'{loc_config.display_name}(으)로 이동 시작...')

        if self.current_location == Location.START:
            path = loc_config.path_from_start
        else:
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

    def destroy_node(self):
        """노드 종료"""
        self.running = False
        if self.mqtt_client:
            self.mqtt_client.loop_stop()
            self.mqtt_client.disconnect()
        super().destroy_node()


def main(args=None):
    rclpy.init(args=args)

    try:
        node = MQTTControllerNode()

        # 노드 초기화 대기
        time.sleep(3)

        node.get_logger().info('MQTT 컨트롤러 준비 완료')

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
