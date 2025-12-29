import cv2
import hand_tracking_module as htm
import server as server

from maths import *
from swipe_utils import *
from remove_gesture import RemoveGesture
from drag_gesture import DragGesture
from split_gesture import SplitGesture
from zoom_gesture import ZoomGesture
from canvas_drawing import CanvasDrawing
from canvas_gestures import CanvasGestureDetector

CURRENT_MODE = VIEW_MODE

remove_gesture = RemoveGesture()
drag_gesture = DragGesture()
split_gesture = SplitGesture()
zoom_gesture = ZoomGesture()
canvas_gesture_detector = CanvasGestureDetector()

position_history = deque(maxlen=SMOOTHING_WINDOW)
last_command_time = 0
last_sent_command = None

cap = cv2.VideoCapture(0)
cap.set(cv2.CAP_PROP_BUFFERSIZE, 1)
cap.set(cv2.CAP_PROP_FPS, TARGET_FPS)
cap.set(cv2.CAP_PROP_FRAME_WIDTH, 640)
cap.set(cv2.CAP_PROP_FRAME_HEIGHT, 480)
cap.set(cv2.CAP_PROP_AUTOFOCUS, 0)  # Disable autofocus for stability

# VIEW MODE: 2 hands, CANVAS MODE: 1 hand for speed
detector_view = htm.handDetector(maxHands=2, detectionConfidence=0.5, trackConfidence=0.5)
detector_canvas = htm.handDetector(maxHands=1, detectionConfidence=0.4, trackConfidence=0.4)

current_active_action = None
camera_active = False
prev_index_point = None
gesture_hold_frames = 0
canvas = None

PROCESS_WIDTH = 640  # Full resolution to prevent corner cutoff
PROCESS_HEIGHT = 480  # Full resolution


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

while True:
    current_time = time.time()

    success, img = cap.read()
    if not success:
        continue

    img = cv2.flip(img, 1)
    h_full, w_full = img.shape[:2]

    # Use appropriate detector based on mode
    detector = detector_canvas if CURRENT_MODE == CANVAS_MODE else detector_view

    # Process at lower resolution
    img_small = cv2.resize(img, (PROCESS_WIDTH, PROCESS_HEIGHT))
    img_small = detector.find_hands(img_small, False)

    if canvas is None:
        canvas = CanvasDrawing(img.shape)

    left_lms = []
    right_lms = []

    scale_x = w_full / PROCESS_WIDTH
    scale_y = h_full / PROCESS_HEIGHT

    if detector.results and detector.results.multi_hand_landmarks:
        for hand_idx in range(len(detector.results.multi_hand_landmarks)):
            handedness = detector.get_handedness(hand_idx)
            lms_small = detector.find_position(img_small, hand_idx, draw=False)

            lms = [[l[0], int(l[1] * scale_x), int(l[2] * scale_y)] for l in lms_small]

            # Draw landmarks on full image for both modes
            if len(lms) > 0:
                for lm in lms:
                    cv2.circle(img, (lm[1], lm[2]), 5, (255, 0, 255), -1)

            if handedness == "Left":
                left_lms = lms
            elif handedness == "Right":
                right_lms = lms

    detected_action = None

    # MODE SWITCHING
    key = cv2.waitKey(1) & 0xFF
    if key == ord('c'):
        CURRENT_MODE = CANVAS_MODE
    elif key == ord('v'):
        CURRENT_MODE = VIEW_MODE

    mode_text = "CANVAS" if CURRENT_MODE == CANVAS_MODE else "VIEW"
    cv2.putText(img, f"Mode: {mode_text}", (10, 30),
                cv2.FONT_HERSHEY_SIMPLEX, 0.7, (255, 255, 0), 2)

    if CURRENT_MODE == VIEW_MODE:
        # ========== VIEW MODE (2 HANDS) ==========

        if len(left_lms) > 0 and len(right_lms) > 0 and not detected_action:
            zoom_action = zoom_gesture.detect(left_lms, right_lms, img)
            if zoom_action:
                detected_action = zoom_action

        if len(left_lms) > 0 and not detected_action and not zoom_gesture.is_active():
            remove_action = remove_gesture.detect(left_lms, img)
            if remove_action:
                detected_action = remove_action
            elif not remove_gesture.is_active():
                swipe_action = detect_swipe_gesture(left_lms, img)
                if swipe_action:
                    detected_action = swipe_action

        if len(right_lms) > 0 and not detected_action and not zoom_gesture.is_active():
            split_action = split_gesture.detect(right_lms, img)
            if split_action:
                detected_action = split_action

            if not detected_action and not split_gesture.is_active():
                drag_action = drag_gesture.detect(right_lms, img)
                if drag_action:
                    detected_action = drag_action
                elif not drag_gesture.is_active():
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

        if detected_action:
            current_active_action = detected_action
            send_command_throttled(current_active_action)
            cv2.putText(img, current_active_action, (50, 60),
                        cv2.FONT_HERSHEY_SIMPLEX, 0.7, (0, 255, 0), 2)
        else:
            if current_active_action is not None:
                send_command_throttled("NULL")
                current_active_action = None

    elif CURRENT_MODE == CANVAS_MODE:
        # ========== CANVAS MODE (1 HAND ONLY - RIGHT) ==========

        if canvas.ui_just_closed:
            canvas.ui_closed_frames += 1
            if canvas.ui_closed_frames >= canvas.UI_GRACE_FRAMES:
                canvas.ui_just_closed = False

        pointer_pos = None
        ui_is_open = canvas.unified_ui.is_active

        # ONLY process right hand in canvas mode
        if len(right_lms) > 0:
            # Check split gesture first - closes UI
            split_action = split_gesture.detect(right_lms, img)
            if split_action == "SPLIT":
                if ui_is_open:
                    canvas.close_unified_ui()
                    detected_action = "CLOSED UI"
                else:
                    canvas.clear_canvas()
                    detected_action = "CLEAR"

            elif not split_gesture.is_active():
                # Canvas gestures
                gesture_result = canvas_gesture_detector.detect_canvas_gestures(right_lms, img)

                if gesture_result:
                    gesture_type, x, y = gesture_result

                    # DEBUG: Show what gesture is detected
                    cv2.putText(img, f"Gesture: {gesture_type}", (10, 90),
                                cv2.FONT_HERSHEY_SIMPLEX, 0.5, (255, 0, 255), 2)

                    if gesture_type == "OPEN_UI":
                        # Open unified UI with thumbs up
                        if not ui_is_open:
                            canvas.open_unified_ui()
                            detected_action = "UI OPENED"
                        # When UI is open, don't use thumbs up position

                    elif gesture_type == "DRAW":
                        if ui_is_open:
                            # When UI is open, index finger selects color/size
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
                            # When UI is open, open hand also selects (any finger)
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
                        # Thumbs up release does nothing - only split closes UI
                        pass
                else:
                    canvas.reset_drawing()
        else:
            # No hand - don't auto-close UI anymore
            canvas.reset_drawing()

        img = canvas.get_overlay(img)
        img = canvas.draw_ui(img, pointer_pos)

        if detected_action:
            cv2.putText(img, detected_action, (50, 60),
                        cv2.FONT_HERSHEY_SIMPLEX, 0.7, (0, 255, 255), 2)

    # FPS
    fps = 1.0 / (time.time() - prev_time)
    prev_time = time.time()
    cv2.putText(img, f"FPS: {int(fps)}", (img.shape[1] - 100, 30),
                cv2.FONT_HERSHEY_SIMPLEX, 0.6, (0, 255, 0), 2)

    cv2.imshow("Image", img)

    elapsed = time.time() - current_time
    if elapsed < FRAME_TIME:
        time.sleep(FRAME_TIME - elapsed)

    if key == ord('q'):
        break

send_command_throttled("NULL")
cap.release()
cv2.destroyAllWindows()
