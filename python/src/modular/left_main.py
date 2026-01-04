import cv2
import time
import sys

from server import Server
from hand_tracking_module import handDetector
from gesture_config import *
from flick_gesture import FlickGesture
from swipe_utils import detect_swipe_gesture

# Parse debug flag
DEBUG = "--debug" in sys.argv

PORT = 65003
server = Server(PORT)

detector = handDetector(maxHands=1, detectionConfidence=0.5, trackConfidence=0.5)
flick_gesture = FlickGesture(return_command="REMOVE")

last_command_time = 0
last_sent_command = None

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

        left_lms = []
        detected_action = None

        if detector.results and detector.results.multi_hand_landmarks:
            for hand_idx in range(len(detector.results.multi_hand_landmarks)):
                handedness = detector.get_handedness(hand_idx)

                if handedness == "Left":
                    lms_small = detector.find_position(img_small, hand_idx, draw=False)
                    left_lms = [[l[0], int(l[1] * scale_x), int(l[2] * scale_y)] for l in lms_small]

                    if DEBUG:
                        for lm in left_lms:
                            cv2.circle(img, (lm[1], lm[2]), 5, (255, 0, 255), -1)

        if len(left_lms) > 0:
            flick_action = flick_gesture.detect(left_lms, img)
            if flick_action:
                detected_action = flick_action

            if not detected_action and not flick_gesture.is_active():
                swipe_action = detect_swipe_gesture(left_lms, img)
                if swipe_action:
                    detected_action = swipe_action

        if detected_action:
            send_command_throttled(detected_action)
            if DEBUG:
                cv2.putText(img, detected_action, (50, 60),
                            cv2.FONT_HERSHEY_SIMPLEX, 0.7, (0, 255, 0), 2)
        else:
            if last_sent_command is not None and last_sent_command != "NULL":
                send_command_throttled("NULL")

        if DEBUG:
            cv2.putText(img, "LEFT GESTURES", (10, 30),
                        cv2.FONT_HERSHEY_SIMPLEX, 0.7, (255, 255, 0), 2)

            fps = 1.0 / (time.time() - prev_time)
            prev_time = time.time()
            cv2.putText(img, f"FPS: {int(fps)}", (img.shape[1] - 100, 30),
                        cv2.FONT_HERSHEY_SIMPLEX, 0.6, (0, 255, 0), 2)

            cv2.namedWindow("Left_65003", cv2.WINDOW_AUTOSIZE)
            cv2.imshow("Left_65003", img)

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
