import math
import threading
import time

from gesture_config import *

# Pinch Zoom settings
PINCH_STABLE_FRAMES = 20
PINCH_CLOSE_THRESHOLD = 0.07
PINCH_OPEN_THRESHOLD = 0.14
PINCH_MAX_DISTANCE = 150
PINCH_MIN_THUMB_EXTENSION = 30


class ZoomGesture:
    def __init__(self):
        self.pinch_active = False
        self.pinch_stable_frames = 0
        self.last_pinch_action = None
        self.pinch_cooldown = 0

    def detect_pinch_zoom(self, lms, img):
        """
        Detect single-hand pinch zoom using thumb and INDEX finger.
        Middle, ring, pinky must be closed.
        """
        if len(lms) == 0:
            self._reset_pinch()
            return None

        if self.pinch_cooldown > 0:
            self.pinch_cooldown -= 1
            return None

        h, w, c = img.shape

        if not self._is_pinch_pose(lms):
            self._reset_pinch()
            return None

        thumb_x, thumb_y = lms[THUMB][1], lms[THUMB][2]
        index_x, index_y = lms[INDEX_FINGER][1], lms[INDEX_FINGER][2]

        current_distance = math.sqrt((index_x - thumb_x) ** 2 + (index_y - thumb_y) ** 2) / w

        self.pinch_stable_frames += 1
        if self.pinch_stable_frames < PINCH_STABLE_FRAMES:
            return None

        self.pinch_active = True

        if current_distance < PINCH_CLOSE_THRESHOLD:
            if self.last_pinch_action != "ZOOM OUT":
                self.last_pinch_action = "ZOOM OUT"
            return "ZOOM OUT"

        elif current_distance > PINCH_OPEN_THRESHOLD:
            if self.last_pinch_action != "ZOOM IN":
                self.last_pinch_action = "ZOOM IN"
            return "ZOOM IN"

        return None

    def _is_pinch_pose(self, lms):
        if len(lms) < 21:
            return False

        middle_tip_y = lms[MIDDLE_FINGER][2]
        middle_mcp_y = lms[MIDDLE_POINT][2]
        ring_tip_y = lms[RING_FINGER][2]
        ring_mcp_y = lms[RING_POINT][2]
        pinky_tip_y = lms[PINKY_FINGER][2]
        pinky_mcp_y = lms[PINKY_POINT][2]

        middle_closed = middle_tip_y > middle_mcp_y - 10
        ring_closed = ring_tip_y > ring_mcp_y - 10
        pinky_closed = pinky_tip_y > pinky_mcp_y - 10

        if not (middle_closed and ring_closed and pinky_closed):
            return False

        thumb_tip_x, thumb_tip_y = lms[THUMB][1], lms[THUMB][2]
        index_tip_x, index_tip_y = lms[INDEX_FINGER][1], lms[INDEX_FINGER][2]

        thumb_index_dist = math.sqrt((thumb_tip_x - index_tip_x) ** 2 + (thumb_tip_y - index_tip_y) ** 2)

        if thumb_index_dist > PINCH_MAX_DISTANCE:
            return False

        thumb_mcp_x, thumb_mcp_y = lms[2][1], lms[2][2]
        thumb_extension = math.sqrt((thumb_tip_x - thumb_mcp_x) ** 2 + (thumb_tip_y - thumb_mcp_y) ** 2)

        if thumb_extension < PINCH_MIN_THUMB_EXTENSION:
            return False

        return True

    def _reset_pinch(self):
        self.pinch_active = False
        self.pinch_stable_frames = 0
        self.last_pinch_action = None

    def reset(self):
        self._reset_pinch()

    def is_active(self):
        return self.pinch_active
