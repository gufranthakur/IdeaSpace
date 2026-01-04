import cv2
import numpy as np
import time
import sys

from collections import deque
from server import Server
from hand_tracking_module import handDetector
from gesture_config import *
from maths import smooth_position, calculate_velocity, get_direction_from_velocity

# Parse debug flag
DEBUG = "--debug" in sys.argv

# Server connection
PORT = 65001
server = Server(PORT)

# Detector - right hand only
detector = handDetector(maxHands=1, detectionConfidence=0.5, trackConfidence=0.5)

# State
position_history = deque(maxlen=SMOOTHING_WINDOW)
prev_index_point = None
gesture_hold_frames = 0
last_command_time = 0
last_sent_command = None

# Camera settings
cap = cv2.VideoCapture(0)
cap.set(cv2.CAP_PROP_BUFFERSIZE, 1)
cap.set(cv2.CAP_PROP_FPS, TARGET_FPS)
cap.set(cv2.CAP_PROP_FRAME_WIDTH, 640)
cap.set(cv2.CAP_PROP_FRAME_HEIGHT, 480)
cap.set(cv2.CAP_PROP_AUTOFOCUS, 0)

PROCESS_WIDTH = 640
PROCESS_HEIGHT = 480


def send_command_throttled(command):
    global last_command_time, last_sent_command
    current_time = time.time()

    if command != last_sent_command or (current_time - last_command_time) >= COMMAND_SEND_INTERVAL:
        server.send_command(command)
        last_command_time = current_time
        last_sent_command = command
        return True
    return False


def is_pointing_gesture(lms):
    """Check if only index finger is up (pointing gesture)"""
    index_up = lms[INDEX_FINGER][2] < lms[INDEX_POINT][2]
    middle_curled = lms[MIDDLE_FINGER][2] > lms[MIDDLE_POINT][2]
    ring_curled = lms[RING_FINGER][2] > lms[RING_POINT][2]
    pinky_curled = lms[PINKY_FINGER][2] > lms[PINKY_POINT][2]

    return index_up and middle_curled and ring_curled and pinky_curled


prev_time = time.time()

try:
    while True:
        current_time = time.time()

        success, img = cap.read()
        if not success:
            continue

        img = cv2.flip(img, 1)
        h_full, w_full = img.shape[:2]

        img_small = cv2.resize(img, (PROCESS_WIDTH, PROCESS_HEIGHT))
        if DEBUG:
            img_small = detector.find_hands(img_small, False)
        else:
            detector.find_hands(img_small, False)

        scale_x = w_full / PROCESS_WIDTH
        scale_y = h_full / PROCESS_HEIGHT

        right_lms = []
        detected_action = None

        if detector.results and detector.results.multi_hand_landmarks:
            for hand_idx in range(len(detector.results.multi_hand_landmarks)):
                handedness = detector.get_handedness(hand_idx)

                if handedness == "Right":
                    lms_small = detector.find_position(img_small, hand_idx, draw=False)
                    right_lms = [[l[0], int(l[1] * scale_x), int(l[2] * scale_y)] for l in lms_small]

                    # Draw landmarks
                    if DEBUG:
                        for lm in right_lms:
                            cv2.circle(img, (lm[1], lm[2]), 5, (255, 0, 255), -1)

        # Detect pointing gesture for rotation
        if len(right_lms) > 0:
            if is_pointing_gesture(right_lms):
                gesture_hold_frames += 1

                if gesture_hold_frames >= STABLE_FRAMES:
                    index_x, index_y = right_lms[INDEX_FINGER][1], right_lms[INDEX_FINGER][2]
                    smoothed_pos = smooth_position(position_history, np, (index_x, index_y))

                    if prev_index_point is not None:
                        dx, dy = calculate_velocity(smoothed_pos, prev_index_point)
                        direction = get_direction_from_velocity(np, DEAD_ZONE, dx, dy)
                        if direction:
                            detected_action = f"ROTATE {direction}"

                    prev_index_point = smoothed_pos
            else:
                prev_index_point = None
                gesture_hold_frames = 0
                position_history.clear()
        else:
            prev_index_point = None
            gesture_hold_frames = 0
            position_history.clear()

        # Send command
        if detected_action:
            send_command_throttled(detected_action)
            if DEBUG:
                cv2.putText(img, detected_action, (50, 60),
                            cv2.FONT_HERSHEY_SIMPLEX, 0.7, (0, 255, 0), 2)
        else:
            if last_sent_command is not None and last_sent_command != "NULL":
                send_command_throttled("NULL")

        # Debug display
        if DEBUG:
            cv2.putText(img, "ROTATOR", (10, 30),
                        cv2.FONT_HERSHEY_SIMPLEX, 0.7, (255, 255, 0), 2)

            fps = 1.0 / (time.time() - prev_time)
            prev_time = time.time()
            cv2.putText(img, f"FPS: {int(fps)}", (img.shape[1] - 100, 30),
                        cv2.FONT_HERSHEY_SIMPLEX, 0.6, (0, 255, 0), 2)

            cv2.namedWindow("Rotator", cv2.WINDOW_AUTOSIZE)
            cv2.imshow("Rotator", img)

        # Frame timing
        elapsed = time.time() - current_time
        if elapsed < FRAME_TIME:
            time.sleep(FRAME_TIME - elapsed)

        if DEBUG and cv2.waitKey(1) & 0xFF == ord('q'):
            break
        elif not DEBUG:
            time.sleep(0.01)

finally:
    send_command_throttled("NULL")
    server.close()
    cap.release()
    if DEBUG:
        cv2.destroyAllWindows()
