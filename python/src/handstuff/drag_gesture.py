import math
from gesture_config import *


class DragGesture:
    def __init__(self):
        self.drag_active = False
        self.cooldown = 0
        self.initial_fist_x = None
        self.idle_frames = 0
        self.MAX_IDLE_FRAMES = 60
        self.fist_confidence = 0
        self.DRAG_THRESHOLD = 0.025  # Reduced from 0.04 for easier triggering

    def is_active(self):
        return self.drag_active

    def detect(self, lms, img):
        if self.cooldown > 0:
            self.cooldown -= 1
            return None

        if len(lms) == 0:
            return None

        h, w, c = img.shape
        current_x = lms[MIDDLE_FINGER][1] / w

        # Check fist
        hand_fist = self._is_fist(lms, img)

        # Reset idle counter
        if hand_fist:
            self.idle_frames = 0
        else:
            self.idle_frames += 1
            if self.idle_frames > self.MAX_IDLE_FRAMES:
                self._reset()
            return None

        # Detect fist and start tracking
        if not self.drag_active:
            if hand_fist:
                # Start tracking immediately when fist is detected
                self.drag_active = True
                self.initial_fist_x = current_x
            return None

        # Detect leftward movement with fist
        if self.drag_active and self.initial_fist_x is not None:
            delta_x = current_x - self.initial_fist_x

            if delta_x < -self.DRAG_THRESHOLD:
                self._reset()
                self.cooldown = 5
                return "DRAG"

            # Cancel if hand opens
            if not hand_fist:
                self._reset()
                return None

            # Only reset if moving too far right
            if delta_x > 0.08:
                self._reset()

        return None

    def _is_fist(self, lms, img):
        """All fingers closed - reject very horizontal hands"""
        h, w, c = img.shape

        # REJECT ONLY VERY HORIZONTAL HANDS - be more lenient
        wrist_x, wrist_y = lms[0][1], lms[0][2]
        middle_tip_x, middle_tip_y = lms[MIDDLE_FINGER][1], lms[MIDDLE_FINGER][2]
        dx = abs(middle_tip_x - wrist_x)
        dy = abs(middle_tip_y - wrist_y)

        # Only reject if hand is very horizontal (pointing strongly sideways)
        # Changed from 0.8 to 1.2 - allows more diagonal orientations
        if dx > dy * 1.2:
            return False

        palm = (lms[0][1], lms[0][2])

        # Fingers must be close to palm for fist (slightly relaxed from 0.15)
        closed_fingers = sum(
            1 for tip in [INDEX_FINGER, MIDDLE_FINGER, RING_FINGER, PINKY_FINGER]
            if math.dist((lms[tip][1], lms[tip][2]), palm) / w < 0.17
        )

        if closed_fingers < 4:
            return False

        # Additional check: fingers must be curled (slightly relaxed from 0.08)
        fingers_very_curled = sum(
            1 for tip, mcp in [(INDEX_FINGER, INDEX_POINT), (MIDDLE_FINGER, MIDDLE_POINT),
                               (RING_FINGER, RING_POINT), (PINKY_FINGER, PINKY_POINT)]
            if math.dist((lms[tip][1], lms[tip][2]), (lms[mcp][1], lms[mcp][2])) / w < 0.10
        )

        is_fist = closed_fingers >= 4 and fingers_very_curled >= 3

        # Check if it's actually split gesture
        if is_fist:
            thumb_middle_dist = math.dist(
                (lms[THUMB][1], lms[THUMB][2]),
                (lms[MIDDLE_FINGER][1], lms[MIDDLE_FINGER][2])
            ) / w

            index_extended = lms[INDEX_FINGER][2] < lms[INDEX_POINT][2] - 10
            ring_extended = lms[RING_FINGER][2] < lms[RING_POINT][2] - 10
            pinky_extended = lms[PINKY_FINGER][2] < lms[PINKY_POINT][2] - 10

            if thumb_middle_dist < 0.08 and sum([index_extended, ring_extended, pinky_extended]) >= 2:
                return False

        return is_fist

    def _reset(self):
        self.drag_active = False
        self.initial_fist_x = None
        self.idle_frames = 0
        self.fist_confidence = 0

    def reset(self):
        self._reset()
        self.cooldown = 0
