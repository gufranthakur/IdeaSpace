import cv2
import time
import sys
import math
import argparse
import numpy as np
from collections import deque

from server import Server
from hand_tracking_module import handDetector
from gesture_config import *
from flick_gesture import FlickGesture
from drag_gesture import DragGesture
from swipe_utils import detect_swipe_gesture

# Parse arguments
parser = argparse.ArgumentParser(description='Unified Gesture System')
parser.add_argument('--debug', action='store_true', help='Enable debug mode (camera view, landmarks, FPS)')
args = parser.parse_args()

DEBUG_MODE = args.debug

# Single server for all commands
PORT = 65000
server = Server(PORT)

# Detector for both hands
detector = handDetector(maxHands=2, detectionConfidence=0.8, trackConfidence=0.8)

# Gesture detectors
left_flick_gesture = FlickGesture(return_command="REMOVE")
right_flick_gesture = FlickGesture(return_command="ANIMATE")
right_drag_gesture = DragGesture()

# Command throttling
core_left_last_command_time = 0
core_left_last_sent_command = None
core_right_last_command_time = 0
core_right_last_sent_command = None
zoom_last_command_time = 0
zoom_last_sent_command = None

# Camera settings
cap = cv2.VideoCapture(0)
cap.set(cv2.CAP_PROP_BUFFERSIZE, 1)
cap.set(cv2.CAP_PROP_FPS, TARGET_FPS)
cap.set(cv2.CAP_PROP_FRAME_WIDTH, 640)
cap.set(cv2.CAP_PROP_FRAME_HEIGHT, 480)
cap.set(cv2.CAP_PROP_AUTOFOCUS, 0)

PROCESS_WIDTH = 640
PROCESS_HEIGHT = 480


def send_command_throttled(command, last_time, last_command, interval=COMMAND_SEND_INTERVAL):
    """Send command with throttling to prevent spam"""
    current_time = time.time()

    if command != last_command or (current_time - last_time) >= interval:
        server.send_command(command)
        return current_time, command, True
    return last_time, last_command, False


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
        if DEBUG_MODE:
            img_small = detector.find_hands(img_small, False)
        else:
            detector.find_hands(img_small, False)

        scale_x = w_full / PROCESS_WIDTH
        scale_y = h_full / PROCESS_HEIGHT

        left_lms = []
        right_lms = []
        left_action = None
        right_action = None
        zoom_action = None

        # Process all detected hands
        if detector.results and detector.results.multi_hand_landmarks:
            for hand_idx in range(len(detector.results.multi_hand_landmarks)):
                handedness = detector.get_handedness(hand_idx)
                lms_small = detector.find_position(img_small, hand_idx, draw=False)
                lms_scaled = [[l[0], int(l[1] * scale_x), int(l[2] * scale_y)] for l in lms_small]

                if handedness == "Left":
                    left_lms = lms_scaled
                    if DEBUG_MODE:
                        for lm in left_lms:
                            cv2.circle(img, (lm[1], lm[2]), 5, (255, 0, 255), -1)

                elif handedness == "Right":
                    right_lms = lms_scaled
                    if DEBUG_MODE:
                        for lm in right_lms:
                            cv2.circle(img, (lm[1], lm[2]), 5, (0, 255, 255), -1)

        # Process LEFT hand gestures
        if len(left_lms) > 0:
            flick_action = left_flick_gesture.detect(left_lms, img)
            if flick_action:
                left_action = flick_action

            if not left_action and not left_flick_gesture.is_active():
                swipe_action = detect_swipe_gesture(left_lms, img)
                if swipe_action:
                    left_action = swipe_action

        # Process RIGHT hand gestures
        if len(right_lms) > 0:
            flick_action = right_flick_gesture.detect(right_lms, img)
            if flick_action:
                right_action = flick_action

            # Check drag gesture if flick is not active
            if not right_action and not right_flick_gesture.is_active():
                drag_action = right_drag_gesture.detect(right_lms, img)
                if drag_action:
                    right_action = drag_action

        # Process ZOOM gesture
        if len(left_lms) > 0 and len(right_lms) > 0:
            # Check if only index finger is extended on LEFT hand
            left_index_extended = left_lms[INDEX_FINGER][2] < left_lms[INDEX_POINT][2] - 20
            left_middle_curled = left_lms[MIDDLE_FINGER][2] > left_lms[MIDDLE_POINT][2] - 10
            left_ring_curled = left_lms[RING_FINGER][2] > left_lms[RING_POINT][2] - 10
            left_pinky_curled = left_lms[PINKY_FINGER][2] > left_lms[PINKY_POINT][2] - 10

            left_valid_pose = (left_index_extended and left_middle_curled and
                              left_ring_curled and left_pinky_curled)

            # Check if only index finger is extended on RIGHT hand
            right_index_extended = right_lms[INDEX_FINGER][2] < right_lms[INDEX_POINT][2] - 20
            right_middle_curled = right_lms[MIDDLE_FINGER][2] > right_lms[MIDDLE_POINT][2] - 10
            right_ring_curled = right_lms[RING_FINGER][2] > right_lms[RING_POINT][2] - 10
            right_pinky_curled = right_lms[PINKY_FINGER][2] > right_lms[PINKY_POINT][2] - 10

            right_valid_pose = (right_index_extended and right_middle_curled and
                               right_ring_curled and right_pinky_curled)

            if left_valid_pose and right_valid_pose:
                left_index_x, left_index_y = left_lms[INDEX_FINGER][1], left_lms[INDEX_FINGER][2]
                right_index_x, right_index_y = right_lms[INDEX_FINGER][1], right_lms[INDEX_FINGER][2]

                distance = math.sqrt(
                    (right_index_x - left_index_x)**2 +
                    (right_index_y - left_index_y)**2
                ) / w_full

                ZOOM_OUT_THRESHOLD = 0.15
                ZOOM_IN_THRESHOLD = 0.35

                if distance < ZOOM_OUT_THRESHOLD:
                    zoom_action = "ZOOM OUT"
                elif distance > ZOOM_IN_THRESHOLD:
                    zoom_action = "ZOOM IN"

        # Send commands
        if left_action:
            core_left_last_command_time, core_left_last_sent_command, _ = send_command_throttled(
                left_action, core_left_last_command_time, core_left_last_sent_command
            )
        else:
            if core_left_last_sent_command is not None and core_left_last_sent_command != "NULL":
                core_left_last_command_time, core_left_last_sent_command, _ = send_command_throttled(
                    "NULL", core_left_last_command_time, core_left_last_sent_command
                )

        if right_action:
            core_right_last_command_time, core_right_last_sent_command, _ = send_command_throttled(
                right_action, core_right_last_command_time, core_right_last_sent_command
            )
        else:
            if core_right_last_sent_command is not None and core_right_last_sent_command != "NULL":
                core_right_last_command_time, core_right_last_sent_command, _ = send_command_throttled(
                    "NULL", core_right_last_command_time, core_right_last_sent_command
                )

        if zoom_action:
            zoom_last_command_time, zoom_last_sent_command, _ = send_command_throttled(
                zoom_action, zoom_last_command_time, zoom_last_sent_command, interval=0.1
            )
        else:
            if zoom_last_sent_command is not None and zoom_last_sent_command != "NULL":
                zoom_last_command_time, zoom_last_sent_command, _ = send_command_throttled(
                    "NULL", zoom_last_command_time, zoom_last_sent_command, interval=0.1
                )

        # Debug display - MINIMAL
        if DEBUG_MODE:
            # Show current action
            action_text = None
            if left_action:
                action_text = f"LEFT: {left_action}"
            elif right_action:
                action_text = f"RIGHT: {right_action}"
            elif zoom_action:
                action_text = f"ZOOM: {zoom_action}"

            if action_text:
                cv2.putText(img, action_text, (10, 30),
                           cv2.FONT_HERSHEY_SIMPLEX, 0.8, (0, 255, 0), 2)

            cv2.namedWindow("Gestures", cv2.WINDOW_AUTOSIZE)
            cv2.imshow("Gestures", img)

        prev_time = time.time()

        elapsed = time.time() - current_time
        if elapsed < FRAME_TIME:
            time.sleep(FRAME_TIME - elapsed)

        if DEBUG_MODE and cv2.waitKey(1) & 0xFF == ord('q'):
            break
        elif not DEBUG_MODE:
            time.sleep(0.01)

finally:
    send_command_throttled("NULL", 0, None)
    server.close()
    cap.release()
    if DEBUG_MODE:
        cv2.destroyAllWindows()
