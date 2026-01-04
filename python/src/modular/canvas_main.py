import cv2
import time
import sys
import argparse

from server import Server
from hand_tracking_module import handDetector
from gesture_config import *
from canvas_drawing import CanvasDrawing
from canvas_gestures import CanvasGestureDetector
from flick_gesture import FlickGesture

# Parse arguments
parser = argparse.ArgumentParser(description='Canvas Drawing App')
parser.add_argument('--debug', action='store_true', help='Enable debug mode (camera overlay, landmarks, FPS)')
args = parser.parse_args()

DEBUG_MODE = args.debug

# Server connection
PORT = 65005
server = Server(PORT)

# Detector - right hand only
detector = handDetector(maxHands=1, detectionConfidence=0.4, trackConfidence=0.4)

# Gesture detectors
canvas_gesture_detector = CanvasGestureDetector()
flick_gesture = FlickGesture(return_command="SPLIT")

# Camera settings
cap = cv2.VideoCapture(0)
cap.set(cv2.CAP_PROP_BUFFERSIZE, 1)
cap.set(cv2.CAP_PROP_FPS, TARGET_FPS)
cap.set(cv2.CAP_PROP_FRAME_WIDTH, 640)
cap.set(cv2.CAP_PROP_FRAME_HEIGHT, 480)
cap.set(cv2.CAP_PROP_AUTOFOCUS, 0)

PROCESS_WIDTH = 640
PROCESS_HEIGHT = 480

canvas = None
prev_time = time.time()

while True:
    current_time = time.time()

    success, img = cap.read()
    if not success:
        continue

    img = cv2.flip(img, 1)
    h_full, w_full = img.shape[:2]

    img_small = cv2.resize(img, (PROCESS_WIDTH, PROCESS_HEIGHT))
    img_small = detector.find_hands(img_small, False)

    if canvas is None:
        canvas = CanvasDrawing(img.shape, server, debug_mode=DEBUG_MODE)

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

                # Draw landmarks only in debug mode
                if DEBUG_MODE:
                    for lm in right_lms:
                        cv2.circle(img, (lm[1], lm[2]), 5, (255, 0, 255), -1)

    if len(right_lms) > 0:
        # Check flick gesture first - clears canvas
        flick_action = flick_gesture.detect(right_lms, img)
        if flick_action == "SPLIT":
            canvas.clear_canvas()
            detected_action = "CLEAR"

        elif not flick_gesture.is_active():
            # Canvas gestures (draw/erase)
            gesture_result = canvas_gesture_detector.detect_canvas_gestures(right_lms, img)

            if gesture_result:
                gesture_type, x, y = gesture_result

                if gesture_type == "DRAW":
                    canvas.set_erase_mode(False)
                    x_clamped = max(0, min(x, w_full - 1))
                    y_clamped = max(0, min(y, h_full - 1))
                    canvas.draw_point(x_clamped, y_clamped)
                    detected_action = "DRAW"

                elif gesture_type == "ERASE":
                    canvas.set_erase_mode(True)
                    x_clamped = max(0, min(x, w_full - 1))
                    y_clamped = max(0, min(y, h_full - 1))
                    canvas.draw_point(x_clamped, y_clamped)
                    detected_action = "ERASE"
            else:
                canvas.reset_drawing()
    else:
        canvas.reset_drawing()

    # Only show window in debug mode
    if DEBUG_MODE:
        output = canvas.get_overlay(img)
        output = canvas.draw_debug_ui(output)

        cv2.putText(output, "CANVAS [DEBUG]", (10, 30),
                    cv2.FONT_HERSHEY_SIMPLEX, 0.7, (255, 255, 0), 2)

        if detected_action:
            cv2.putText(output, detected_action, (50, 90),
                        cv2.FONT_HERSHEY_SIMPLEX, 0.7, (0, 255, 255), 2)

        fps = 1.0 / (time.time() - prev_time)
        cv2.putText(output, f"FPS: {int(fps)}", (output.shape[1] - 100, 30),
                    cv2.FONT_HERSHEY_SIMPLEX, 0.6, (0, 255, 0), 2)

        cv2.imshow("Canvas", output)

    prev_time = time.time()

    elapsed = time.time() - current_time
    if elapsed < FRAME_TIME:
        time.sleep(FRAME_TIME - elapsed)

    if cv2.waitKey(1) & 0xFF == ord('q'):
        break

server.send_command("CANVAS END")
server.close()
cap.release()
cv2.destroyAllWindows()
