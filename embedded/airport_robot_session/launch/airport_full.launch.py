#!/usr/bin/env python3
"""
공항 로봇 전체 시스템 런치 파일
- 모터 제어
- 오도메트리
- 정밀 이동
- MQTT 컨트롤러
- LiDAR (옵션)
- YOLO 감지 (옵션)
"""
from launch import LaunchDescription
from launch.actions import DeclareLaunchArgument, IncludeLaunchDescription
from launch.conditions import IfCondition
from launch.substitutions import LaunchConfiguration
from launch.launch_description_sources import PythonLaunchDescriptionSource
from launch_ros.actions import Node
import os
from ament_index_python.packages import get_package_share_directory


def generate_launch_description():
    # 런치 인자
    use_yolo_arg = DeclareLaunchArgument(
        'use_yolo',
        default_value='false',
        description='Enable YOLO person detection and LiDAR'
    )

    mqtt_broker_arg = DeclareLaunchArgument(
        'mqtt_broker',
        default_value='localhost',
        description='MQTT broker address'
    )

    mqtt_port_arg = DeclareLaunchArgument(
        'mqtt_port',
        default_value='1883',
        description='MQTT broker port'
    )

    # 설정 값
    use_yolo = LaunchConfiguration('use_yolo')
    mqtt_broker = LaunchConfiguration('mqtt_broker')
    mqtt_port = LaunchConfiguration('mqtt_port')

    # 1. 모터 제어 노드
    motor_control_node = Node(
        package='robot_control',
        executable='motor_control_node',
        name='motor_control_node',
        output='screen'
    )

    # 2. 오도메트리 노드
    odometry_node = Node(
        package='time_based_control',
        executable='odometry_node',
        name='odometry_node',
        output='screen'
    )

    # 3. 정밀 이동 노드
    precise_motion_node = Node(
        package='time_based_control',
        executable='precise_motion_node',
        name='precise_motion_node',
        output='screen'
    )

    # 4. MQTT ROS2 노드 (test.py 통합 버전)
    robot_mqtt_node = Node(
        package='airport_robot_session',
        executable='robot_mqtt_node',
        name='robot_mqtt_node',
        output='screen',
        parameters=[{
            'mqtt_broker': mqtt_broker,
            'mqtt_port': mqtt_port,
        }]
    )

    # 5. LiDAR (YOLO 사용 시)
    ydlidar_share = get_package_share_directory('ydlidar_ros2_driver')
    lidar_launch = IncludeLaunchDescription(
        PythonLaunchDescriptionSource(
            os.path.join(ydlidar_share, 'launch', 'ydlidar_launch.py')
        ),
        condition=IfCondition(use_yolo)
    )

    # 6. YOLO 감지 시스템 (YOLO 사용 시)
    object_detection_share = get_package_share_directory('object_detection')
    yolo_launch = IncludeLaunchDescription(
        PythonLaunchDescriptionSource(
            os.path.join(object_detection_share, 'launch', 'yolo_detection.launch.py')
        ),
        condition=IfCondition(use_yolo)
    )

    return LaunchDescription([
        # 런치 인자
        use_yolo_arg,
        mqtt_broker_arg,
        mqtt_port_arg,

        # 노드들
        motor_control_node,
        odometry_node,
        precise_motion_node,
        robot_mqtt_node,

        # 조건부 런치
        lidar_launch,
        yolo_launch,
    ])
