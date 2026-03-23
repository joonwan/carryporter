from setuptools import find_packages, setup
import os
from glob import glob

package_name = 'object_detection'

setup(
    name=package_name,
    version='0.0.0',
    packages=find_packages(exclude=['test']),
    data_files=[
        ('share/ament_index/resource_index/packages',
            ['resource/' + package_name]),
        ('share/' + package_name, ['package.xml']),
        (os.path.join('share', package_name, 'launch'), glob('launch/*.launch.py')),
    ],
    install_requires=['setuptools'],
    zip_safe=True,
    maintainer='e101',
    maintainer_email='e101@todo.todo',
    description='TODO: Package description',
    license='TODO: License declaration',
    extras_require={
        'test': [
            'pytest',
        ],
    },
    entry_points={
        'console_scripts': [
            'camera_node = object_detection.camera_node:main',
            'yolo_detector_node = object_detection.yolo_detector_node:main',
            'safety_controller_node = object_detection.safety_controller_node:main',
        ],
    },
)
