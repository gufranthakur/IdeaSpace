import math
import sys
sys.path.append('..')

from modular.gesture_config import *
from modular.canvas_config import INDEX_EXTENSION_THRESHOLD, THUMB_EXTENSION_THRESHOLD, FINGER_EXTENSION_THRESHOLD, INDEX_POINTING_FORWARD_THRESHOLD


class CanvasGestureDetector:
    def __init__(self):
        self.last_gesture = None
        self.thumbs_up_hold_frames = 0
        self.last_thumbs_up_state = False

    def _is_index_pointing_forward(self, lms):
        """
        Detect if index finger is pointing forward (toward camera) in depth.
        Works regardless of tilt/rotation (left, right, up, down).
        
        Uses average 2D distance between finger joints - when pointing forward,
        joints appear compressed together in 2D space.
        """
        # Index finger landmarks
        index_tip_x, index_tip_y = lms[8][1], lms[8][2]   # Tip
        index_dip_x, index_dip_y = lms[7][1], lms[7][2]   # DIP joint
        index_pip_x, index_pip_y = lms[6][1], lms[6][2]   # PIP joint (middle)
        index_mcp_x, index_mcp_y = lms[5][1], lms[5][2]   # MCP joint (knuckle)
        
        # Calculate distances between joints in 2D
        tip_to_pip = math.sqrt((index_tip_x - index_pip_x)**2 + (index_tip_y - index_pip_y)**2)
        tip_to_mcp = math.sqrt((index_tip_x - index_mcp_x)**2 + (index_tip_y - index_mcp_y)**2)
        dip_to_mcp = math.sqrt((index_dip_x - index_mcp_x)**2 + (index_dip_y - index_mcp_y)**2)
        
        # Average distance - captures pointing forward even when tilted
        avg_joint_distance = (tip_to_pip + tip_to_mcp + dip_to_mcp) / 3
        
        return avg_joint_distance < INDEX_POINTING_FORWARD_THRESHOLD

    def detect_canvas_gestures(self, lms, img):
        """
        Detect canvas gestures:
        - Thumbs up: Opens unified color/brush UI
        - Index finger pointing FORWARD + Thumb extended: Draw
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

        palm_center_x = (lms[0][1] + lms[9][1]) // 2
        palm_center_y = (lms[0][2] + lms[9][2]) // 2
        
        thumb_dist_from_palm = math.sqrt((thumb_tip_x - palm_center_x) ** 2 + (thumb_tip_y - palm_center_y) ** 2)

        # Check if thumb is extended using global threshold
        thumb_extended = thumb_dist_from_palm > THUMB_EXTENSION_THRESHOLD

        # Check if index finger is pointing forward (works with any tilt)
        index_pointing_forward = self._is_index_pointing_forward(lms)

        # Check if index finger is extended using global threshold
        index_dist_from_palm = math.sqrt(
            (lms[INDEX_FINGER][1] - palm_center_x) ** 2 + (lms[INDEX_FINGER][2] - palm_center_y) ** 2)
        index_extended = index_dist_from_palm > INDEX_EXTENSION_THRESHOLD

        index_curled = index_dist_from_palm < 80
        middle_curled = math.sqrt(
            (lms[MIDDLE_FINGER][1] - palm_center_x) ** 2 + (lms[MIDDLE_FINGER][2] - palm_center_y) ** 2) < 80
        ring_curled = math.sqrt(
            (lms[RING_FINGER][1] - palm_center_x) ** 2 + (lms[RING_FINGER][2] - palm_center_y) ** 2) < 80
        pinky_curled = math.sqrt(
            (lms[PINKY_FINGER][1] - palm_center_x) ** 2 + (lms[PINKY_FINGER][2] - palm_center_y) ** 2) < 80

        all_fingers_curled = index_curled and middle_curled and ring_curled and pinky_curled
        thumb_really_extended = thumb_dist_from_palm > 100

        index_x, index_y = lms[INDEX_FINGER][1], lms[INDEX_FINGER][2]

        # Thumbs up gesture (for opening UI)
        if thumb_really_extended and all_fingers_curled:
            self.thumbs_up_hold_frames += 1
            self.last_thumbs_up_state = True
            if self.thumbs_up_hold_frames >= 5:
                return ("OPEN_UI", thumb_tip_x, thumb_tip_y)
            return None
        else:
            self.thumbs_up_hold_frames = 0

        # Distance-based finger extension detection
        middle_dist_from_palm = math.sqrt(
            (lms[MIDDLE_FINGER][1] - palm_center_x) ** 2 + (lms[MIDDLE_FINGER][2] - palm_center_y) ** 2)
        ring_dist_from_palm = math.sqrt(
            (lms[RING_FINGER][1] - palm_center_x) ** 2 + (lms[RING_FINGER][2] - palm_center_y) ** 2)
        pinky_dist_from_palm = math.sqrt(
            (lms[PINKY_FINGER][1] - palm_center_x) ** 2 + (lms[PINKY_FINGER][2] - palm_center_y) ** 2)

        middle_extended = middle_dist_from_palm > FINGER_EXTENSION_THRESHOLD
        ring_extended = ring_dist_from_palm > FINGER_EXTENSION_THRESHOLD
        pinky_extended = pinky_dist_from_palm > FINGER_EXTENSION_THRESHOLD

        fingers_extended_count = sum([index_extended, middle_extended, ring_extended, pinky_extended])

        # DRAW: Index pointing forward + thumb extended
        if index_pointing_forward and thumb_extended:
            # Check that middle, ring, and pinky are NOT extended (to avoid confusion with erase gesture)
            if not (middle_extended and ring_extended and pinky_extended):
                if self.last_thumbs_up_state:
                    self.last_thumbs_up_state = False
                    return ("THUMBS_UP_RELEASED", index_x, index_y)
                self._reset()
                return ("DRAW", index_x, index_y)

        # Erase gesture - 4+ fingers extended
        if fingers_extended_count >= 4:
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