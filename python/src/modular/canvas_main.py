import cv2
import time
import sys

from server import Server
from hand_tracking_module import handDetector
from gesture_config import *
from canvas_drawing import CanvasDrawing
from canvas_gestures import CanvasGestureDetector
from flick_gesture import FlickGesture

# Server connection
PORT = 65005
server = Server(PORT)

# Detector - right hand only
detector = handDetector(maxHands=1, detectionConfidence=0.4, trackConfidence=0.4)

# Gesture detectors
canvas_gesture_detector = CanvasGestureDetector()
flick_gesture = FlickGesture(return_command="SPLIT")  # For clearing canvas

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
        canvas = CanvasDrawing(img.shape, server)

    scale_x = w_full / PROCESS_WIDTH
    scale_y = h_full / PROCESS_HEIGHT

    right_lms = []
    detected_action = None
    pointer_pos = None

    if detector.results and detector.results.multi_hand_landmarks:
        for hand_idx in range(len(detector.results.multi_hand_landmarks)):
            handedness = detector.get_handedness(hand_idx)

            if handedness == "Right":
                lms_small = detector.find_position(img_small, hand_idx, draw=False)
                right_lms = [[l[0], int(l[1] * scale_x), int(l[2] * scale_y)] for l in lms_small]

                # Draw landmarks
                for lm in right_lms:
                    cv2.circle(img, (lm[1], lm[2]), 5, (255, 0, 255), -1)

    # Handle UI grace period
    if canvas.ui_just_closed:
        canvas.ui_closed_frames += 1
        if canvas.ui_closed_frames >= canvas.UI_GRACE_FRAMES:
            canvas.ui_just_closed = False

    ui_is_open = canvas.unified_ui.is_active

    if len(right_lms) > 0:
        # Check flick gesture first - closes UI or clears canvas
        flick_action = flick_gesture.detect(right_lms, img)
        if flick_action == "SPLIT":
            if ui_is_open:
                canvas.close_unified_ui()
                detected_action = "CLOSED UI"
            else:
                canvas.clear_canvas()
                detected_action = "CLEAR"

        elif not flick_gesture.is_active():
            # Canvas gestures
            gesture_result = canvas_gesture_detector.detect_canvas_gestures(right_lms, img)

            if gesture_result:
                gesture_type, x, y = gesture_result

                cv2.putText(img, f"Gesture: {gesture_type}", (10, 90),
                            cv2.FONT_HERSHEY_SIMPLEX, 0.5, (255, 0, 255), 2)

                if gesture_type == "OPEN_UI":
                    if not ui_is_open:
                        canvas.open_unified_ui()
                        detected_action = "UI OPENED"

                elif gesture_type == "DRAW":
                    if ui_is_open:
                        pointer_pos = (x, y)
                        canvas.handle_ui_point(x, y)
                        detected_action = "SELECTING"
                    elif not canvas.ui_just_closed:
                        canvas.set_erase_mode(False)
                        x_clamped = max(0, min(x, w_full - 1))
                        y_clamped = max(0, min(y, h_full - 1))
                        canvas.draw_point(x_clamped, y_clamped)
                        detected_action = "DRAW"

                elif gesture_type == "ERASE":
                    if ui_is_open:
                        pointer_pos = (x, y)
                        canvas.handle_ui_point(x, y)
                        detected_action = "SELECTING"
                    elif not canvas.ui_just_closed:
                        canvas.set_erase_mode(True)
                        x_clamped = max(0, min(x, w_full - 1))
                        y_clamped = max(0, min(y, h_full - 1))
                        canvas.draw_point(x_clamped, y_clamped)
                        detected_action = "ERASE"

                elif gesture_type == "THUMBS_UP_RELEASED":
                    pass
            else:
                canvas.reset_drawing()
    else:
        canvas.reset_drawing()

    # Overlay canvas and UI
    img = canvas.get_overlay(img)
    img = canvas.draw_ui(img, pointer_pos)

    # Display
    cv2.putText(img, "CANVAS", (10, 30),
                cv2.FONT_HERSHEY_SIMPLEX, 0.7, (255, 255, 0), 2)

    if detected_action:
        cv2.putText(img, detected_action, (50, 60),
                    cv2.FONT_HERSHEY_SIMPLEX, 0.7, (0, 255, 255), 2)

    fps = 1.0 / (time.time() - prev_time)
    prev_time = time.time()
    cv2.putText(img, f"FPS: {int(fps)}", (img.shape[1] - 100, 30),
                cv2.FONT_HERSHEY_SIMPLEX, 0.6, (0, 255, 0), 2)

    cv2.imshow("Canvas", img)

    elapsed = time.time() - current_time
    if elapsed < FRAME_TIME:
        time.sleep(FRAME_TIME - elapsed)

    if cv2.waitKey(1) & 0xFF == ord('q'):
        break

server.send_command("CANVAS END")
server.close()
cap.release()
cv2.destroyAllWindows()
