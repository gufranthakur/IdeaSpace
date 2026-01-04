import math
import sys
sys.path.append('..')

from gesture_config import *


class CanvasGestureDetector:
    def __init__(self):
        self.last_gesture = None
        self.thumbs_up_hold_frames = 0
        self.last_thumbs_up_state = False

    def detect_canvas_gestures(self, lms, img):
        """
        Detect canvas gestures:
        - Thumbs up: Opens unified color/brush UI
        - Index finger only: Draw
        - Open hand (4+ fingers): Erase
        """
        if len(lms) == 0:
            if self.last_thumbs_up_state:
                self.last_thumbs_up_state = False
                return ("THUMBS_UP_RELEASED", 0, 0)
            self._reset()
            return None

        h, w, c = img.shape

        thumb_tip_x, thumb_tip_y = lms[THUMB][1], lms[THUMB][2]
        thumb_base_x, thumb_base_y = lms[2][1], lms[2][2]

        index_tip = lms[INDEX_FINGER][2]
        index_base = lms[INDEX_POINT][2]

        middle_tip = lms[MIDDLE_FINGER][2]
        middle_base = lms[MIDDLE_POINT][2]

        ring_tip = lms[RING_FINGER][2]
        ring_base = lms[RING_POINT][2]

        pinky_tip = lms[PINKY_FINGER][2]
        pinky_base = lms[PINKY_POINT][2]

        wrist_x, wrist_y = lms[0][1], lms[0][2]
        thumb_dist_from_wrist = math.sqrt((thumb_tip_x - wrist_x) ** 2 + (thumb_tip_y - wrist_y) ** 2)

        palm_center_x = (lms[0][1] + lms[9][1]) // 2
        palm_center_y = (lms[0][2] + lms[9][2]) // 2
        thumb_dist_from_palm = math.sqrt((thumb_tip_x - palm_center_x) ** 2 + (thumb_tip_y - palm_center_y) ** 2)

        thumb_extended = thumb_dist_from_palm > 80

        index_curled = math.sqrt(
            (lms[INDEX_FINGER][1] - palm_center_x) ** 2 + (lms[INDEX_FINGER][2] - palm_center_y) ** 2) < 80
        middle_curled = math.sqrt(
            (lms[MIDDLE_FINGER][1] - palm_center_x) ** 2 + (lms[MIDDLE_FINGER][2] - palm_center_y) ** 2) < 80
        ring_curled = math.sqrt(
            (lms[RING_FINGER][1] - palm_center_x) ** 2 + (lms[RING_FINGER][2] - palm_center_y) ** 2) < 80
        pinky_curled = math.sqrt(
            (lms[PINKY_FINGER][1] - palm_center_x) ** 2 + (lms[PINKY_FINGER][2] - palm_center_y) ** 2) < 80

        all_fingers_curled = index_curled and middle_curled and ring_curled and pinky_curled
        thumb_really_extended = thumb_dist_from_palm > 100

        index_x, index_y = lms[INDEX_FINGER][1], lms[INDEX_FINGER][2]

        if thumb_really_extended and all_fingers_curled:
            self.thumbs_up_hold_frames += 1
            self.last_thumbs_up_state = True
            if self.thumbs_up_hold_frames >= 5:
                return ("OPEN_UI", thumb_tip_x, thumb_tip_y)
            return None
        else:
            self.thumbs_up_hold_frames = 0

        index_up = index_tip < index_base - 40
        middle_up = middle_tip < middle_base - 40
        ring_up = ring_tip < ring_base + 20
        pinky_up = pinky_tip < pinky_base + 20

        fingers_up_count = sum([index_up, middle_up, ring_up, pinky_up])

        if fingers_up_count == 1 and index_up:
            if self.last_thumbs_up_state:
                self.last_thumbs_up_state = False
                return ("THUMBS_UP_RELEASED", index_x, index_y)
            self._reset()
            return ("DRAW", index_x, index_y)

        elif fingers_up_count >= 4:
            if self.last_thumbs_up_state:
                self.last_thumbs_up_state = False
                return ("THUMBS_UP_RELEASED", index_x, index_y)
            self._reset()
            return ("ERASE", index_x, index_y)

        else:
            if self.last_thumbs_up_state:
                self.last_thumbs_up_state = False
                return ("THUMBS_UP_RELEASED", 0, 0)
            self._reset()
            return None

    def _reset(self):
        self.last_gesture = None
        self.thumbs_up_hold_frames = 0

    def reset(self):
        self._reset()
        self.last_thumbs_up_state = False
