import cv2
import time
import sys

from server import Server
from hand_tracking_module import handDetector
from gesture_config import *
from zoom_gesture import ZoomGesture

# Server connection
PORT = 65004
server = Server(PORT)

# Detector - TWO hands for expand gesture
detector = handDetector(maxHands=2, detectionConfidence=0.5, trackConfidence=0.5)

# Gesture detector
zoom_gesture = ZoomGesture()

# Start expand detection thread
zoom_gesture.start_expand_thread()

# State
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
        img_small = detector.find_hands(img_small, False)

        scale_x = w_full / PROCESS_WIDTH
        scale_y = h_full / PROCESS_HEIGHT

        left_lms = []
        right_lms = []
        detected_action = None

        if detector.results and detector.results.multi_hand_landmarks:
            for hand_idx in range(len(detector.results.multi_hand_landmarks)):
                handedness = detector.get_handedness(hand_idx)
                lms_small = detector.find_position(img_small, hand_idx, draw=False)
                lms = [[l[0], int(l[1] * scale_x), int(l[2] * scale_y)] for l in lms_small]

                # Draw landmarks
                for lm in lms:
                    cv2.circle(img, (lm[1], lm[2]), 5, (255, 0, 255), -1)

                if handedness == "Left":
                    left_lms = lms
                elif handedness == "Right":
                    right_lms = lms

        # Update hands for expand thread
        zoom_gesture.update_hands(left_lms, right_lms, img.shape)

        # Priority 1: Check two-hand expand gesture (from background thread)
        expand_action = zoom_gesture.get_expand_result()
        if expand_action:
            detected_action = expand_action

        # Priority 2: Single-hand pinch zoom (only if no expand detected)
        if not detected_action and len(right_lms) > 0:
            zoom_action = zoom_gesture.detect_pinch_zoom(right_lms, img)
            if zoom_action:
                detected_action = zoom_action

        # Send command
        if detected_action:
            send_command_throttled(detected_action)
            cv2.putText(img, detected_action, (50, 60),
                        cv2.FONT_HERSHEY_SIMPLEX, 0.7, (0, 255, 0), 2)
        else:
            if last_sent_command is not None and last_sent_command != "NULL":
                send_command_throttled("NULL")

        # Display status
        cv2.putText(img, "ZOOM", (10, 30),
                    cv2.FONT_HERSHEY_SIMPLEX, 0.7, (255, 255, 0), 2)
        
        # Show hand detection status
        hand_status = f"L:{'Y' if left_lms else 'N'} R:{'Y' if right_lms else 'N'}"
        cv2.putText(img, hand_status, (10, 60),
                    cv2.FONT_HERSHEY_SIMPLEX, 0.5, (200, 200, 200), 1)
        
        # Show expand tracking status
        if zoom_gesture.is_expand_active():
            cv2.putText(img, "EXPAND TRACKING", (10, 80),
                        cv2.FONT_HERSHEY_SIMPLEX, 0.5, (0, 255, 255), 1)
        
        # Debug: Show current palm distance if both hands visible
        if left_lms and right_lms:
            left_palm_x = left_lms[MIDDLE_POINT][1]
            right_palm_x = right_lms[MIDDLE_POINT][1]
            left_palm_y = left_lms[MIDDLE_POINT][2]
            right_palm_y = right_lms[MIDDLE_POINT][2]
            import math
            dist = math.sqrt((right_palm_x - left_palm_x)**2 + (right_palm_y - left_palm_y)**2) / w_full
            cv2.putText(img, f"Dist: {dist:.2f}", (10, 100),
                        cv2.FONT_HERSHEY_SIMPLEX, 0.5, (255, 255, 0), 1)

        fps = 1.0 / (time.time() - prev_time)
        prev_time = time.time()
        cv2.putText(img, f"FPS: {int(fps)}", (img.shape[1] - 100, 30),
                    cv2.FONT_HERSHEY_SIMPLEX, 0.6, (0, 255, 0), 2)

        cv2.imshow("Zoom", img)

        elapsed = time.time() - current_time
        if elapsed < FRAME_TIME:
            time.sleep(FRAME_TIME - elapsed)

        if cv2.waitKey(1) & 0xFF == ord('q'):
            break

finally:
    # Cleanup
    zoom_gesture.stop_expand_thread()
    send_command_throttled("NULL")
    server.close()
    cap.release()
    cv2.destroyAllWindows()