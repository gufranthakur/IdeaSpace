import math
from gesture_config import *
from canvas_config import *


class CanvasGestureDetector:
    """Simplified gesture detector - only DRAW and ERASE"""

    def __init__(self):
        pass

    def _get_hand_scale(self, lms):
        """Get hand size based on wrist-to-middle-mcp distance (scales with distance from camera)"""
        wrist_x, wrist_y = lms[0][1], lms[0][2]
        middle_mcp_x, middle_mcp_y = lms[MIDDLE_POINT][1], lms[MIDDLE_POINT][2]
        return math.sqrt((middle_mcp_x - wrist_x) ** 2 + (middle_mcp_y - wrist_y) ** 2)

    def detect_canvas_gestures(self, lms, img):
        """
        Detect canvas gestures:
        - Index finger only: Draw
        - Open hand (4+ fingers): Erase

        Returns: (gesture_type, x, y) or None
        """
        if len(lms) == 0:
            return None

        h, w, c = img.shape

        # Get hand scale for relative threshold
        hand_scale = self._get_hand_scale(lms)
        threshold = hand_scale * 0.3  # 30% of hand size

        # Get finger positions
        index_tip = lms[INDEX_FINGER][2]
        index_base = lms[INDEX_POINT][2]

        middle_tip = lms[MIDDLE_FINGER][2]
        middle_base = lms[MIDDLE_POINT][2]

        ring_tip = lms[RING_FINGER][2]
        ring_base = lms[RING_POINT][2]

        pinky_tip = lms[PINKY_FINGER][2]
        pinky_base = lms[PINKY_POINT][2]

        # Check which fingers are up (using relative threshold)
        index_up = index_tip < index_base - threshold
        middle_up = middle_tip < middle_base - threshold
        ring_up = ring_tip < ring_base + (threshold * 0.5)
        pinky_up = pinky_tip < pinky_base + (threshold * 0.5)

        fingers_up_count = sum([index_up, middle_up, ring_up, pinky_up])

        # Get index finger position for drawing
        index_x, index_y = lms[INDEX_FINGER][1], lms[INDEX_FINGER][2]

        # DRAW: Only index finger up
        if fingers_up_count == 1 and index_up:
            return ("DRAW", index_x, index_y)

        # ERASE: Open hand (4+ fingers)
        elif fingers_up_count >= 4:
            return ("ERASE", index_x, index_y)

        return None
