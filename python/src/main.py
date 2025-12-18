import cv2
import time
import hand_tracking_module as htm
import server as server
from collections import deque
import numpy as np

from gesture_config import *
from maths import *
from swipe_utils import *
from remove_gesture import RemoveGesture
from drag_gesture import DragGesture
from split_gesture import SplitGesture
from zoom_gesture import ZoomGesture

CURRENT_MODE = VIEW_MODE

# Initialize gesture detectors
remove_gesture = RemoveGesture()
drag_gesture = DragGesture()
split_gesture = SplitGesture()
zoom_gesture = ZoomGesture()

# Runtime variables
position_history = deque(maxlen=SMOOTHING_WINDOW)
last_command_time = 0
last_sent_command = None

cap = cv2.VideoCapture(0)
cap.set(cv2.CAP_PROP_BUFFERSIZE, 1)
cap.set(cv2.CAP_PROP_FPS, TARGET_FPS)
detector = htm.handDetector(maxHands=2, detectionConfidence=0.7, trackConfidence=0.5)

# State tracking
current_active_action = None
camera_active = False
prev_index_point = None
gesture_hold_frames = 0


def send_command_throttled(command):
    """Send commands with throttling to reduce network overhead"""
    global last_command_time, last_sent_command
    current_time = time.time()

    if command != last_sent_command or (current_time - last_command_time) >= COMMAND_SEND_INTERVAL:
        server.send_command(command)
        last_command_time = current_time
        last_sent_command = command
        return True
    return False


prev_time = time.time()

while True:
    current_time = time.time()

    success, img = cap.read()
    if not success:
        continue

    img = cv2.flip(img, 1)
    img = detector.find_hands(img, True)

    # Get positions for both hands
    left_lms = []
    right_lms = []

    if detector.results and detector.results.multi_hand_landmarks:
        for hand_idx in range(len(detector.results.multi_hand_landmarks)):
            handedness = detector.get_handedness(hand_idx)
            lms = detector.find_position(img, hand_idx, draw=False)

            if handedness == "Left":
                left_lms = lms
            elif handedness == "Right":
                right_lms = lms

    detected_action = None

    # Show detected hands
    hands_present = []
    if len(left_lms) > 0:
        hands_present.append("Left")
    if len(right_lms) > 0:
        hands_present.append("Right")

    if hands_present:
        cv2.putText(img, f"Hands: {', '.join(hands_present)}", (10, 30),
                    cv2.FONT_HERSHEY_SIMPLEX, 0.7, (0, 255, 0), 2)

    # BOTH HANDS gestures - Zoom (check first, highest priority)
    if len(left_lms) > 0 and len(right_lms) > 0 and not detected_action:
        zoom_action = zoom_gesture.detect(left_lms, right_lms, img)
        if zoom_action:
            detected_action = zoom_action

    # LEFT HAND gestures - Swipe and Remove (only if zoom not active)
    if len(left_lms) > 0 and not detected_action and not zoom_gesture.is_active():
        remove_action = remove_gesture.detect(left_lms, img)
        if remove_action:
            detected_action = remove_action
        elif not remove_gesture.is_active():
            swipe_action = detect_swipe_gesture(left_lms, img)
            if swipe_action:
                detected_action = swipe_action

    # RIGHT HAND gestures - Split (snap), Drag, Camera Look (only if zoom not active)
    if len(right_lms) > 0 and not detected_action and not zoom_gesture.is_active():

        # PRIORITY 1: Check split gesture (snap) first
        split_action = split_gesture.detect(right_lms, img)
        if split_action:
            detected_action = split_action

        # PRIORITY 2: Other right hand gestures (only if split not active)
        if not detected_action and not split_gesture.is_active():
            if CURRENT_MODE == VIEW_MODE:
                # Check drag gesture
                drag_action = drag_gesture.detect(right_lms, img)
                if drag_action:
                    detected_action = drag_action
                elif not drag_gesture.is_active():
                    # Camera look gesture
                    index_up = right_lms[INDEX_FINGER][2] < right_lms[INDEX_POINT][2]
                    middle_curled = right_lms[MIDDLE_FINGER][2] > right_lms[MIDDLE_POINT][2]
                    ring_curled = right_lms[RING_FINGER][2] > right_lms[RING_POINT][2]
                    pinky_curled = right_lms[PINKY_FINGER][2] > right_lms[PINKY_POINT][2]

                    camera_look_gesture = index_up and middle_curled and ring_curled and pinky_curled

                    if camera_look_gesture:
                        gesture_hold_frames += 1

                        if gesture_hold_frames >= STABLE_FRAMES:
                            camera_active = True
                            index_x, index_y = right_lms[INDEX_FINGER][1], right_lms[INDEX_FINGER][2]
                            smoothed_pos = smooth_position(position_history, np, (index_x, index_y))

                            if prev_index_point is not None:
                                dx, dy = calculate_velocity(smoothed_pos, prev_index_point)
                                direction = get_direction_from_velocity(np, DEAD_ZONE, dx, dy)
                                if direction:
                                    detected_action = f"CAMERA LOOK {direction}"

                            prev_index_point = smoothed_pos
                    else:
                        camera_active = False
                        prev_index_point = None
                        gesture_hold_frames = 0
                        position_history.clear()

            elif CURRENT_MODE == CANVAS_MODE:
                print("CANVAS")

    # Send commands with throttling
    if detected_action:
        current_active_action = detected_action
        send_command_throttled(current_active_action)
        cv2.putText(img, current_active_action, (50, 50),
                    cv2.FONT_HERSHEY_SIMPLEX, 1, (0, 255, 0), 2)
    else:
        if current_active_action is not None:
            send_command_throttled("NULL")
            current_active_action = None

    # Display FPS
    fps = 1.0 / (time.time() - prev_time)
    prev_time = time.time()
    cv2.putText(img, f"FPS: {int(fps)}", (50, 100),
                cv2.FONT_HERSHEY_SIMPLEX, 0.7, (0, 255, 0), 2)

    cv2.imshow("Image", img)

    # Frame rate control
    elapsed = time.time() - current_time
    if elapsed < FRAME_TIME:
        time.sleep(FRAME_TIME - elapsed)

    if cv2.waitKey(1) & 0xFF == ord('q'):
        break

# Cleanup
send_command_throttled("NULL")
cap.release()
cv2.destroyAllWindows()
