from setuptools import setup
import os
from glob import glob

package_name = 'airport_robot_session'

setup(
    name=package_name,
    version='0.0.1',
    packages=[package_name],
    data_files=[
        ('share/ament_index/resource_index/packages',
            ['resource/' + package_name]),
        ('share/' + package_name, ['package.xml']),
        ('share/' + package_name + '/launch', glob('launch/*.py')),
    ],
    install_requires=['setuptools'],
    zip_safe=True,
    maintainer='e101',
    maintainer_email='e101@todo.todo',
    description='Airport robot session controller for luggage storage',
    license='MIT',
    extras_require={
        'test': ['pytest'],
    },
    entry_points={
        'console_scripts': [
            'session_controller_node = airport_robot_session.session_controller_node:main',
            'mqtt_controller_node = airport_robot_session.mqtt_controller_node:main',
            'robot_mqtt_node = airport_robot_session.robot_mqtt_node:main',
            'line_follower_node = airport_robot_session.line_follower_node:main',  # ← 추가!
        ],
    },
)
