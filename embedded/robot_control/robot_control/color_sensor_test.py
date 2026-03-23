#!/usr/bin/env python3
"""
TCS34725 컬러 센서 실시간 테스트
RGB 값을 0.5초마다 출력하여 색상 인식 기준값을 확인
Ctrl+C로 종료
"""
import os
import time

os.environ['BLINKA_I2C_BUS'] = '7'
import board
import busio
import adafruit_tcs34725

def main():
    i2c = busio.I2C(board.SCL, board.SDA)
    sensor = adafruit_tcs34725.TCS34725(i2c)
    sensor.integration_time = 50
    sensor.gain = 4

    print("=== TCS34725 Color Sensor Test ===")
    print("Ctrl+C to quit\n")
    print(f"{'R':>5} {'G':>5} {'B':>5} {'Sum':>5}  판정")
    print("-" * 40)

    try:
        while True:
            r, g, b = sensor.color_rgb_bytes
            total = r + g + b

            # 현재 line_follower_node.py와 동일한 판정 로직
            if r > 70:
                label = "RED"
            elif g > 35 and g > r * 3.0:
                label = "GREEN"
            else:
                label = "-"

            print(f"{r:>5} {g:>5} {b:>5} {total:>5}  {label}")
            time.sleep(0.5)
    except KeyboardInterrupt:
        print("\nDone.")

if __name__ == '__main__':
    main()
