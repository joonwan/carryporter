#!/usr/bin/env python3
"""
ROS2 Launch File: Line Follower System
한 번에 모터 제어 노드와 라인 트레이서 노드를 실행합니다.
"""

from launch import LaunchDescription
from launch_ros.actions import Node

def generate_launch_description():
    return LaunchDescription([
        # 1. 모터 제어 노드 (근육) - I2C 통신 전담
        Node(
            package='robot_control',
            executable='motor_control_node',
            name='motor_control_node',
            output='screen',
            emulate_tty=True,
        ),

        # 2. 라인 트레이서 노드 (두뇌) - 센서 읽고 명령 발행
        Node(
            package='robot_control',
            executable='line_follower_node',
            name='line_follower_node',
            output='screen',
            emulate_tty=True,
        ),
    ])
