import time
import numpy as np
from gesture_config import *

# Swipe tracking
swipe_start_pos = None
swipe_start_time = None
last_swipe_time = 0
swipe_in_progress = False


def detect_swipe_gesture(lms, img):
    """Detect deliberate swipe motion for left hand - requires horizontal hand orientation"""
    global swipe_start_pos, swipe_start_time, last_swipe_time, swipe_in_progress

    current_time = time.time()
    h, w, c = img.shape

    # Check hand orientation - fingers should be horizontal, not vertical
    wrist_x, wrist_y = lms[0][1], lms[0][2]
    middle_tip_x, middle_tip_y = lms[MIDDLE_FINGER][1], lms[MIDDLE_FINGER][2]

    dx = abs(middle_tip_x - wrist_x)
    dy = abs(middle_tip_y - wrist_y)

    # Hand must be oriented horizontally (dx > dy means pointing sideways)
    hand_horizontal = dx > dy * 1.2

    if not hand_horizontal:
        swipe_start_pos = None
        swipe_start_time = None
        swipe_in_progress = False
        return None

    # Check if fingers are extended
    index_up = lms[INDEX_FINGER][2] < lms[INDEX_POINT][2] + 20
    middle_up = lms[MIDDLE_FINGER][2] < lms[MIDDLE_POINT][2] + 20
    ring_up = lms[RING_FINGER][2] < lms[RING_POINT][2] + 20
    pinky_up = lms[PINKY_FINGER][2] < lms[PINKY_POINT][2] + 20

    fingers_up_count = sum([index_up, middle_up, ring_up, pinky_up])
    hand_extended = fingers_up_count >= 3

    if hand_extended:
        palm_x = lms[MIDDLE_POINT][1]
        palm_y = lms[MIDDLE_POINT][2]

        if swipe_start_pos is None:
            # Don't start tracking if we're in cooldown
            if current_time - last_swipe_time < SWIPE_COOLDOWN:
                return None

            swipe_start_pos = (palm_x, palm_y)
            swipe_start_time = current_time
            swipe_in_progress = True
            return None

        if swipe_in_progress:
            time_elapsed = current_time - swipe_start_time
            dx = palm_x - swipe_start_pos[0]
            dy = palm_y - swipe_start_pos[1]

            # Timeout if too slow
            if time_elapsed > SWIPE_MAX_TIME:
                swipe_start_pos = None
                swipe_start_time = None
                swipe_in_progress = False
                return None

            # Calculate movement as ratio of frame width (more resolution-independent)
            dx_ratio = abs(dx) / w
            dy_ratio = abs(dy) / h

            # Require significant horizontal movement (12% of screen width)
            # and ensure it's mostly horizontal (not diagonal)
            if dx_ratio >= 0.12 and abs(dx) > abs(dy) * 1.5:
                last_swipe_time = current_time
                swipe_start_pos = None
                swipe_start_time = None
                swipe_in_progress = False

                if dx < 0:
                    return "SWIPED RIGHT"
                else:
                    return "SWIPED LEFT"
    else:
        # Reset when hand closes
        swipe_start_pos = None
        swipe_start_time = None
        swipe_in_progress = False

    return None
