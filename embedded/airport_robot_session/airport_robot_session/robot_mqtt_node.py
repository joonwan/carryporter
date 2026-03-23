#!/usr/bin/env python3
"""
공항 로봇 MQTT ROS2 노드
- test.py의 RobotSimulator를 ROS2 노드로 변환
- 서버로부터 MQTT 명령 수신
- ROS2로 로봇 제어
- 서버로 상태 응답
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
import uuid

try:
    import paho.mqtt.client as mqtt
    MQTT_AVAILABLE = True
except ImportError:
    MQTT_AVAILABLE = False
    print("경고: paho-mqtt가 설치되지 않았습니다. 'pip install paho-mqtt'로 설치하세요.")

from airport_robot_session.config import (
    RobotState, Location, LOCATIONS, DEFAULT_CONFIG
)


def get_mac():
    """MAC 주소 가져오기"""
    mac = uuid.getnode()
    return ":".join([f"{(mac >> ele) & 0xff:02X}" for ele in range(40, -1, -8)])


class RobotMQTTNode(Node):
    """
    공항 로봇 MQTT ROS2 노드

    서버 → 로봇 명령:
    - robot/{MAC}/command/dispatch: {"location": "station1" or "gate1", "missionId": 1}
    - robot/{MAC}/command/return: {"missionId": 1}
    - robot/{MAC}/command/stop: {}
    - robot/{MAC}/command/lock: {"missionId": 1}
    - robot/{MAC}/command/unlock: {"missionId": 1}

    로봇 → 서버 응답:
    - robot/{MAC}/arrived: {"missionId": 1}
    - robot/{MAC}/returned: {"missionId": 1}
    - robot/{MAC}/locked: {"missionId": 1, "status": "success"}
    - robot/{MAC}/unlocked: {"missionId": 1, "status": "success"}
    - robot/{MAC}/error: {"code": "ERR_XXX", "msg": "..."}
    """

    def __init__(self):
        super().__init__('robot_mqtt_node')

        # MAC 주소
        self.mac = get_mac()

        # MQTT 설정
        self.declare_parameter('mqtt_broker', 'localhost')
        self.declare_parameter('mqtt_port', 1883)
        self.declare_parameter('mqtt_username', '')
        self.declare_parameter('mqtt_password', '')

        self.mqtt_broker = self.get_parameter('mqtt_broker').value
        self.mqtt_port = self.get_parameter('mqtt_port').value
        self.mqtt_username = self.get_parameter('mqtt_username').value
        self.mqtt_password = self.get_parameter('mqtt_password').value

        # 설정
        self.config = DEFAULT_CONFIG

        # 상태
        self.state = RobotState.IDLE
        self.current_location = Location.START
        self.target_location = None
        self.door_locked = True
        self.emergency_stopped = False
        self.current_mission_id = None

        # 이동 상태 추적
        self.motion_completed = threading.Event()

        # 오도메트리
        self.current_x = 0.0
        self.current_y = 0.0
        self.current_theta = 0.0

        # ROS2 발행자
        self.motion_cmd_pub = self.create_publisher(String, '/motion_command', 10)
        self.cmd_vel_pub = self.create_publisher(Twist, '/cmd_vel', 10)

        # ROS2 구독자
        self.motion_status_sub = self.create_subscription(
            String, '/motion_status', self.motion_status_callback, 10
        )
        self.emergency_stop_sub = self.create_subscription(
            Bool, '/emergency_stop', self.emergency_stop_callback, 10
        )
        self.odom_sub = self.create_subscription(
            Odometry, '/odom', self.odom_callback, 10
        )

        # MQTT 클라이언트 설정
        self.mqtt_client = None
        self.registered = False
        if MQTT_AVAILABLE:
            self.setup_mqtt()

        self.get_logger().info('=' * 60)
        self.get_logger().info(f'공항 로봇 MQTT ROS2 노드 시작')
        self.get_logger().info(f'MAC: {self.mac}')
        self.get_logger().info(f'MQTT: {self.mqtt_broker}:{self.mqtt_port}')
        self.get_logger().info('=' * 60)

    def setup_mqtt(self):
        """MQTT 클라이언트 설정"""
        self.mqtt_client = mqtt.Client(client_id=self.mac)
        self.mqtt_client.on_connect = self.on_mqtt_connect
        self.mqtt_client.on_message = self.on_mqtt_message
        self.mqtt_client.on_disconnect = self.on_mqtt_disconnect
        self.mqtt_client.on_publish = self.on_mqtt_publish

        # 인증 설정
        if self.mqtt_username and self.mqtt_password:
            self.mqtt_client.username_pw_set(self.mqtt_username, self.mqtt_password)

        try:
            self.mqtt_client.connect(self.mqtt_broker, self.mqtt_port, 60)
            self.mqtt_client.loop_start()
            self.get_logger().info('MQTT 연결 시도 중...')
        except Exception as e:
            self.get_logger().error(f'MQTT 연결 실패: {e}')

    def on_mqtt_connect(self, client, userdata, flags, rc):
        """MQTT 연결 콜백"""
        if rc == 0:
            self.get_logger().info('✅ MQTT 브로커 연결 성공')

            # 명령 토픽 구독
            command_topic = f"robot/{self.mac}/command/#"
            client.subscribe(command_topic)
            self.get_logger().info(f'📥 구독: {command_topic}')

            # 등록 ACK 구독
            register_ack_topic = f"robot/{self.mac}/register/ack"
            client.subscribe(register_ack_topic)

            # 자동 등록
            self.register()
        else:
            self.get_logger().error(f'MQTT 연결 실패 (코드: {rc})')

    def on_mqtt_disconnect(self, client, userdata, rc):
        """MQTT 연결 해제 콜백"""
        if rc != 0:
            self.get_logger().warn(f'MQTT 연결 끊김 (코드: {rc}), 재연결 시도 중...')
        self.registered = False

    def on_mqtt_publish(self, client, userdata, mid):
        """MQTT 발행 확인"""
        if not self.registered:
            self.registered = True
            self.get_logger().info('[등록 완료] 브로커 ACK 수신')

    def on_mqtt_message(self, client, userdata, msg):
        """MQTT 메시지 수신"""
        topic = msg.topic
        try:
            payload = json.loads(msg.payload.decode()) if msg.payload else {}
        except json.JSONDecodeError:
            payload = {}

        self.get_logger().info(f'📥 수신 - 토픽: {topic}')
        self.get_logger().info(f'📥 수신 - 페이로드: {payload}')

        # 토픽 파싱
        parts = topic.split("/")

        # 등록 ACK
        if len(parts) >= 4 and parts[2] == "register" and parts[3] == "ack":
            self.registered = True
            self.get_logger().info('[등록 ACK] 서버 확인')
            return

        # 명령 처리: robot/{MAC}/command/{action}
        if len(parts) >= 4 and parts[2] == "command":
            action = parts[3]
            self.handle_command(action, payload)

    def handle_command(self, action: str, payload: dict):
        """명령 처리 - test.py의 _handle_command와 동일"""
        mission_id = payload.get("missionId", 0)
        self.current_mission_id = mission_id

        if action == "dispatch":
            location_str = payload.get("location", "")
            self.get_logger().info(f'[명령] dispatch - 목적지: {location_str}, missionId: {mission_id}')
            self.handle_dispatch(location_str, mission_id)

        elif action == "return":
            self.get_logger().info(f'[명령] return - missionId: {mission_id}')
            self.handle_return(mission_id)

        elif action == "stop": 
            self.get_logger().info('[명령] stop - 긴급 정지')
            self.handle_stop()

        elif action == "lock":
            self.get_logger().info(f'[명령] lock - missionId: {mission_id}')
            self.handle_lock(mission_id)

        elif action == "unlock":
            self.get_logger().info(f'[명령] unlock - missionId: {mission_id}')
            self.handle_unlock(mission_id)

        else:
            self.get_logger().error(f'알 수 없는 명령: {action}')
            self.send_error("ERR_UNKNOWN_CMD", f"Unknown command: {action}")

    def handle_dispatch(self, location_str: str, mission_id: int):
        """dispatch 명령 처리 - 목적지로 이동"""
        if self.state != RobotState.IDLE:
            self.get_logger().warn('로봇이 이미 작업 중입니다')
            self.send_error("ERR_BUSY", "Robot is busy")
            return

        # 위치 변환
        location_map = {
            "station1": Location.STATION1,
            "gate1": Location.GATE1
        }

        if location_str not in location_map:
            self.get_logger().error(f'잘못된 위치: {location_str}')
            self.send_error("ERR_INVALID_LOCATION", f"Invalid location: {location_str}")
            return

        target = location_map[location_str]
        self.target_location = target
        self.state = RobotState.MOVING_TO_DEST

        # 비동기 이동
        thread = threading.Thread(target=self._move_to_location, args=(target, mission_id))
        thread.daemon = True
        thread.start()

    def handle_return(self, mission_id: int):
        """return 명령 처리 - 시작지점으로 복귀"""
        if self.state != RobotState.AT_DESTINATION:
            self.get_logger().warn('복귀할 수 없는 상태입니다')
            self.send_error("ERR_INVALID_STATE", "Cannot return in current state")
            return

        # 문이 열려있으면 닫기
        if not self.door_locked:
            self.door_locked = True
            self.get_logger().info(self.config.msg_door_locked)

        self.get_logger().info(self.config.msg_returning)
        self.state = RobotState.RETURNING

        # 비동기 복귀
        thread = threading.Thread(target=self._return_to_start, args=(mission_id,))
        thread.daemon = True
        thread.start()

    def handle_stop(self):
        """stop 명령 처리 - 긴급 정지"""
        # 모든 모터 정지
        twist = Twist()
        twist.linear.x = 0.0
        twist.angular.z = 0.0
        self.cmd_vel_pub.publish(twist)

        self.get_logger().warn('긴급 정지 명령 수신!')
        # 상태는 유지 (이동 중이었으면 일시정지)

    def handle_lock(self, mission_id: int):
        """lock 명령 처리 - 잠금"""
        if self.state != RobotState.AT_DESTINATION:
            self.get_logger().warn('잠금할 수 없는 상태입니다')
            self.send_locked(mission_id, "failure")
            return

        self.door_locked = True
        self.get_logger().info(self.config.msg_door_locked)
        self.send_locked(mission_id, "success")

    def handle_unlock(self, mission_id: int):
        """unlock 명령 처리 - 잠금 해제"""
        if self.state != RobotState.AT_DESTINATION:
            self.get_logger().warn('잠금 해제할 수 없는 상태입니다')
            self.send_unlocked(mission_id, "failure")
            return

        self.door_locked = False
        self.get_logger().info(self.config.msg_door_unlocked)
        self.send_unlocked(mission_id, "success")

    # ============== 이동 로직 (기존 mqtt_controller_node.py와 동일) ==============

    def _move_to_location(self, location: Location, mission_id: int):
        """특정 위치로 이동 (별도 스레드)"""
        if self.move_to_location(location):
            self.get_logger().info(self.config.msg_arrived)
            self.state = RobotState.AT_DESTINATION
            self.door_locked = True

            # 도착 알림 전송
            self.send_arrived(mission_id)
        else:
            self.get_logger().error('이동 실패!')
            self.state = RobotState.IDLE
            self.send_error("ERR_MOVE_FAILED", "Failed to move to destination")

    def _return_to_start(self, mission_id: int):
        """시작지점으로 복귀 (별도 스레드)"""
        if self.return_to_start():
            self.get_logger().info(self.config.msg_returned)
            self.state = RobotState.IDLE

            # 복귀 완료 알림 전송
            self.send_returned(mission_id)
        else:
            self.get_logger().error('복귀 실패!')
            self.state = RobotState.AT_DESTINATION
            self.send_error("ERR_RETURN_FAILED", "Failed to return to start")

    def motion_status_callback(self, msg: String):
        """이동 상태 콜백"""
        status = msg.data

        if status == 'completed':
            self.motion_completed.set()
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
            return True

        loc_config = LOCATIONS[location]

        if self.current_location == Location.START:
            path = loc_config.path_from_start
        else:
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
            return True

        loc_config = LOCATIONS[self.current_location]

        if self.execute_path(loc_config.path_to_start):
            self.current_location = Location.START
            return True

        return False

    # ============== MQTT 메시지 전송 (test.py와 동일) ==============

    def register(self):
        """로봇 등록"""
        topic = f"robot/{self.mac}/register"
        payload = {"mac": self.mac}
        self._publish(topic, payload)
        self.get_logger().info(f'📤 등록 요청 - MAC: {self.mac}')

    def send_arrived(self, mission_id: int):
        """도착 알림"""
        topic = f"robot/{self.mac}/arrived"
        payload = {"missionId": mission_id}
        self._publish(topic, payload)
        self.get_logger().info(f'📤 도착 알림 - missionId: {mission_id}')

    def send_returned(self, mission_id: int):
        """복귀 완료"""
        topic = f"robot/{self.mac}/returned"
        payload = {"missionId": mission_id}
        self._publish(topic, payload)
        self.get_logger().info(f'📤 복귀 완료 - missionId: {mission_id}')

    def send_locked(self, mission_id: int, status: str):
        """잠금 완료"""
        topic = f"robot/{self.mac}/locked"
        payload = {"missionId": mission_id, "status": status}
        self._publish(topic, payload)
        self.get_logger().info(f'📤 잠금 완료 - missionId: {mission_id}')

    def send_unlocked(self, mission_id: int, status: str):
        """잠금 해제 완료"""
        topic = f"robot/{self.mac}/unlocked"
        payload = {"missionId": mission_id, "status": status}
        self._publish(topic, payload)
        self.get_logger().info(f'📤 잠금 해제 완료 - missionId: {mission_id}')

    def send_error(self, code: str, msg: str):
        """에러 발생"""
        topic = f"robot/{self.mac}/error"
        payload = {"code": code, "msg": msg}
        self._publish(topic, payload)
        self.get_logger().error(f'📤 에러 - code: {code}, msg: {msg}')

    def _publish(self, topic: str, payload: dict):
        """메시지 발행"""
        if self.mqtt_client:
            self.mqtt_client.publish(topic, json.dumps(payload), qos=1)

    def destroy_node(self):
        """노드 종료"""
        if self.mqtt_client:
            self.mqtt_client.loop_stop()
            self.mqtt_client.disconnect()
        super().destroy_node()


def main(args=None):
    rclpy.init(args=args)

    try:
        node = RobotMQTTNode()

        # 초기화 대기
        time.sleep(2)

        node.get_logger().info('MQTT ROS2 노드 준비 완료')

        rclpy.spin(node)

    except KeyboardInterrupt:
        print('\n사용자 종료')
    finally:
        if rclpy.ok():
            node.destroy_node()
            rclpy.shutdown()


if __name__ == '__main__':
    main()
