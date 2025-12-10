import cv2
import time
import hand_tracking_module as htm
import hand_utils as utils
import server as server
from collections import deque
import numpy as np

THUMB = 4
INDEX_FINGER = 8
MIDDLE_FINGER = 12
PINKY_FINGER = 20
RING_FINGER = 16

MIDDLE_POINT = 9
INDEX_POINT = 5
RING_POINT = 13
PINKY_POINT = 17

VIEW_MODE = 2
EDIT_MODE = 3
CANVAS_MODE = 4

CURRENT_MODE = VIEW_MODE

RED = (40, 40, 240)
GREEN = (40, 240, 40)
BLUE = (240, 40, 40)

# Performance settings

TARGET_FPS = 30
FRAME_TIME = 1.0 / TARGET_FPS

# Smoothing settings
SMOOTHING_WINDOW = 5
position_history = deque(maxlen=SMOOTHING_WINDOW)

# Movement settings
STABLE_FRAMES = 2
MOVEMENT_THRESHOLD = 10
DEAD_ZONE = 3

# Swipe detection settings
SWIPE_MIN_DISTANCE = 70  # Minimum horizontal distance for swipe
SWIPE_MAX_TIME = 0.5  # Maximum time for swipe gesture (seconds)
SWIPE_MAX_VERTICAL = 50  # Maximum vertical movement allowed
SWIPE_COOLDOWN = 0.8  # Cooldown between swipes

# Command throttling
COMMAND_SEND_INTERVAL = 0.05
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

# Swipe tracking
swipe_start_pos = None
swipe_start_time = None
last_swipe_time = 0
swipe_in_progress = False

def smooth_position(current_pos):
    """Apply moving average smoothing to position"""
    position_history.append(current_pos)
    if len(position_history) < 2:
        return current_pos

    positions = np.array(position_history)
    return tuple(np.mean(positions, axis=0).astype(int))

def calculate_velocity(current_pos, prev_pos):
    """Calculate velocity for more responsive movement"""
    if prev_pos is None:
        return 0, 0
    dx = current_pos[0] - prev_pos[0]
    dy = current_pos[1] - prev_pos[1]
    return dx, dy

def get_direction_from_velocity(dx, dy):
    """Determine direction with velocity-based approach"""
    magnitude = np.sqrt(dx**2 + dy**2)

    if magnitude < DEAD_ZONE:
        return None

    angle = np.arctan2(dy, dx) * 180 / np.pi

    if -22.5 <= angle < 22.5:
        return "RIGHT"
    elif 22.5 <= angle < 67.5:
        return "BOTTOM-RIGHT"
    elif 67.5 <= angle < 112.5:
        return "BOTTOM"
    elif 112.5 <= angle < 157.5:
        return "BOTTOM-LEFT"
    elif angle >= 157.5 or angle < -157.5:
        return "LEFT"
    elif -157.5 <= angle < -112.5:
        return "TOP-LEFT"
    elif -112.5 <= angle < -67.5:
        return "TOP"
    elif -67.5 <= angle < -22.5:
        return "TOP-RIGHT"

    return None

def detect_swipe_gesture(lms):
    """Detect horizontal swipe gestures (all fingers extended)"""
    global swipe_start_pos, swipe_start_time, last_swipe_time, swipe_in_progress

    current_time = time.time()

    # Check if all fingers are extended
    index_up = lms[INDEX_FINGER][2] < lms[INDEX_POINT][2]
    middle_up = lms[MIDDLE_FINGER][2] < lms[MIDDLE_POINT][2]
    ring_up = lms[RING_FINGER][2] < lms[RING_POINT][2]
    pinky_up = lms[PINKY_FINGER][2] < lms[PINKY_POINT][2]

    all_fingers_extended = index_up and middle_up and ring_up and pinky_up

    if all_fingers_extended:
        palm_x = lms[MIDDLE_POINT][1]  # Use middle of palm for tracking
        palm_y = lms[MIDDLE_POINT][2]

        # Start tracking swipe
        if swipe_start_pos is None:
            swipe_start_pos = (palm_x, palm_y)
            swipe_start_time = current_time
            swipe_in_progress = True
            return None

        # Check if swipe is still in progress
        if swipe_in_progress:
            time_elapsed = current_time - swipe_start_time
            dx = palm_x - swipe_start_pos[0]
            dy = abs(palm_y - swipe_start_pos[1])

            # Check if gesture completed
            if time_elapsed > SWIPE_MAX_TIME:
                # Timeout - reset
                swipe_start_pos = (palm_x, palm_y)
                swipe_start_time = current_time
                return None

            # Check for valid swipe
            if abs(dx) >= SWIPE_MIN_DISTANCE and dy <= SWIPE_MAX_VERTICAL:
                # Check cooldown
                if current_time - last_swipe_time >= SWIPE_COOLDOWN:
                    last_swipe_time = current_time
                    swipe_start_pos = None
                    swipe_start_time = None
                    swipe_in_progress = False

                    if dx > 0:
                        return "SWIPED RIGHT"
                    else:
                        return "SWIPED LEFT"
    else:
        # Reset swipe tracking when hand configuration changes
        swipe_start_pos = None
        swipe_start_time = None
        swipe_in_progress = False

    return None

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

    if handedness == "Right" and len(lms) != 0:
        # Check for swipe gesture first (highest priority)
        swipe_action = detect_swipe_gesture(lms)
        if swipe_action:
            detected_action = swipe_action

        elif CURRENT_MODE == VIEW_MODE:
            # Zoom gestures
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

                    smoothed_pos = smooth_position((index_x, index_y))

                    if prev_index_point is not None:
                        dx, dy = calculate_velocity(smoothed_pos, prev_index_point)
                        direction = get_direction_from_velocity(dx, dy)

                        if direction:
                            detected_action = f"CAMERA LOOK {direction}"

                    prev_index_point = smoothed_pos
            else:
                camera_active = False
                prev_index_point = None
                gesture_hold_frames = 0
                position_history.clear()

            # Zoom detection (only when not in camera mode)
            if not camera_active:
                if utils.action_between_threshold(zoom_out_action, 0, 30) and \
                   utils.action_between_threshold(zoom_in_action, 60, 100):
                    zoom_out_frames += 1
                    if zoom_out_frames >= STABLE_FRAMES:
                        detected_action = "ZOOMED OUT"
                else:
                    zoom_out_frames = 0

                if utils.action_between_threshold(zoom_in_action, 180, 300):
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
