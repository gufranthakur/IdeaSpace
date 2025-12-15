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
        self.DRAG_THRESHOLD = 0.04  # Reduced from 0.06

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
                self.fist_confidence += 1
                if self.fist_confidence >= 2:
                    self.drag_active = True
                    self.initial_fist_x = current_x
            else:
                self.fist_confidence = 0
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

    def _is_hand_open(self, lms, img):
        """All fingers extended"""
        h, w, c = img.shape

        # Check thumb extension
        thumb_dist = math.dist(
            (lms[THUMB][1], lms[THUMB][2]),
            (lms[0][1], lms[0][2])
        ) / w

        if thumb_dist < 0.10:
            return False

        # Check if all 4 fingers are extended
        palm = (lms[0][1], lms[0][2])
        finger_dists = []
        for tip in [INDEX_FINGER, MIDDLE_FINGER, RING_FINGER, PINKY_FINGER]:
            dist = math.dist((lms[tip][1], lms[tip][2]), palm) / w
            finger_dists.append(dist)

        open_fingers = sum(1 for d in finger_dists if d > 0.15)

        if open_fingers < 4:
            return False

        # Just check fingers are pointing generally upward (not down)
        fingers_up = sum(
            1 for tip, mcp in [(INDEX_FINGER, INDEX_POINT), (MIDDLE_FINGER, MIDDLE_POINT),
                               (RING_FINGER, RING_POINT), (PINKY_FINGER, PINKY_POINT)]
            if lms[tip][2] < lms[mcp][2]
        )

        return fingers_up >= 3

    def _is_fist(self, lms, img):
        """All fingers closed - MUCH STRICTER"""
        h, w, c = img.shape

        palm = (lms[0][1], lms[0][2])

        # Fingers must be VERY close to palm for fist
        closed_fingers = sum(
            1 for tip in [INDEX_FINGER, MIDDLE_FINGER, RING_FINGER, PINKY_FINGER]
            if math.dist((lms[tip][1], lms[tip][2]), palm) / w < 0.15  # Much stricter from 0.22
        )

        # Only consider it a fist if BOTH metrics agree
        if closed_fingers < 4:  # All 4 fingers must be close to palm
            return False

        # Additional check: fingers must be actually curled (very short)
        fingers_very_curled = sum(
            1 for tip, mcp in [(INDEX_FINGER, INDEX_POINT), (MIDDLE_FINGER, MIDDLE_POINT),
                               (RING_FINGER, RING_POINT), (PINKY_FINGER, PINKY_POINT)]
            if math.dist((lms[tip][1], lms[tip][2]), (lms[mcp][1], lms[mcp][2])) / w < 0.08  # Much stricter from 0.16
        )

        is_fist = closed_fingers >= 4 and fingers_very_curled >= 3

        # If looks like fist, check if it's actually split gesture
        if is_fist:
            thumb_middle_dist = math.dist(
                (lms[THUMB][1], lms[THUMB][2]),
                (lms[MIDDLE_FINGER][1], lms[MIDDLE_FINGER][2])
            ) / w

            # Split = thumb-middle VERY close + other fingers extended
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
