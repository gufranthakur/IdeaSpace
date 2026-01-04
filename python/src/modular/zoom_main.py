import cv2
import time
import math
import sys
import threading

from server import Server
from hand_tracking_module import handDetector
from gesture_config import *

# Parse debug flag
DEBUG = "--debug" in sys.argv

# TUNABLE PARAMETERS
ZOOM_OUT_THRESHOLD = 0.15  # Close together = zoom out (normalized distance)
ZOOM_IN_THRESHOLD = 0.35   # Far apart = zoom in (normalized distance)
STABLE_FRAMES = 5          # Frames to hold before activating
THREAD_UPDATE_INTERVAL = 0.016  # ~60 FPS

PORT = 65004
server = Server(PORT)

detector = handDetector(maxHands=2, detectionConfidence=0.7, trackConfidence=0.7)

last_command_time = 0
last_sent_command = None

# Threading variables
left_index = None
right_index = None
img_width = 640
lock = threading.Lock()
running = True
zoom_result = None
stable_count = 0

cap = cv2.VideoCapture(0)
cap.set(cv2.CAP_PROP_BUFFERSIZE, 1)
cap.set(cv2.CAP_PROP_FPS, 30)
cap.set(cv2.CAP_PROP_FRAME_WIDTH, 640)
cap.set(cv2.CAP_PROP_FRAME_HEIGHT, 480)


def send_command_throttled(command):
    global last_command_time, last_sent_command
    current_time = time.time()

    if command != last_sent_command or (current_time - last_command_time) >= 0.1:
        server.send_command(command)
        last_command_time = current_time
        last_sent_command = command


def zoom_detection_thread():
    global zoom_result, stable_count

    while running:
        with lock:
            l_idx = left_index
            r_idx = right_index
            w = img_width

        if l_idx and r_idx:
            distance = math.sqrt(
                (r_idx[0] - l_idx[0])**2 +
                (r_idx[1] - l_idx[1])**2
            ) / w

            stable_count += 1

            if stable_count >= STABLE_FRAMES:
                if distance < ZOOM_OUT_THRESHOLD:
                    with lock:
                        zoom_result = "ZOOM OUT"
                elif distance > ZOOM_IN_THRESHOLD:
                    with lock:
                        zoom_result = "ZOOM IN"
                else:
                    with lock:
                        zoom_result = None
        else:
            stable_count = 0
            with lock:
                zoom_result = None

        time.sleep(THREAD_UPDATE_INTERVAL)


# Start zoom detection thread
zoom_thread = threading.Thread(target=zoom_detection_thread, daemon=True)
zoom_thread.start()

prev_time = time.time()

try:
    while True:
        success, img = cap.read()
        if not success:
            continue

        img = cv2.flip(img, 1)
        h, w = img.shape[:2]

        with lock:
            img_width = w

        if DEBUG:
            img = detector.find_hands(img, False)
        else:
            detector.find_hands(img, False)

        left_idx_pos = None
        right_idx_pos = None

        if detector.results and detector.results.multi_hand_landmarks:
            for hand_idx in range(len(detector.results.multi_hand_landmarks)):
                handedness = detector.get_handedness(hand_idx)
                lms = detector.find_position(img, hand_idx, draw=False)

                if len(lms) >= 21:
                    index_tip = lms[8]

                    if handedness == "Left":
                        left_idx_pos = (index_tip[1], index_tip[2])
                        if DEBUG:
                            cv2.circle(img, left_idx_pos, 10, (255, 0, 0), -1)
                    elif handedness == "Right":
                        right_idx_pos = (index_tip[1], index_tip[2])
                        if DEBUG:
                            cv2.circle(img, right_idx_pos, 10, (0, 255, 0), -1)

        # Update thread-safe variables
        with lock:
            left_index = left_idx_pos
            right_index = right_idx_pos
            detected_action = zoom_result

        # Send command
        if detected_action:
            send_command_throttled(detected_action)
            if DEBUG:
                cv2.putText(img, detected_action, (50, 50),
                           cv2.FONT_HERSHEY_SIMPLEX, 1, (0, 255, 0), 2)
        else:
            if last_sent_command != "NULL":
                send_command_throttled("NULL")

        # Debug info
        if DEBUG:
            if left_idx_pos and right_idx_pos:
                cv2.line(img, left_idx_pos, right_idx_pos, (0, 255, 255), 2)
                dist = math.sqrt(
                    (right_idx_pos[0] - left_idx_pos[0])**2 +
                    (right_idx_pos[1] - left_idx_pos[1])**2
                ) / w
                cv2.putText(img, f"Dist: {dist:.3f}", (10, 80),
                           cv2.FONT_HERSHEY_SIMPLEX, 0.5, (255, 255, 255), 1)

            cv2.putText(img, f"L:{'Y' if left_idx_pos else 'N'} R:{'Y' if right_idx_pos else 'N'}",
                       (10, 100), cv2.FONT_HERSHEY_SIMPLEX, 0.5, (200, 200, 200), 1)

            fps = 1.0 / (time.time() - prev_time)
            prev_time = time.time()
            cv2.putText(img, f"FPS: {int(fps)}", (w - 100, 30),
                       cv2.FONT_HERSHEY_SIMPLEX, 0.6, (0, 255, 0), 2)

            cv2.namedWindow("Zoom_65004", cv2.WINDOW_AUTOSIZE)
            cv2.imshow("Zoom_65004", img)

        if DEBUG and cv2.waitKey(1) & 0xFF == ord('q'):
            break
        elif not DEBUG:
            time.sleep(0.01)

finally:
    running = False
    zoom_thread.join(timeout=1.0)
    send_command_throttled("NULL")
    server.close()
    cap.release()
    if DEBUG:
        cv2.destroyAllWindows()
