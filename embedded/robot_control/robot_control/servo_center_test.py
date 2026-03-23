#!/usr/bin/env python3
"""
서보모터 중앙값 찾기 테스트 스크립트

키보드로 서보 각도를 실시간 조절하여 바퀴가 정면을 향하는 중앙값을 찾습니다.

조작법:
  a / d     : 1도씩 좌/우 조절
  q / e     : 5도씩 좌/우 조절
  z / c     : 0.5도씩 좌/우 미세 조절
  r         : 현재 시작값(110)으로 리셋
  s         : 현재 각도를 중앙값으로 확정 (출력)
  Ctrl+C    : 종료
"""

import os
import sys
import time
import termios
import tty


def get_key():
    """키보드 한 글자 읽기 (non-blocking 아닌 blocking)"""
    fd = sys.stdin.fileno()
    old_settings = termios.tcgetattr(fd)
    try:
        tty.setraw(fd)
        ch = sys.stdin.read(1)
    finally:
        termios.tcsetattr(fd, termios.TCSADRAIN, old_settings)
    return ch


def main():
    # ── 설정 ──
    I2C_BUS = 7
    PCA_ADDRESS = 0x60
    SERVO_CHANNEL = 0
    START_ANGLE = 110.0  # 현재 빌드된 중앙값

    SERVO_MIN = 30
    SERVO_MAX = 180

    print("=" * 50)
    print("  서보모터 중앙값 테스트")
    print("=" * 50)
    print(f"  I2C Bus: {I2C_BUS}, PCA9685: 0x{PCA_ADDRESS:02X}")
    print(f"  서보 채널: {SERVO_CHANNEL}")
    print(f"  시작 각도: {START_ANGLE}")
    print(f"  허용 범위: {SERVO_MIN} ~ {SERVO_MAX}")
    print("-" * 50)

    # ── 하드웨어 초기화 ──
    os.environ['BLINKA_I2C_BUS'] = str(I2C_BUS)

    try:
        from adafruit_servokit import ServoKit
        import busio
        import board

        i2c = busio.I2C(board.SCL, board.SDA)
        kit = ServoKit(channels=16, i2c=i2c, address=PCA_ADDRESS)
        print("[OK] 서보 초기화 성공")
    except Exception as e:
        print(f"[ERROR] 서보 초기화 실패: {e}")
        sys.exit(1)

    # ── 초기 위치 설정 ──
    angle = START_ANGLE
    kit.servo[SERVO_CHANNEL].angle = angle

    print()
    print("조작법:")
    print("  a / d   : 1도씩 좌/우")
    print("  q / e   : 5도씩 좌/우")
    print("  z / c   : 0.5도씩 미세 조절")
    print("  r       : 리셋 (110)")
    print("  s       : 현재 값 확정 출력")
    print("  Ctrl+C  : 종료")
    print("-" * 50)
    print(f"  >>> 현재 각도: {angle:.1f}")

    try:
        while True:
            key = get_key()
            prev = angle

            if key == 'a':
                angle -= 1.0
            elif key == 'd':
                angle += 1.0
            elif key == 'q':
                angle -= 5.0
            elif key == 'e':
                angle += 5.0
            elif key == 'z':
                angle -= 0.5
            elif key == 'c':
                angle += 0.5
            elif key == 'r':
                angle = START_ANGLE
                print(f"\r  [RESET] 각도: {angle:.1f}              ")
            elif key == 's':
                print(f"\n{'=' * 50}")
                print(f"  확정된 중앙값: {angle:.1f}")
                print(f"  config.py 에 servo_center = {angle:.1f} 로 수정하세요")
                print(f"{'=' * 50}")
                continue
            elif key == '\x03':  # Ctrl+C
                raise KeyboardInterrupt
            else:
                continue

            # 범위 제한
            angle = max(SERVO_MIN, min(SERVO_MAX, angle))

            # 서보 이동
            kit.servo[SERVO_CHANNEL].angle = angle
            print(f"\r  >>> 현재 각도: {angle:.1f}  (변화: {angle - prev:+.1f})     ", end='', flush=True)

    except KeyboardInterrupt:
        print(f"\n\n종료. 마지막 각도: {angle:.1f}")
        # 서보를 마지막 위치에 유지
        kit.servo[SERVO_CHANNEL].angle = angle


if __name__ == '__main__':
    main()
