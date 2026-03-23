#!/usr/bin/env python3
"""
카메라 ROS2 노드
- 카메라 영상을 sensor_msgs/Image 또는 CompressedImage로 발행
- OpenCV로 캡처
"""
import rclpy
from rclpy.node import Node
from rclpy.qos import QoSProfile, QoSReliabilityPolicy, QoSHistoryPolicy
from sensor_msgs.msg import Image, CompressedImage
from cv_bridge import CvBridge
import cv2


class CameraNode(Node):
    def __init__(self):
        super().__init__('camera_node')

        # 파라미터
        self.declare_parameter('device_id', 0)
        self.declare_parameter('frame_width', 640)
        self.declare_parameter('frame_height', 480)
        self.declare_parameter('publish_rate', 30.0)  # Hz
        self.declare_parameter('use_compression', True)  # JPEG 압축 사용 여부

        device_id = self.get_parameter('device_id').value
        width = self.get_parameter('frame_width').value
        height = self.get_parameter('frame_height').value
        self.publish_rate = self.get_parameter('publish_rate').value
        self.use_compression = self.get_parameter('use_compression').value

        # 카메라 초기화
        self.cap = cv2.VideoCapture(device_id)
        self.cap.set(cv2.CAP_PROP_FRAME_WIDTH, width)
        self.cap.set(cv2.CAP_PROP_FRAME_HEIGHT, height)

        if not self.cap.isOpened():
            self.get_logger().error(f'Failed to open camera {device_id}')
            raise RuntimeError('Camera not available')

        # CvBridge
        self.bridge = CvBridge()

        # QoS 설정 (Best Effort로 실시간성 향상)
        qos_profile = QoSProfile(
            reliability=QoSReliabilityPolicy.BEST_EFFORT,
            history=QoSHistoryPolicy.KEEP_LAST,
            depth=1
        )

        # 퍼블리셔 (압축/비압축)
        if self.use_compression:
            self.image_pub = self.create_publisher(
                CompressedImage,
                '/camera/image_raw/compressed',
                qos_profile
            )
            topic_name = '/camera/image_raw/compressed (JPEG)'
        else:
            self.image_pub = self.create_publisher(
                Image,
                '/camera/image_raw',
                10
            )
            topic_name = '/camera/image_raw'

        # 타이머
        period = 1.0 / self.publish_rate
        self.timer = self.create_timer(period, self.publish_image)

        self.get_logger().info(f'Camera node started at {self.publish_rate}Hz')
        self.get_logger().info(f'Publishing to {topic_name}')
        self.get_logger().info(f'Resolution: {width}x{height}')

    def publish_image(self):
        ret, frame = self.cap.read()
        if not ret:
            self.get_logger().warn('Failed to read frame')
            return

        # ROS 메시지 변환
        if self.use_compression:
            msg = self.bridge.cv2_to_compressed_imgmsg(frame, dst_format='jpeg')
        else:
            msg = self.bridge.cv2_to_imgmsg(frame, encoding='bgr8')

        msg.header.stamp = self.get_clock().now().to_msg()
        msg.header.frame_id = 'camera_link'

        self.image_pub.publish(msg)

    def destroy_node(self):
        self.cap.release()
        super().destroy_node()


def main(args=None):
    rclpy.init(args=args)
    node = CameraNode()
    try:
        rclpy.spin(node)
    except KeyboardInterrupt:
        pass
    finally:
        node.destroy_node()
        rclpy.shutdown()


if __name__ == '__main__':
    main()
