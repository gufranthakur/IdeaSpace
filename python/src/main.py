import cv2
import time
import hand_tracking_module as htm
import hand_utils as utils
import server as server
from collections import deque
import numpy as np

from gesture_config import *
from maths import *
from swipe_utils import *
from remove_gesture import RemoveGesture

CURRENT_MODE = VIEW_MODE

# Initialize remove gesture detector
remove_gesture = RemoveGesture()

# Runtime variables
position_history = deque(maxlen=SMOOTHING_WINDOW)
last_command_time = 0
last_sent_command = None
snap_ready = False
snap_sent = False

cap = cv2.VideoCapture(0)
cap.set(cv2.CAP_PROP_BUFFERSIZE, 1)
cap.set(cv2.CAP_PROP_FPS, TARGET_FPS)
detector = htm.handDetector(maxHands=1)

# State tracking
zoom_in_frames = 0
zoom_out_frames = 0
current_active_action = None
camera_active = False
prev_index_point = None
gesture_hold_frames = 0

# Resolution-independent thresholds (as ratios of frame width)
ZOOM_OUT_MIN = 0.0
ZOOM_OUT_MAX = 0.07  # ~40 pixels on 640 width
ZOOM_IN_MIN_1 = 0.08  # ~50 pixels
ZOOM_IN_MAX_1 = 0.23  # ~150 pixels
ZOOM_IN_MIN_2 = 0.14  # ~90 pixels
ZOOM_IN_MAX_2 = 0.19  # ~120 pixels


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
    lms = detector.find_position(img, draw=False)

    detected_action = None
    handedness = detector.get_handedness(0)

    # Show detected hand
    if handedness:
        cv2.putText(img, f"Hand: {handedness}", (10, 30),
                    cv2.FONT_HERSHEY_SIMPLEX, 0.7, (0, 255, 0), 2)

    # LEFT HAND gestures - Swipe and Remove
    if handedness == "Left" and len(lms) != 0:
        # Check remove gesture first
        remove_action = remove_gesture.detect(lms, img)
        if remove_action:
            detected_action = remove_action
        else:
            # Check swipe if remove not detected
            swipe_action = detect_swipe_gesture(lms, img)
            if swipe_action:
                detected_action = swipe_action

    # Right hand gestures for other controls
    if handedness == "Right" and len(lms) != 0:

        if CURRENT_MODE == VIEW_MODE:
            # Zoom gestures - now using normalized distances (0.0-1.0)
            zoom_out_action = utils.distance_between_points(THUMB, INDEX_FINGER, RED, img, lms)
            zoom_in_action = utils.distance_between_points(THUMB, MIDDLE_FINGER, BLUE, img, lms)

            # Gesture detection - Only index finger up, rest closed
            index_up = lms[INDEX_FINGER][2] < lms[INDEX_POINT][2]
            middle_curled = lms[MIDDLE_FINGER][2] > lms[MIDDLE_POINT][2]
            ring_curled = lms[RING_FINGER][2] > lms[RING_POINT][2]
            pinky_curled = lms[PINKY_FINGER][2] > lms[PINKY_POINT][2]

            # Camera look gesture: only index up, all others closed
            camera_look_gesture = index_up and middle_curled and ring_curled and pinky_curled

            if camera_look_gesture:
                gesture_hold_frames += 1

                if gesture_hold_frames >= STABLE_FRAMES:
                    camera_active = True
                    index_x, index_y = lms[INDEX_FINGER][1], lms[INDEX_FINGER][2]

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

            # Zoom detection using ratio-based thresholds
            if not camera_active:
                if utils.action_between_threshold(zoom_out_action, ZOOM_OUT_MIN, ZOOM_OUT_MAX) and \
                        utils.action_between_threshold(zoom_in_action, ZOOM_IN_MIN_1, ZOOM_IN_MAX_1):
                    zoom_out_frames += 1
                    if zoom_out_frames >= STABLE_FRAMES:
                        detected_action = "ZOOMED OUT"
                else:
                    zoom_out_frames = 0

                if utils.action_between_threshold(zoom_in_action, ZOOM_IN_MIN_2, ZOOM_IN_MAX_2):
                    zoom_in_frames += 1
                    if zoom_in_frames >= STABLE_FRAMES:
                        detected_action = "ZOOMED IN"
                else:
                    zoom_in_frames = 0

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
