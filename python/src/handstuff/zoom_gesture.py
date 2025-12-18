import math
from gesture_config import *


class ZoomGesture:
    def __init__(self):
        self.zoom_active = False
        self.previous_distance = None
        self.stable_frames = 0
        self.distance_history = []
        self.STABLE_FRAMES_REQUIRED = 2
        self.MIN_HAND_DISTANCE = 0.01  # Very small - 1% of screen
        self.HISTORY_SIZE = 3
        self.MIN_CHANGE = 0.005  # 0.5% minimum change - more sensitive

    def detect(self, left_lms, right_lms, img):
        """Detect zoom: moving apart=zoom in, moving together=zoom out"""

        if len(left_lms) == 0 or len(right_lms) == 0:
            self._reset()
            return None

        h, w, c = img.shape

        # Check if both hands are horizontal
        if not (self._is_hand_horizontal(left_lms) and self._is_hand_horizontal(right_lms)):
            self._reset()
            return None

        # Calculate distance between hands
        left_x = left_lms[MIDDLE_FINGER][1]
        right_x = right_lms[MIDDLE_FINGER][1]
        left_y = left_lms[MIDDLE_FINGER][2]
        right_y = right_lms[MIDDLE_FINGER][2]

        current_distance = math.sqrt((right_x - left_x) ** 2 + (right_y - left_y) ** 2) / w

        # Remove minimum distance requirement - detect at any distance
        # if current_distance < self.MIN_HAND_DISTANCE:
        #     self._reset()
        #     return None

        # Initialize tracking
        if not self.zoom_active:
            self.stable_frames += 1
            if self.stable_frames >= self.STABLE_FRAMES_REQUIRED:
                self.zoom_active = True
                self.distance_history = [current_distance]
            return None

        # Track distance over time
        self.distance_history.append(current_distance)
        if len(self.distance_history) > self.HISTORY_SIZE:
            self.distance_history.pop(0)

        # Need enough history
        if len(self.distance_history) < self.HISTORY_SIZE:
            return None

        # Calculate trend
        oldest = self.distance_history[0]
        newest = self.distance_history[-1]
        change = newest - oldest
        change_ratio = abs(change / oldest)

        # Detect zoom direction
        if change_ratio >= self.MIN_CHANGE:
            if change > 0:
                return "ZOOM IN"
            else:
                return "ZOOM OUT"

        return None

    def _is_hand_horizontal(self, lms):
        """Check if hand is horizontal"""
        wrist_x, wrist_y = lms[0][1], lms[0][2]
        middle_tip_x, middle_tip_y = lms[MIDDLE_FINGER][1], lms[MIDDLE_FINGER][2]

        dx = abs(middle_tip_x - wrist_x)
        dy = abs(middle_tip_y - wrist_y)

        if dx <= dy:
            return False

        # Check at least 3 fingers extended
        index_extended = lms[INDEX_FINGER][2] < lms[INDEX_POINT][2] + 25
        middle_extended = lms[MIDDLE_FINGER][2] < lms[MIDDLE_POINT][2] + 25
        ring_extended = lms[RING_FINGER][2] < lms[RING_POINT][2] + 25
        pinky_extended = lms[PINKY_FINGER][2] < lms[PINKY_POINT][2] + 25

        extended_count = sum([index_extended, middle_extended, ring_extended, pinky_extended])
        return extended_count >= 3

    def _reset(self):
        self.zoom_active = False
        self.previous_distance = None
        self.stable_frames = 0
        self.distance_history = []

    def reset(self):
        self._reset()

    def is_active(self):
        return self.zoom_active
