#!/usr/bin/env python3
"""
YOLO 객체 탐지 ROS2 노드
- 카메라 이미지를 구독하여 사람 탐지
- 탐지 결과를 여러 형태로 발행
- GPU 가속 사용 (Jetson Orin 최적화)
"""
import os

# GPU 강제 사용 설정 (import 전에 설정)
# CRITICAL: Jetson uses native allocator for unified memory
os.environ['PYTORCH_CUDA_ALLOC_CONF'] = 'backend:native'
os.environ['CUDA_VISIBLE_DEVICES'] = '0'

import rclpy
from rclpy.node import Node
from rclpy.qos import QoSProfile, QoSReliabilityPolicy, QoSHistoryPolicy
from sensor_msgs.msg import Image, CompressedImage
from std_msgs.msg import Bool, String
from vision_msgs.msg import Detection2DArray, Detection2D, ObjectHypothesisWithPose
from geometry_msgs.msg import Pose2D
from cv_bridge import CvBridge
from ultralytics import YOLO
import cv2
import torch

class YOLODetectorNode(Node):
    def __init__(self):
        super().__init__('yolo_detector_node')

        # 파라미터 선언
        self.declare_parameter('model_path', 'yolov8n.pt')
        self.declare_parameter('confidence_threshold', 0.75)
        self.declare_parameter('target_class', 0)  # 0 = person
        self.declare_parameter('publish_rate', 15.0)  # Hz
        self.declare_parameter('use_compression', True)

        # 파라미터 읽기
        model_path = self.get_parameter('model_path').value
        self.confidence_threshold = self.get_parameter('confidence_threshold').value
        self.target_class = self.get_parameter('target_class').value
        self.publish_rate = self.get_parameter('publish_rate').value
        self.use_compression = self.get_parameter('use_compression').value

        # GPU 설정
        self.device = 'cuda:0' if torch.cuda.is_available() else 'cpu'
        self.get_logger().info(f'Using device: {self.device}')

        if torch.cuda.is_available():
            torch.cuda.set_device(0)
            self.get_logger().info(f'GPU: {torch.cuda.get_device_name(0)}')
            self.get_logger().info(f'GPU Memory: {torch.cuda.get_device_properties(0).total_memory / 1024**3:.2f} GB')
            torch.cuda.empty_cache()

        # YOLO 모델 로드
        self.get_logger().info(f'Loading YOLO model: {model_path} on {self.device}...')
        self.model = YOLO(model_path)
        if torch.cuda.is_available():
            self.model.to(self.device)

        self.get_logger().info(f'✅ YOLO model loaded on {self.device}!')

        # GPU 메모리 확인
        if torch.cuda.is_available():
            allocated = torch.cuda.memory_allocated(0) / 1024**2
            reserved = torch.cuda.memory_reserved(0) / 1024**2
            self.get_logger().info(f'GPU Memory: {allocated:.1f}MB allocated, {reserved:.1f}MB reserved')

        # CvBridge
        self.bridge = CvBridge()

        # QoS 설정 (Best Effort로 실시간성 향상)
        qos_profile = QoSProfile(
            reliability=QoSReliabilityPolicy.BEST_EFFORT,
            history=QoSHistoryPolicy.KEEP_LAST,
            depth=1
        )

        # 퍼블리셔
        if self.use_compression:
            self.detection_image_pub = self.create_publisher(
                CompressedImage,
                '/camera/detections_image/compressed',
                qos_profile
            )
        else:
            self.detection_image_pub = self.create_publisher(
                Image,
                '/camera/detections_image',
                qos_profile
            )

        self.detection_pub = self.create_publisher(Detection2DArray, '/detections', 10)
        self.person_detected_pub = self.create_publisher(Bool, '/person_detected', 10)
        self.status_pub = self.create_publisher(String, '/yolo_status', 10)

        # 구독 (압축/비압축 이미지 자동 선택)
        self.image_sub = None
        self.compressed_sub = None

        # 먼저 압축 이미지 토픽을 시도
        self.compressed_sub = self.create_subscription(
            CompressedImage,
            '/camera/image_raw/compressed',
            self.compressed_image_callback,
            qos_profile
        )

        # 통계
        self.frame_count = 0
        self.person_detected_count = 0
        self.first_inference = True

        self.get_logger().info('YOLO Detector Node started!')
        self.get_logger().info(f'Subscribed to: /camera/image_raw/compressed')
        self.get_logger().info(f'Target class: {self.target_class} (person)')
        self.get_logger().info(f'Confidence threshold: {self.confidence_threshold}')
        self.get_logger().info(f'Publish rate: {self.publish_rate} Hz')

    def compressed_image_callback(self, msg):
        """압축 이미지 콜백"""
        # CompressedImage → OpenCV
        frame = self.bridge.compressed_imgmsg_to_cv2(msg, desired_encoding='bgr8')
        self.process_image(frame, msg.header)

    def image_callback(self, msg):
        """일반 이미지 콜백 (fallback)"""
        # Image → OpenCV
        frame = self.bridge.imgmsg_to_cv2(msg, desired_encoding='bgr8')
        self.process_image(frame, msg.header)

    def process_image(self, frame, header):
        """이미지 처리 및 YOLO 추론"""
        self.frame_count += 1

        # YOLO 추론
        results = self.model(
            frame,
            classes=[self.target_class],
            conf=self.confidence_threshold,
            verbose=False,
            device=0 if torch.cuda.is_available() else 'cpu',
            imgsz=416,
            stream=False
        )

        # 첫 추론 후 GPU 메모리 확인
        if self.first_inference and torch.cuda.is_available():
            allocated = torch.cuda.memory_allocated(0) / 1024**2
            reserved = torch.cuda.memory_reserved(0) / 1024**2
            self.get_logger().info(f'✅ First inference done! GPU Memory: {allocated:.1f}MB allocated, {reserved:.1f}MB reserved')
            self.first_inference = False

        # 탐지 결과 파싱
        detections_msg = Detection2DArray()
        detections_msg.header = header

        person_detected = False
        boxes = results[0].boxes

        for box in boxes:
            # Bounding box 정보
            x1, y1, x2, y2 = box.xyxy[0].cpu().numpy()
            confidence = float(box.conf[0])
            class_id = int(box.cls[0])

            # Detection2D 메시지 생성
            detection = Detection2D()
            detection.header = header

            # Bounding box center와 size
            detection.bbox.center.position.x = float((x1 + x2) / 2)
            detection.bbox.center.position.y = float((y1 + y2) / 2)
            detection.bbox.center.theta = 0.0
            detection.bbox.size_x = float(x2 - x1)
            detection.bbox.size_y = float(y2 - y1)

            # Hypothesis (클래스 ID와 신뢰도)
            hypothesis = ObjectHypothesisWithPose()
            hypothesis.hypothesis.class_id = str(class_id)
            hypothesis.hypothesis.score = confidence
            detection.results.append(hypothesis)

            detections_msg.detections.append(detection)
            person_detected = True

        # 탐지 결과 발행
        self.detection_pub.publish(detections_msg)

        # 사람 감지 여부 발행
        person_msg = Bool()
        person_msg.data = person_detected
        self.person_detected_pub.publish(person_msg)

        # 탐지 결과 이미지 발행
        annotated_frame = results[0].plot()

        if self.use_compression:
            detection_img_msg = self.bridge.cv2_to_compressed_imgmsg(annotated_frame, dst_format='jpeg')
        else:
            detection_img_msg = self.bridge.cv2_to_imgmsg(annotated_frame, encoding='bgr8')

        detection_img_msg.header = header
        self.detection_image_pub.publish(detection_img_msg)

        if person_detected:
            self.person_detected_count += 1

        # 상태 메시지
        person_count = len(boxes)
        status = f"Frame: {self.frame_count} | Persons: {person_count} | Total detected: {self.person_detected_count}"
        status_msg = String()
        status_msg.data = status
        self.status_pub.publish(status_msg)

        # 로그 (10초마다)
        log_interval = int(self.publish_rate * 10)  # 10초
        if self.frame_count % log_interval == 0:
            self.get_logger().info(status)

    def destroy_node(self):
        if torch.cuda.is_available():
            torch.cuda.empty_cache()
        super().destroy_node()

def main(args=None):
    rclpy.init(args=args)
    node = YOLODetectorNode()
    try:
        rclpy.spin(node)
    except KeyboardInterrupt:
        pass
    finally:
        node.destroy_node()
        rclpy.shutdown()

if __name__ == '__main__':
    main()
