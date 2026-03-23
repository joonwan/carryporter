from setuptools import setup
import os
from glob import glob

package_name = 'robot_control'

setup(
    name=package_name,
    version='0.0.1',
    packages=[package_name],
    data_files=[
        ('share/ament_index/resource_index/packages',
            ['resource/' + package_name]),
        ('share/' + package_name, ['package.xml']),
        (os.path.join('share', package_name, 'launch'), glob('launch/*.launch.py')),
    ],
    install_requires=['setuptools'],
    zip_safe=True,
    maintainer='e101',
    maintainer_email='user@todo.todo',
    description='Robot motor control for autonomous car',
    license='MIT',
    entry_points={
        'console_scripts': [
            'motor_control_node = robot_control.motor_control_node:main',
            'line_follower_node = robot_control.line_follower_node:main',
        ],
    },
)
