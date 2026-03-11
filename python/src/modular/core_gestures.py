from hand_tracking_module import handDetector
import cv2
import socket
import time
import sys
import math
import argparse

from server import Server
from gesture_config import *
from flick_gesture import FlickGesture
from drag_gesture import DragGesture
from swipe_utils import detect_swipe_gesture

# ── Args ──────────────────────────────────────────────────────────────────────
parser = argparse.ArgumentParser(description='Unified Gesture + Simulation System')
parser.add_argument('--debug', action='store_true', help='Enable debug mode (camera view, landmarks, FPS)')
args = parser.parse_args()

DEBUG_MODE = args.debug

# ── Gesture Server (TCP) ──────────────────────────────────────────────────────
PORT = 64000
server = Server(PORT)

# ── Simulation UDP Socket ─────────────────────────────────────────────────────
sim_sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
SIM_RIGHT_PORT = ("127.0.0.1", 65000)
SIM_LEFT_PORT  = ("127.0.0.1", 65005)

# ── Detector ──────────────────────────────────────────────────────────────────
detector = handDetector(maxHands=2, detectionConfidence=0.8, trackConfidence=0.8)

# ── Gesture State ─────────────────────────────────────────────────────────────
left_flick_gesture  = FlickGesture(return_command="REMOVE")
right_flick_gesture = FlickGesture(return_command="ANIMATE")
right_drag_gesture  = DragGesture()

core_left_last_command_time  = 0
core_left_last_sent_command  = None
core_right_last_command_time = 0
core_right_last_sent_command = None
zoom_last_command_time       = 0
zoom_last_sent_command       = None

# Right hand: TOP_ROTATE, BOTTOM_ROTATE, LEFT_ROTATE
top_rotate_was_active    = {"Right": False}
bottom_rotate_was_active = {"Right": False}
left_rotate_was_active   = {"Right": False}

# Left hand: RIGHT_ROTATE
right_rotate_was_active  = {"Left": False}

ROTATE_HOLD_FRAMES  = 6
rotate_hold_counter = {"Left": 0, "Right": 0}
rotate_hold_type    = {"Left": None, "Right": None}

# ── Camera ────────────────────────────────────────────────────────────────────
cap = cv2.VideoCapture(0)
cap.set(cv2.CAP_PROP_BUFFERSIZE, 1)
cap.set(cv2.CAP_PROP_FPS, TARGET_FPS)
cap.set(cv2.CAP_PROP_FRAME_WIDTH, 640)
cap.set(cv2.CAP_PROP_FRAME_HEIGHT, 480)
cap.set(cv2.CAP_PROP_AUTOFOCUS, 0)

PROCESS_WIDTH  = 640
PROCESS_HEIGHT = 480

# ── Helpers ───────────────────────────────────────────────────────────────────
def send_command_throttled(command, last_time, last_command, interval=COMMAND_SEND_INTERVAL):
    current_time = time.time()
    if command != last_command or (current_time - last_time) >= interval:
        server.send_command(command)
        return current_time, command, True
    return last_time, last_command, False


# ── Rotate gesture detectors ──────────────────────────────────────────────────
def is_top_rotate_gesture(lms):
    index_up   = lms[8][2]  < lms[6][2]
    middle_up  = lms[12][2] < lms[10][2]
    ring_down  = lms[16][2] > lms[14][2]
    pinky_down = lms[20][2] > lms[18][2]
    thumb_down = abs(lms[4][1] - lms[2][1]) < 40
    return index_up and middle_up and ring_down and pinky_down and thumb_down


def is_bottom_rotate_gesture(lms):
    index_down   = lms[8][2]  > lms[6][2] + 20
    middle_down  = lms[12][2] > lms[10][2] + 20
    thumb_out    = abs(lms[4][1] - lms[2][1]) > 35
    pinky_curled = lms[20][2] < lms[18][2]
    return index_down and middle_down and thumb_out and pinky_curled


def is_left_rotate_gesture(lms):
    index_left   = lms[8][1] < lms[6][1] - 20
    middle_left  = lms[12][1] < lms[10][1] - 20
    pinky_curled = lms[20][1] > lms[18][1]
    return index_left and middle_left and pinky_curled


def is_right_rotate_gesture(lms):
    index_right  = lms[8][1] > lms[6][1] + 20
    middle_right = lms[12][1] > lms[10][1] + 20
    pinky_curled = lms[20][1] < lms[18][1]
    return index_right and middle_right and pinky_curled


def detect_right_hand_rotate(lms):
    """Right hand: TOP_ROTATE, BOTTOM_ROTATE, LEFT_ROTATE."""
    if is_top_rotate_gesture(lms):    return "TOP_ROTATE"
    if is_bottom_rotate_gesture(lms): return "BOTTOM_ROTATE"
    if is_left_rotate_gesture(lms):   return "LEFT_ROTATE"
    return None


def detect_left_hand_rotate(lms):
    """Left hand: RIGHT_ROTATE."""
    if is_right_rotate_gesture(lms): return "RIGHT_ROTATE"
    return None


# ── Main Loop ─────────────────────────────────────────────────────────────────
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

        left_lms  = []
        right_lms = []
        left_action  = None
        right_action = None
        zoom_action  = None

        if detector.results and detector.results.multi_hand_landmarks:
            for hand_idx in range(len(detector.results.multi_hand_landmarks)):
                handedness = detector.get_handedness(hand_idx)
                lms_small  = detector.find_position(img_small, hand_idx, draw=False)
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

                # ── Send simulation landmark data via UDP ─────────────────────
                hand_landmarks_raw = detector.results.multi_hand_landmarks[hand_idx]
                sim_data = []
                for landmark in hand_landmarks_raw.landmark:
                    sim_data.extend([landmark.x * w_full, landmark.y * h_full, landmark.z * 200])

                if handedness == "Right":
                    sim_sock.sendto(str.encode(str(sim_data)), SIM_RIGHT_PORT)
                elif handedness == "Left":
                    sim_sock.sendto(str.encode(str(sim_data)), SIM_LEFT_PORT)

        # ── LEFT hand gestures (RIGHT_ROTATE + flick + swipe) ─────────────────
        if len(left_lms) > 0:
            rotate_cmd = detect_left_hand_rotate(left_lms)
            if rotate_cmd == "RIGHT_ROTATE":
                if rotate_hold_type["Left"] == rotate_cmd:
                    rotate_hold_counter["Left"] += 1
                else:
                    rotate_hold_type["Left"]    = rotate_cmd
                    rotate_hold_counter["Left"] = 1

                if rotate_hold_counter["Left"] >= ROTATE_HOLD_FRAMES:
                    if not right_rotate_was_active["Left"]:
                        left_action = rotate_cmd
                        right_rotate_was_active["Left"] = True
            else:
                right_rotate_was_active["Left"] = False
                rotate_hold_counter["Left"] = 0
                rotate_hold_type["Left"]    = None
                flick_action = left_flick_gesture.detect(left_lms, img)
                if flick_action:
                    left_action = flick_action
                if not left_action and not left_flick_gesture.is_active():
                    swipe_action = detect_swipe_gesture(left_lms, img)
                    if swipe_action:
                        left_action = swipe_action
        else:
            right_rotate_was_active["Left"] = False
            rotate_hold_counter["Left"] = 0
            rotate_hold_type["Left"]    = None

        # ── RIGHT hand gestures (TOP/BOTTOM/LEFT_ROTATE + flick + drag) ───────
        if len(right_lms) > 0:
            rotate_cmd = detect_right_hand_rotate(right_lms)
            if rotate_cmd in ("TOP_ROTATE", "BOTTOM_ROTATE", "LEFT_ROTATE"):
                if rotate_hold_type["Right"] == rotate_cmd:
                    rotate_hold_counter["Right"] += 1
                else:
                    rotate_hold_type["Right"]    = rotate_cmd
                    rotate_hold_counter["Right"] = 1

                if rotate_hold_counter["Right"] >= ROTATE_HOLD_FRAMES:
                    was_active = {
                        "TOP_ROTATE":    top_rotate_was_active["Right"],
                        "BOTTOM_ROTATE": bottom_rotate_was_active["Right"],
                        "LEFT_ROTATE":   left_rotate_was_active["Right"],
                    }
                    if not was_active[rotate_cmd]:
                        right_action = rotate_cmd
                        top_rotate_was_active["Right"]    = (rotate_cmd == "TOP_ROTATE")
                        bottom_rotate_was_active["Right"] = (rotate_cmd == "BOTTOM_ROTATE")
                        left_rotate_was_active["Right"]   = (rotate_cmd == "LEFT_ROTATE")
            else:
                top_rotate_was_active["Right"]    = False
                bottom_rotate_was_active["Right"] = False
                left_rotate_was_active["Right"]   = False
                rotate_hold_counter["Right"] = 0
                rotate_hold_type["Right"]    = None
                flick_action = right_flick_gesture.detect(right_lms, img)
                if flick_action:
                    right_action = flick_action
                if not right_action and not right_flick_gesture.is_active():
                    drag_action = right_drag_gesture.detect(right_lms, img)
                    if drag_action:
                        right_action = drag_action
        else:
            top_rotate_was_active["Right"]    = False
            bottom_rotate_was_active["Right"] = False
            left_rotate_was_active["Right"]   = False
            rotate_hold_counter["Right"] = 0
            rotate_hold_type["Right"]    = None

        # ── ZOOM gesture ──────────────────────────────────────────────────────
        any_rotate_active = (
            top_rotate_was_active["Right"]    or
            bottom_rotate_was_active["Right"] or
            left_rotate_was_active["Right"]   or
            right_rotate_was_active["Left"]
        )

        if len(left_lms) > 0 and len(right_lms) > 0 and not any_rotate_active:
            left_index_extended = left_lms[INDEX_FINGER][2] < left_lms[INDEX_POINT][2] - 20
            left_middle_curled  = left_lms[MIDDLE_FINGER][2] > left_lms[MIDDLE_POINT][2] - 10
            left_ring_curled    = left_lms[RING_FINGER][2]   > left_lms[RING_POINT][2]   - 10
            left_pinky_curled   = left_lms[PINKY_FINGER][2]  > left_lms[PINKY_POINT][2]  - 10
            left_valid_pose = (left_index_extended and left_middle_curled and
                               left_ring_curled and left_pinky_curled)

            right_index_extended = right_lms[INDEX_FINGER][2] < right_lms[INDEX_POINT][2] - 20
            right_middle_curled  = right_lms[MIDDLE_FINGER][2] > right_lms[MIDDLE_POINT][2] - 10
            right_ring_curled    = right_lms[RING_FINGER][2]   > right_lms[RING_POINT][2]   - 10
            right_pinky_curled   = right_lms[PINKY_FINGER][2]  > right_lms[PINKY_POINT][2]  - 10
            right_valid_pose = (right_index_extended and right_middle_curled and
                                right_ring_curled and right_pinky_curled)

            if left_valid_pose and right_valid_pose:
                left_index_x  = left_lms[INDEX_FINGER][1]
                left_index_y  = left_lms[INDEX_FINGER][2]
                right_index_x = right_lms[INDEX_FINGER][1]
                right_index_y = right_lms[INDEX_FINGER][2]

                distance = math.sqrt(
                    (right_index_x - left_index_x) ** 2 +
                    (right_index_y - left_index_y) ** 2
                ) / w_full

                if distance < 0.15:
                    zoom_action = "ZOOM OUT"
                elif distance > 0.35:
                    zoom_action = "ZOOM IN"

        # ── Send gesture commands ─────────────────────────────────────────────
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

        # ── Debug overlay ─────────────────────────────────────────────────────
        if DEBUG_MODE:
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

            for i, (hand, color) in enumerate([("Left", (255, 200, 0)), ("Right", (0, 200, 255))]):
                if rotate_hold_counter[hand] > 0:
                    cv2.putText(img,
                                f"{hand} [{rotate_hold_type[hand]}] {rotate_hold_counter[hand]}/{ROTATE_HOLD_FRAMES}",
                                (10, 60 + i * 30),
                                cv2.FONT_HERSHEY_SIMPLEX, 0.6, color, 2)

            cv2.namedWindow("Gestures", cv2.WINDOW_AUTOSIZE)
            cv2.imshow("Gestures", img)

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
    sim_sock.close()
    cap.release()
    if DEBUG_MODE:
        cv2.destroyAllWindows()

#The correct file should end at line 361, not above 400. If you see this line, means it is correct
