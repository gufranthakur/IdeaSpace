import cv2
import time
import sys

from server import Server
from hand_tracking_module import handDetector
from gesture_config import *
from drag_gesture import DragGesture
from flick_gesture import FlickGesture
from swipe_utils import detect_swipe_gesture

# Parse debug flag
DEBUG = "--debug" in sys.argv

# Ports for left and right hand servers
RIGHT_PORT = 65002

# Initialize servers
left_server = Server(RIGHT_PORT)
right_server = Server(RIGHT_PORT)

# Detector for both hands
detector = handDetector(maxHands=2, detectionConfidence=0.5, trackConfidence=0.5)

# Gesture detectors
drag_gesture = DragGesture()
left_flick_gesture = FlickGesture(return_command="REMOVE")
right_flick_gesture = FlickGesture(return_command="ANIMATE")

# Command throttling
left_last_command_time = 0
left_last_sent_command = None
right_last_command_time = 0
right_last_sent_command = None

# Camera settings
cap = cv2.VideoCapture(0)
cap.set(cv2.CAP_PROP_BUFFERSIZE, 1)
cap.set(cv2.CAP_PROP_FPS, TARGET_FPS)
cap.set(cv2.CAP_PROP_FRAME_WIDTH, 640)
cap.set(cv2.CAP_PROP_FRAME_HEIGHT, 480)
cap.set(cv2.CAP_PROP_AUTOFOCUS, 0)

PROCESS_WIDTH = 640
PROCESS_HEIGHT = 480


def send_command_throttled(server, command, last_time, last_command):
    """Send command with throttling to prevent spam"""
    current_time = time.time()

    if command != last_command or (current_time - last_time) >= COMMAND_SEND_INTERVAL:
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

        # Process at lower resolution for performance
        img_small = cv2.resize(img, (PROCESS_WIDTH, PROCESS_HEIGHT))
        if DEBUG:
            img_small = detector.find_hands(img_small, False)
        else:
            detector.find_hands(img_small, False)

        scale_x = w_full / PROCESS_WIDTH
        scale_y = h_full / PROCESS_HEIGHT

        # Separate landmarks for left and right hands
        left_lms = []
        right_lms = []
        left_action = None
        right_action = None

        # Process all detected hands
        if detector.results and detector.results.multi_hand_landmarks:
            for hand_idx in range(len(detector.results.multi_hand_landmarks)):
                handedness = detector.get_handedness(hand_idx)
                lms_small = detector.find_position(img_small, hand_idx, draw=False)
                lms_scaled = [[l[0], int(l[1] * scale_x), int(l[2] * scale_y)] for l in lms_small]

                if handedness == "Left":
                    left_lms = lms_scaled
                    if DEBUG:
                        for lm in left_lms:
                            cv2.circle(img, (lm[1], lm[2]), 5, (255, 0, 255), -1)

                elif handedness == "Right":
                    right_lms = lms_scaled
                    if DEBUG:
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

            if not right_action and not right_flick_gesture.is_active():
                drag_action = drag_gesture.detect(right_lms, img)
                if drag_action:
                    right_action = drag_action

        # Send LEFT hand commands
        if left_action:
            left_last_command_time, left_last_sent_command, _ = send_command_throttled(
                left_server, left_action, left_last_command_time, left_last_sent_command
            )
            if DEBUG:
                cv2.putText(img, f"LEFT: {left_action}", (10, 60),
                           cv2.FONT_HERSHEY_SIMPLEX, 0.7, (255, 0, 255), 2)
        else:
            if left_last_sent_command is not None and left_last_sent_command != "NULL":
                left_last_command_time, left_last_sent_command, _ = send_command_throttled(
                    left_server, "NULL", left_last_command_time, left_last_sent_command
                )

        # Send RIGHT hand commands
        if right_action:
            right_last_command_time, right_last_sent_command, _ = send_command_throttled(
                right_server, right_action, right_last_command_time, right_last_sent_command
            )
            if DEBUG:
                cv2.putText(img, f"RIGHT: {right_action}", (10, 90),
                           cv2.FONT_HERSHEY_SIMPLEX, 0.7, (0, 255, 255), 2)
        else:
            if right_last_sent_command is not None and right_last_sent_command != "NULL":
                right_last_command_time, right_last_sent_command, _ = send_command_throttled(
                    right_server, "NULL", right_last_command_time, right_last_sent_command
                )

        # Debug display
        if DEBUG:
            cv2.putText(img, "CORE GESTURES", (10, 30),
                       cv2.FONT_HERSHEY_SIMPLEX, 0.7, (255, 255, 0), 2)

            fps = 1.0 / (time.time() - prev_time)
            prev_time = time.time()
            cv2.putText(img, f"FPS: {int(fps)}", (img.shape[1] - 100, 30),
                       cv2.FONT_HERSHEY_SIMPLEX, 0.6, (0, 255, 0), 2)

            cv2.namedWindow("CoreGestures", cv2.WINDOW_AUTOSIZE)
            cv2.imshow("CoreGestures", img)

        # Frame timing
        elapsed = time.time() - current_time
        if elapsed < FRAME_TIME:
            time.sleep(FRAME_TIME - elapsed)

        if DEBUG and cv2.waitKey(1) & 0xFF == ord('q'):
            break
        elif not DEBUG:
            time.sleep(0.01)

finally:
    send_command_throttled(left_server, "NULL", 0, None)
    send_command_throttled(right_server, "NULL", 0, None)
    left_server.close()
    right_server.close()
    cap.release()
    if DEBUG:
        cv2.destroyAllWindows()
