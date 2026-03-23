#!/usr/bin/env python3
"""
YOLO Object Detection Launch File
- camera_node: 카메라 영상 발행
- yolo_detector_node: YOLO 객체 탐지
- safety_controller_node: 사람 감지 시 긴급정지 신호 발행
"""
from launch import LaunchDescription
from launch_ros.actions import Node

def generate_launch_description():
    return LaunchDescription([
        # Camera Node
        Node(
            package='object_detection',
            executable='camera_node',
            name='camera_node',
            output='screen',
            parameters=[{
                'device_id': 0,
                'frame_width': 640,
                'frame_height': 480,
                'publish_rate': 15.0,  # 15Hz (YOLO와 동기화)
                'use_compression': True,
            }]
        ),

        # YOLO Detector Node
        Node(
            package='object_detection',
            executable='yolo_detector_node',
            name='yolo_detector_node',
            output='screen',
            parameters=[{
                'model_path': 'yolov8n.pt',
                'confidence_threshold': 0.5,
                'target_class': 0,  # person
                'publish_rate': 15.0,
                'use_compression': True,
            }]
        ),

        # Safety Controller Node (YOLO + LiDAR Fusion)
        Node(
            package='object_detection',
            executable='safety_controller_node',
            name='safety_controller_node',
            output='screen',
            parameters=[{
                'stop_on_person_detected': True,
                'safety_distance': 1.0,  # 긴급정지 거리
                'recovery_distance': 1.2,  # 복귀 거리 (hysteresis)
                'front_angle_range': 10.0,  # 정면 범위 ±10도
                'tf_rotation_offset': -90.0,  # TF 회전 보정
            }]
        ),
    ])
