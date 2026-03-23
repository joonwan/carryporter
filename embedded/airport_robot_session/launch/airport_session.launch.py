#!/usr/bin/env python3
"""
공항 로봇 세션 런치 파일
- 모든 필요한 노드를 함께 실행
"""

from launch import LaunchDescription
from launch_ros.actions import Node
from launch.actions import DeclareLaunchArgument, IncludeLaunchDescription
from launch.substitutions import LaunchConfiguration
from launch.launch_description_sources import PythonLaunchDescriptionSource
import os
from ament_index_python.packages import get_package_share_directory


def generate_launch_description():
    # 모터 제어 노드 (robot_control)
    motor_control_node = Node(
        package='robot_control',
        executable='motor_control_node',
        name='motor_control_node',
        output='screen'
    )

    # 오도메트리 노드 (time_based_control)
    odometry_node = Node(
        package='time_based_control',
        executable='odometry_node',
        name='odometry_node',
        output='screen'
    )

    # 정밀 이동 노드 (time_based_control)
    precise_motion_node = Node(
        package='time_based_control',
        executable='precise_motion_node',
        name='precise_motion_node',
        output='screen'
    )

    # 세션 컨트롤러 노드 (airport_robot_session)
    session_controller_node = Node(
        package='airport_robot_session',
        executable='session_controller_node',
        name='session_controller_node',
        output='screen',
        prefix='xterm -e'  # 별도 터미널에서 실행 (대화형 입력용)
    )

    return LaunchDescription([
        motor_control_node,
        odometry_node,
        precise_motion_node,
        session_controller_node,
    ])
