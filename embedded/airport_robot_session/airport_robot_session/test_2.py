import json
import socket
import uuid
import datetime
import time
import threading
import subprocess
import os
import atexit
import paho.mqtt.client as mqtt

import rclpy
from rclpy.node import Node
from std_msgs.msg import String, Bool

# --- 1. 하드웨어 설정 (Jetson 전용) ---
RELAY_PIN = 7
GPIO = None

try:
    import Jetson.GPIO as GPIO
    GPIO.setwarnings(False)  # 경고 비활성화
    GPIO.setmode(GPIO.BOARD)

    # 기존 설정 정리 후 재설정
    try:
        GPIO.cleanup(RELAY_PIN)
    except:
        pass

    GPIO.setup(RELAY_PIN, GPIO.OUT, initial=GPIO.LOW)
    print("[GPIO] Jetson GPIO 초기화 완료")
except ImportError:
    print("[GPIO] Jetson.GPIO 모듈 없음 - 시뮬레이션 모드")
except Exception as e:
    print(f"[GPIO] 초기화 실패: {e}")
    print("[GPIO] 시뮬레이션 모드로 전환")
    GPIO = None

# 프로그램 종료 시 GPIO 정리
def cleanup_gpio():
    if GPIO is not None:
        try:
            GPIO.cleanup()
            print("[GPIO] 정리 완료")
        except:
            pass

atexit.register(cleanup_gpio)

# --- 1.5 백그라운드 ROS2 노드 관리 ---
bg_processes = []

def cleanup_ros_nodes():
    """종료 시 백그라운드 ROS2 노드 정리"""
    for p in bg_processes:
        try:
            p.terminate()
            p.wait(timeout=3)
        except:
            p.kill()
    if bg_processes:
        print("[정리] 백그라운드 ROS2 노드 종료 완료")

atexit.register(cleanup_ros_nodes)

def start_background_nodes():
    """motor_control, ydlidar, yolo, line_tracer 노드를 백그라운드로 실행"""
    script_dir = os.path.dirname(os.path.abspath(__file__))

    nodes = [
        ["ros2", "run", "robot_control", "motor_control_node"],
        ["ros2", "launch", "ydlidar_ros2_driver", "ydlidar_launch.py"],  # launch는 --log-level 미지원
        ["ros2", "launch", "object_detection", "yolo_detection.launch.py"],  # 필요시 활성화
        # line_follower_node.py는 dispatch 시 매개변수와 함께 실행됨
    ]
    for cmd in nodes:
        name = cmd[-1]
        print(f"[ROS2] {name} 실행 중...")
        p = subprocess.Popen(
            cmd,
        )
        bg_processes.append(p)

    print("[ROS2] 노드 초기화 대기 (3초)...")
    time.sleep(3)
    print("[ROS2] 백그라운드 노드 준비 완료\n")


# --- 1.6 ROS2 라인 트레이싱 브릿지 ---
class LineTraceBridge(Node):
    """ROS2 라인 트레이싱 명령 브릿지 - /line_trace/command 발행, /line_trace/status 구독"""

    def __init__(self):
        super().__init__('line_trace_bridge')

        # 상태 구독 (line_follower_node로부터)
        self.status_sub = self.create_subscription(
            String, '/line_trace/status',
            self._status_callback, 10
        )

        # 긴급 정지 구독
        self.emergency_stop_sub = self.create_subscription(
            Bool, '/emergency_stop',
            self._emergency_stop_callback, 10
        )
        self.emergency_stopped = False
        self._emergency_notified = False  # 최초 1회 MQTT 알림 플래그

        # RobotSimulator 참조 (외부에서 설정)
        self.robot = None

        self.line_trace_completed = threading.Event()
        self.last_status = ""
        self.get_logger().info('LineTraceBridge 노드 시작')

    def _status_callback(self, msg: String):
        """라인 트레이싱 상태 수신"""
        self.last_status = msg.data
        if msg.data == 'completed':
            self.get_logger().info('[완료] 라인 트레이싱 완료')
            self.line_trace_completed.set()
        elif msg.data == 'stopped':
            self.get_logger().info('[정지] 라인 트레이싱 정지됨')
            self.line_trace_completed.set()

    def _emergency_stop_callback(self, msg: Bool):
        """긴급 정지 콜백 - 최초 1회만 MQTT로 백엔드 알림"""
        prev_state = self.emergency_stopped
        self.emergency_stopped = msg.data

        if self.emergency_stopped and not prev_state:
            self.get_logger().warn('긴급 정지 활성화 - 1m 내 사람 감지!')
            if not self._emergency_notified and self.robot:
                self._emergency_notified = True
                self.robot.send_emergency()
        elif not self.emergency_stopped and prev_state:
            self.get_logger().info('긴급 정지 해제 - 안전 거리 확보')
            self._emergency_notified = False

    def start_line_trace(self, timeout: float = 180.0, target_color: str = None, initial_direction: str = 'CENTER') -> bool:
        """라인 트레이싱 시작 및 완료 대기

        Args:
            timeout: 타임아웃 시간 (초)
            target_color: 목표 색상 (RED, GREEN, BLUE 등)
            initial_direction: 초기 방향 (CENTER, LEFT, RIGHT)
        """
        self.line_trace_completed.clear()

        # 이전 line_follower_node 프로세스가 남아있으면 종료 (GPIO 충돌 방지)
        if hasattr(self, 'line_follower_process') and self.line_follower_process:
            if self.line_follower_process.poll() is None:
                self.get_logger().warn('[정리] 이전 line_follower_node 프로세스 종료 중...')
                self.line_follower_process.terminate()
                try:
                    self.line_follower_process.wait(timeout=5)
                except subprocess.TimeoutExpired:
                    self.line_follower_process.kill()
                    self.line_follower_process.wait(timeout=3)
                self.get_logger().info('[정리] 이전 프로세스 종료 완료')

        # line_follower_node.py를 매개변수와 함께 실행
        cmd = ["ros2", "run", "airport_robot_session", "line_follower_node"]

        if target_color:
            cmd.extend(["--target-color", target_color])
        if initial_direction != 'CENTER':
            cmd.extend(["--initial-direction", initial_direction])
        self.get_logger().info(f'[실행] line_follower_node (color={target_color}, direction={initial_direction})')

        # 프로세스 실행
        self.line_follower_process = subprocess.Popen(
            cmd
        )

        # 완료 대기
        if not self.line_trace_completed.wait(timeout=timeout):
            self.get_logger().error('[타임아웃] 라인 트레이싱 시간 초과')
            # 타임아웃 시 프로세스 종료
            try:
                self.line_follower_process.terminate()
                self.line_follower_process.wait(timeout=3)
            except:
                self.line_follower_process.kill()
            return False

        # ✅ completed 수신 즉시 리턴 (프로세스 종료 대기 제거)
        # GPIO 충돌은 다음 start_line_trace 호출 시 이전 프로세스 종료로 방지됨 (line 127-136)
        self.get_logger().info('[완료] line_follower_node completed 수신 - 즉시 리턴')
        return 'error' not in self.last_status

    def stop_line_trace(self):
        """라인 트레이싱 정지"""
        if hasattr(self, 'line_follower_process') and self.line_follower_process:
            try:
                self.line_follower_process.terminate()
                self.line_follower_process.wait(timeout=3)
                self.get_logger().info('[정지] line_follower_node 프로세스 종료')
            except:
                self.line_follower_process.kill()
                self.get_logger().warn('[강제 종료] line_follower_node 프로세스 kill')


# --- 2. 릴레이 제어 함수 ---
def set_relay(is_on: bool):
    if GPIO is None:
        # PC 시뮬레이션 모드
        if is_on:
            print("[시뮬레이션] 릴레이 ON (잠금 해제)")
        else:
            print("[시뮬레이션] 릴레이 OFF")
        return

    # 실제 Jetson 하드웨어
    if is_on:
        GPIO.output(RELAY_PIN, GPIO.HIGH)
        time.sleep(1)
        GPIO.output(RELAY_PIN, GPIO.LOW)
        time.sleep(1)
        print("[Action] 릴레이 ON (잠금 해제)")
    else:
        GPIO.output(RELAY_PIN, GPIO.LOW)
        print("[Action] 릴레이 OFF")

# ============== 설정 ==============
# 로컬 브로커
BROKER_HOST = "localhost"
BROKER_PORT = 1883

# 원격 브로커 (SSAFY)
REMOTE_BROKER_HOST = "i14e101.p.ssafy.io"
REMOTE_BROKER_PORT = 8030
REMOTE_USERNAME = "e101"
REMOTE_PASSWORD = "gdrd486dr"



def get_mac():
    """MAC 주소 가져오기"""
    mac = uuid.getnode()
    return ":".join([f"{(mac >> ele) & 0xff:02X}" for ele in range(40, -1, -8)])

def get_ip():
    """IP 주소 가져오기"""
    s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    try:
        s.connect(("8.8.8.8", 80))
        return s.getsockname()[0]
    finally:
        s.close()

def get_timestamp():
    """KST 타임스탬프"""
    return datetime.datetime.now(datetime.timezone(datetime.timedelta(hours=9))).isoformat()

# ============== 로봇 시뮬레이터 클래스 ==============
class RobotSimulator:
    def __init__(self, mac=None, broker_host=BROKER_HOST, broker_port=BROKER_PORT,
                 username=None, password=None):
        self.mac = "DW:AB:CC:DD:EE:FF"  # 파라미터 없으면 실제 MAC 주소 사용
        self.broker_host = broker_host
        self.broker_port = broker_port
        self.username = username
        self.password = password
        self.client = mqtt.Client()
        self.current_mission_id = None

        # 상태 관리
        self.state = "IDLE"            # IDLE, MOVING_TO_DEST, AT_DESTINATION, RETURNING
        self.current_location = "start"
        self.target_location = None
        self.door_locked = True
        self.is_first_dispatch = True  # 첫 dispatch 여부

        # ROS2 라인 트레이싱 브릿지 (외부에서 설정)
        self.line_trace_bridge = None

        # 인증 설정
        if username and password:
            self.client.username_pw_set(username, password)

        # 등록 관련
        self.registered = False
        self.register_retry_interval = 3  # 재시도 간격 (초)
        self.register_max_retries = 10    # 최대 재시도 횟수

        # 콜백 설정
        self.client.on_connect = self._on_connect
        self.client.on_message = self._on_message
        self.client.on_disconnect = self._on_disconnect
        self.client.on_publish = self._on_publish


    def _on_connect(self, client, userdata, flags, rc):
        if rc == 0:
            print(f"[연결 성공] 브로커 연결됨: {self.broker_host}:{self.broker_port}")
            # 서버로부터 명령을 받기 위해 구독
            command_topic = f"robot/{self.mac}/command/#"

            client.subscribe(command_topic)
            print(f"[구독] {command_topic}")

            # 연결되면 자동으로 등록 시작
            self._start_register_with_retry()
        else:
            print(f"[연결 실패] 코드: {rc}")

    def _on_disconnect(self, client, userdata, rc):
        print(f"[연결 해제] 코드: {rc}")
        self.registered = False

    def _on_publish(self, client, userdata, mid):
        """PUBBACK 수신 - 브로커가 메지를 받았음"""
        print(f"[PUBACK] mid={mid} 브로커 수신 확인")
        # 등록 메시지의 PUBACK이면 등록 완료 처리
        if not self.registered:
            self.registered = True
            print("[등록 완료] 브로커 ACK 수신")

    def _start_register_with_retry(self):
        """등록 요청 (재시도 로직 포함) - 별도 스레드에서 실행"""
        thread = threading.Thread(target=self._register_with_retry, daemon=True)
        thread.start()

    def _register_with_retry(self):
        """등록 요청 재시도 로직"""
        retries = 0
        while not self.registered and retries < self.register_max_retries:
            retries += 1
            print(f"\n[등록 시도 {retries}/{self.register_max_retries}]")
            self.register()

            # ACK 대기
            time.sleep(self.register_retry_interval)

            if self.registered:
                print("[등록 완료] ACK 수신 성공")
                return

            print(f"[등록 실패] ACK 미수신, 재시도...")

        if not self.registered:
            print(f"[등록 실패] 최대 재시도 횟수 초과 ({self.register_max_retries}회)")

    def _on_message(self, client, userdata, msg):
        """서버로부터 명령 수신 처리"""
        topic = msg.topic
        try:
            payload = json.loads(msg.payload.decode()) if msg.payload else {}
        except json.JSONDecodeError:
            payload = msg.payload.decode()

        print(f"\n[명령 수신] 토픽: {topic}")
        print(f"[명령 수신] 페이로드: {payload}")

        # 토픽 파싱
        parts = topic.split("/")

        # 등록 ACK 처리: robot/{mac}/register/ack
        if len(parts) >= 4 and parts[2] == "register" and parts[3] == "ack":
            self.registered = True
            print("[등록 ACK] 서버로부터 등록 확인 수신")
            return

        # 명령 처리: robot/{mac}/command/{action}
        if len(parts) >= 4 and parts[2] == "command":
            action = parts[3]
            self._handle_command(action, payload)

    def _handle_command(self, action, payload):
        """명령 처리 및 자동 응답"""
        # payload가 문자열이면 dict로 변환 시도
        if isinstance(payload, str):
            try:
                payload = json.loads(payload)
            except:
                payload = {}
        
        print("payload: ", payload)

        mission_id = payload.get("missionId", -1) if isinstance(payload, dict) else -1
        self.current_mission_id = mission_id

        if action == "dispatch":
            destination = payload.get("destination", "")
            print(f"[처리] 이동 명령 - 목적지: {destination}, missionId: {mission_id}")

            # 목적지 → 색상 매핑
            location_color_map = {
                "a": "RED",         # 첫번째 사용자 호출 위치
                "b": "GREEN",       # 두번째 사용자 호출 위치
                "c": "BLUE",        # 사용자 게이트 호출 위치
            }
            target_color = location_color_map.get(destination)
            print(f"[색상] {destination} → {target_color}")

            # 상태 검증: IDLE 상태에서만 이동 가능
            if self.state != "IDLE":
                print(f"[에러] 로봇이 이미 작업 중 (현재 상태: {self.state})")
                self.send_error("ERR_BUSY", "Robot is busy")
                return

            self.state = "MOVING_TO_DEST"
            self.target_location = destination
            print(f"[상태] IDLE → MOVING_TO_DEST (라인 트레이싱)")

            # 비동기 라인 트레이싱 시작 (색상 정보 포함)
            thread = threading.Thread(target=self._simulate_dispatch, args=(mission_id, target_color))
            thread.daemon = True
            thread.start()

        elif action == "return":
            print(f"[처리] 복귀 명령 - missionId: {mission_id}")

            # 상태 검증: AT_DESTINATION에서만 복귀 가능
            if self.state != "AT_DESTINATION":
                print(f"[에러] 복귀할 수 없는 상태 (현재 상태: {self.state})")
                self.send_error("ERR_INVALID_STATE", "Cannot return in current state")
                return

            # 문이 열려있으면 자동 잠금
            # if not self.door_locked:
            #     self.door_locked = True
            #     set_relay(False)
            #     print("[자동] 문 잠금 후 복귀 시작")

            self.state = "RETURNING"
            print(f"[상태] AT_DESTINATION → RETURNING")

            # 비동기 복귀 시작
            thread = threading.Thread(target=self._simulate_return, args=(mission_id,))
            thread.daemon = True
            thread.start()

        elif action == "stop":
            print(f"[처리] 긴급 정지 명령 (현재 상태: {self.state})")
            # 라인 트레이싱 정지
            if self.line_trace_bridge:
                self.line_trace_bridge.stop_line_trace()

        elif action == "lock":
            print(f"[처리] 잠금 명령 - missionId: {mission_id}")

            # 상태 검증: AT_DESTINATION에서만 잠금 가능
            if self.state != "AT_DESTINATION":
                print(f"[에러] 잠금할 수 없는 상태 (현재 상태: {self.state})")
                self.send_locked(mission_id, "failure")
                return

            self.door_locked = True
            set_relay(False)
            print("[응답] 잠금 완료 전송")
            self.send_locked(mission_id, "success")

        elif action == "unlock":
            print(f"[처리] 잠금 해제 명령 - missionId: {mission_id}")

            # 상태 검증: AT_DESTINATION에서만 잠금 해제 가능
            if self.state != "AT_DESTINATION":
                print(f"[에러] 잠금 해제할 수 없는 상태 (현재 상태: {self.state})")
                self.send_unlocked(mission_id, "failure")
                return

            self.door_locked = False
            set_relay(True)  # 릴레이 ON (솔레노이드 열림)
            print("[응답] 잠금 해제 완료 전송")
            self.send_unlocked(mission_id, "success")

        else:
            print(f"[처리] 알 수 없는 명령: {action}")
            self.send_error("ERR_UNKNOWN_CMD", f"Unknown command: {action}")

    def _simulate_dispatch(self, mission_id, target_color):
        """라인 트레이싱으로 목적지까지 이동 (별도 스레드)"""
        print(f"[이동] 라인 트레이싱 시작... (목표 색상: {target_color}, missionId: {mission_id})")

        if self.line_trace_bridge:
            # 첫 dispatch만 CENTER(직진), 이후는 LEFT(좌회전)으로 시작
            if self.is_first_dispatch:
                direction = 'CENTER'
                self.is_first_dispatch = False
            else:
                direction = 'LEFT'
            success = self.line_trace_bridge.start_line_trace(timeout=180.0, target_color=target_color, initial_direction=direction)
            if not success:
                print(f"[실패] 라인 트레이싱 실패 또는 타임아웃 (missionId: {mission_id})")
                self.state = "IDLE"
                self.send_error("ERR_LINE_TRACE", "Line tracing failed")
                return
        else:
            print("[경고] LineTraceBridge 없음 - 3초 시뮬레이션")
            time.sleep(3)

        self.current_location = "destination"
        self.state = "AT_DESTINATION"
        self.door_locked = True
        print(f"[상태] MOVING_TO_DEST → AT_DESTINATION (라인 트레이싱 완료, missionId: {mission_id})")
        print(f"[MQTT 발행 직전] 도착 알림 전송 시도 - missionId: {mission_id}")
        self.send_arrived(mission_id)
        print(f"[MQTT 발행 완료] 도착 알림 전송됨 - missionId: {mission_id}")

    def _simulate_return(self, mission_id):
        """라인 트레이싱으로 복귀 (별도 스레드)"""
        print(f"[복귀] 라인 트레이싱 시작...")

        if self.line_trace_bridge:
            success = self.line_trace_bridge.start_line_trace(timeout=180.0, initial_direction='LEFT')
            if not success:
                print("[실패] 라인 트레이싱 복귀 실패")
                self.state = "AT_DESTINATION"
                self.send_error("ERR_LINE_TRACE", "Line tracing return failed")
                return
        else:
            print("[경고] LineTraceBridge 없음 - 3초 시뮬레이션")
            time.sleep(3)

        self.current_location = "start"
        self.state = "IDLE"
        print(f"[상태] RETURNING → IDLE (라인 트레이싱 복귀 완료)")
        print("[응답] 복귀 완료 전송")
        self.send_returned(mission_id)

    def connect(self):
        """브로커 연결"""
        print(f"[시작] 로봇 MAC: {self.mac}")
        self.client.connect(self.broker_host, self.broker_port, keepalive=60)

    def start_loop(self):
        """메시지 루프 시작 (블로킹)"""
        self.client.loop_forever()

    def start_loop_background(self):
        """메시지 루프 시작 (백그라운드)"""
        self.client.loop_start()

    def stop(self):
        """연결 종료"""
        self.client.loop_stop()
        self.client.disconnect()



    # ============== 서버로 메시지 발행 ==============

    def register(self):
        """로봇 등록 요청: robot/{MAC}/register"""
        topic = f"robot/{self.mac}/register"
        payload = {"mac": self.mac}
        self._publish(topic, payload)
        print(f"[발행] 로봇 등록 요청 - MAC: {self.mac}")

    def send_arrived(self, mission_id: int):
        """도착 알림: robot/{MAC}/arrived"""
        topic = f"robot/{self.mac}/arrived"
        payload = {"missionId": mission_id}
        self._publish(topic, payload)
        print(f"[발행] 도착 알림 - missionId: {mission_id}")

    def send_returned(self, mission_id: int):
        """관리소 복귀 완료: robot/{MAC}/returned"""
        topic = f"robot/{self.mac}/returned"
        payload = {"missionId": mission_id}
        self._publish(topic, payload)
        print(f"[발행] 복귀 완료 - missionId: {mission_id}")

    def send_locked(self, mission_id: int, status: str = "success"):
        """잠금 완료 응답: robot/{MAC}/locked"""
        topic = f"robot/{self.mac}/locked"
        payload = {"missionId": mission_id, "status": status}
        self._publish(topic, payload)
        print(f"[발행] 잠금 완료 - missionId: {mission_id}")

    def send_unlocked(self, mission_id: int, status: str = "success"):
        """잠금 해제 완료 응답: robot/{MAC}/unlocked"""
        topic = f"robot/{self.mac}/unlocked"
        payload = {"missionId": mission_id, "status": status}
        self._publish(topic, payload)
        print(f"[발행] 잠금 해제 완료 - missionId: {mission_id}")

    def send_emergency(self):
        """긴급 정지 알림: robot/{MAC}/emergency"""
        topic = f"robot/{self.mac}/emergency"
        payload = {"type": "person_detected", "msg": "1m 이내 사람 감지로 긴급 정지"}
        self._publish(topic, payload)
        print(f"[발행] 긴급 정지 알림 - 사람 감지")

    def send_error(self, code: str, msg: str):
        """에러 발생 알림: robot/{MAC}/error"""
        topic = f"robot/{self.mac}/error"
        payload = {"code": code, "msg": msg}
        self._publish(topic, payload)
        print(f"[발행] 에러 알림 - code: {code}, msg: {msg}")

    def _publish(self, topic: str, payload: dict):
        """메시지 발행"""
        json_payload = json.dumps(payload)
        result = self.client.publish(topic, json_payload, qos=1)
        print(f"[MQTT 발행] topic={topic}, payload={json_payload}, mid={result.mid}, rc={result.rc}")
        if result.rc != 0:
            print(f"[MQTT 에러] 발행 실패 - rc={result.rc}")
        return result


# ============== ROS2 + MQTT 초기화 ==============
def init_ros2_line_trace():
    """ROS2 백그라운드 노드 실행 + LineTraceBridge 생성"""
    # 1. 백그라운드 노드 자동 실행 (별도 터미널에서 수동 실행)
    # start_background_nodes()

    # 2. ROS2 초기화
    rclpy.init()
    line_trace_bridge = LineTraceBridge()

    # 3. ROS2 spin을 별도 스레드에서 실행
    spin_thread = threading.Thread(target=rclpy.spin, args=(line_trace_bridge,), daemon=True)
    spin_thread.start()

    # 4. 노드 안정화 대기
    time.sleep(1)
    print("[ROS2] LineTraceBridge 준비 완료\n")

    return line_trace_bridge


def shutdown_ros2(line_trace_bridge):
    """ROS2 정리"""
    try:
        line_trace_bridge.destroy_node()
        rclpy.shutdown()
    except:
        pass


# ============== 대화형 테스트 ==============
def interactive_test(use_remote=False):
    """대화형 테스트 모드 - 실행 시 자동 등록 + 라인 트레이싱"""
    # ROS2 라인 트레이싱 초기화
    line_trace_bridge = init_ros2_line_trace()

    if use_remote:
        print(f"[모드] 원격 브로커 연결: {REMOTE_BROKER_HOST}:{REMOTE_BROKER_PORT}")
        robot = RobotSimulator(
            broker_host=REMOTE_BROKER_HOST,
            broker_port=REMOTE_BROKER_PORT,
            username=REMOTE_USERNAME,
            password=REMOTE_PASSWORD
        )
    else:
        print(f"[모드] 로컬 브로커 연결: {BROKER_HOST}:{BROKER_PORT}")
        robot = RobotSimulator()

    # LineTraceBridge ↔ RobotSimulator 양방향 연결
    robot.line_trace_bridge = line_trace_bridge
    line_trace_bridge.robot = robot

    robot.connect()
    robot.start_loop_background()

    # 등록 완료 대기 (최대 35초)
    print("\n[대기] 등록 완료 대기 중...")
    wait_time = 0
    while not robot.registered and wait_time < 35:
        time.sleep(1)
        wait_time += 1

    print("\n" + "="*50)
    print("로봇 MQTT + ROS2 라인 트레이싱 테스트")
    print(f"등록 상태: {'완료' if robot.registered else '실패'}")
    print("="*50)
    print("명령어:")
    print("  1. arrived <id> - 도착 알림")
    print("  2. returned <id>- 복귀 완료")
    print("  3. locked <id>  - 잠금 완료")
    print("  4. unlocked <id>- 잠금 해제 완료")
    print("  5. error        - 에러 발생")
    print("  r. register     - 수동 재등록")
    print("  s. status       - 현재 상태 확인")
    print("  q. quit         - 종료")
    print("="*50 + "\n")

    try:
        while True:
            cmd = input("명령> ").strip().lower().split()
            if not cmd:
                continue

            action = cmd[0]

            if action == "q" or action == "quit":
                break
            elif action == "r" or action == "register":
                robot.registered = False
                robot._start_register_with_retry()
            elif action in ("s", "status"):
                print(f"  상태: {robot.state}")
                print(f"  위치: {robot.current_location}")
                print(f"  문: {'잠김' if robot.door_locked else '열림'}")
            elif action in ("1", "arrived"):
                mission_id = int(cmd[1]) if len(cmd) > 1 else 1
                robot.send_arrived(mission_id)
            elif action in ("2", "returned"):
                mission_id = int(cmd[1]) if len(cmd) > 1 else 1
                robot.send_returned(mission_id)
            elif action in ("3", "locked"):
                mission_id = int(cmd[1]) if len(cmd) > 1 else 1
                robot.send_locked(mission_id)
            elif action in ("4", "unlocked"):
                mission_id = int(cmd[1]) if len(cmd) > 1 else 1
                robot.send_unlocked(mission_id)
            elif action in ("5", "error"):
                robot.send_error("ERR_TEST", "테스트 에러 메시지")
            else:
                print("알 수 없는 명령입니다.")

    except KeyboardInterrupt:
        print("\n종료...")
    finally:
        robot.stop()
        shutdown_ros2(line_trace_bridge)


# ============== 자동 실행 모드 ==============
def auto_run(use_remote=False):
    """자동 실행 - 등록 후 대기 + 라인 트레이싱"""
    # ROS2 라인 트레이싱 초기화
    line_trace_bridge = init_ros2_line_trace()

    if use_remote:
        print(f"[모드] 원격 브로커 연결: {REMOTE_BROKER_HOST}:{REMOTE_BROKER_PORT}")
        robot = RobotSimulator(
            broker_host=REMOTE_BROKER_HOST,
            broker_port=REMOTE_BROKER_PORT,
            username=REMOTE_USERNAME,
            password=REMOTE_PASSWORD
        )
    else:
        robot = RobotSimulator()

    # LineTraceBridge ↔ RobotSimulator 양방향 연결
    robot.line_trace_bridge = line_trace_bridge
    line_trace_bridge.robot = robot

    robot.connect()

    try:
        robot.start_loop()  # 블로킹 - 계속 실행
    except KeyboardInterrupt:
        print("\n종료...")
    finally:
        robot.stop()
        shutdown_ros2(line_trace_bridge)


if __name__ == "__main__":
    import sys

    use_remote = "remote" in sys.argv or "-r" in sys.argv
    use_auto = "auto" in sys.argv or "-a" in sys.argv

    print("="*50)
    print("MQTT 로봇 시뮬레이터 (라인 트레이싱)")
    print("="*50)
    print("사용법:")
    print("  python test_2_modified.py          - 로컬 브로커 (대화형)")
    print("  python test_2_modified.py remote   - 원격 브로커 (대화형)")
    print("  python test_2_modified.py auto     - 로컬 브로커 (자동)")
    print("  python test_2_modified.py remote auto - 원격 브로커 (자동)")
    print("="*50 + "\n")

    if use_auto:
        auto_run(use_remote)
    else:
        interactive_test(use_remote)