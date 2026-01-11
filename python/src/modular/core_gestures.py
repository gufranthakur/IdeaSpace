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
from swipe_utils import detect_swipe_gesture
from maths import smooth_position, calculate_velocity, get_direction_from_velocity

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

# Rotation state
position_history = deque(maxlen=SMOOTHING_WINDOW)
prev_index_point = None
rotation_gesture_hold_frames = 0
rotation_active = False

# Hysteresis thresholds for stable rotation
ROTATION_ACTIVATE_THRESHOLD = 40
ROTATION_DEACTIVATE_THRESHOLD = 55
ROTATION_FRAMES_THRESHOLD = 15

# Command throttling
rotate_last_command_time = 0
rotate_last_sent_command = None
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


def is_rotation_gesture(lms, img, rotation_currently_active):
    """Check if thumb and index are pinched together for rotation - with hysteresis"""
    h, w = img.shape[:2]

    thumb_x, thumb_y = lms[THUMB][1], lms[THUMB][2]
    index_x, index_y = lms[INDEX_FINGER][1], lms[INDEX_FINGER][2]

    pinch_distance = math.sqrt((thumb_x - index_x)**2 + (thumb_y - index_y)**2)

    if rotation_currently_active:
        threshold = ROTATION_DEACTIVATE_THRESHOLD
    else:
        threshold = ROTATION_ACTIVATE_THRESHOLD

    return pinch_distance <= threshold


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
        rotation_action = None
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
                if DEBUG_MODE and rotation_active:
                    print("⚠️ ROTATION STOPPED: Flick detected")

            both_hands_detected = len(left_lms) > 0 and len(right_lms) > 0

            can_check_rotation = not right_action and (not right_flick_gesture.is_active() or rotation_active) and not both_hands_detected

            if DEBUG_MODE and both_hands_detected and rotation_active:
                print("⛔ ROTATION BLOCKED: Both hands detected")

            if can_check_rotation:
                is_rot_gesture = is_rotation_gesture(right_lms, img, rotation_active)

                if is_rot_gesture:
                    rotation_gesture_hold_frames += 1

                    if rotation_gesture_hold_frames >= ROTATION_FRAMES_THRESHOLD:
                        if not rotation_active:
                            rotation_active = True
                            if DEBUG_MODE:
                                print("✅ ROTATION ACTIVATED")

                        thumb_x, thumb_y = right_lms[THUMB][1], right_lms[THUMB][2]
                        index_x, index_y = right_lms[INDEX_FINGER][1], right_lms[INDEX_FINGER][2]
                        midpoint_x = (thumb_x + index_x) // 2
                        midpoint_y = (thumb_y + index_y) // 2

                        smoothed_pos = smooth_position(position_history, np, (midpoint_x, midpoint_y))

                        if prev_index_point is not None:
                            dx, dy = calculate_velocity(smoothed_pos, prev_index_point)
                            direction = get_direction_from_velocity(np, DEAD_ZONE, dx, dy)
                            if direction:
                                rotation_action = f"ROTATE {direction}"

                        prev_index_point = smoothed_pos
                else:
                    if rotation_active and DEBUG_MODE:
                        print("❌ ROTATION STOPPED: Pinch lost")

                    prev_index_point = None
                    rotation_gesture_hold_frames = 0
                    rotation_active = False
                    position_history.clear()
            else:
                if rotation_active and DEBUG_MODE:
                    if both_hands_detected:
                        print("❌ ROTATION STOPPED: Both hands")
                    else:
                        print("❌ ROTATION STOPPED: Other gesture blocking")

                prev_index_point = None
                rotation_gesture_hold_frames = 0
                rotation_active = False
                position_history.clear()
        else:
            prev_index_point = None
            rotation_gesture_hold_frames = 0
            rotation_active = False
            position_history.clear()

        # Process ZOOM gesture
        if len(left_lms) > 0 and len(right_lms) > 0:
            left_index_extended = left_lms[INDEX_FINGER][2] < left_lms[INDEX_POINT][2]
            right_index_extended = right_lms[INDEX_FINGER][2] < right_lms[INDEX_POINT][2]

            if left_index_extended and right_index_extended:
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

        if rotation_action:
            rotate_last_command_time, rotate_last_sent_command, _ = send_command_throttled(
                rotation_action, rotate_last_command_time, rotate_last_sent_command
            )
        else:
            if rotate_last_sent_command is not None and rotate_last_sent_command != "NULL":
                rotate_last_command_time, rotate_last_sent_command, _ = send_command_throttled(
                    "NULL", rotate_last_command_time, rotate_last_sent_command
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
            elif rotation_action:
                action_text = f"ROTATE: {rotation_action}"
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
