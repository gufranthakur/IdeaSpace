import math
from gesture_config import *


class ZoomGesture:
    def __init__(self):
        # Two-hand zoom state
        self.zoom_active = False
        self.previous_distance = None
        self.stable_frames = 0
        self.distance_history = []
        self.STABLE_FRAMES_REQUIRED = 2
        self.MIN_HAND_DISTANCE = 0.01  # Very small - 1% of screen
        self.HISTORY_SIZE = 3
        self.MIN_CHANGE = 0.005  # 0.5% minimum change - more sensitive

        # Pinch zoom state (single hand - thumb + middle finger)
        self.pinch_active = False
        self.pinch_stable_frames = 0
        self.PINCH_STABLE_FRAMES = 3
        self.PINCH_CLOSE_THRESHOLD = 0.07   # Below this = ZOOM OUT (fingers close)
        self.PINCH_OPEN_THRESHOLD = 0.14    # Above this = ZOOM IN (fingers far apart)
        self.last_pinch_action = None
        self.pinch_cooldown = 0

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

    def detect_pinch_zoom(self, lms, img):
        """
        Detect single-hand pinch zoom using thumb and INDEX finger.
        Middle, ring, pinky must be closed.
        Based on ABSOLUTE distance:
        - Thumb and index CLOSE together = ZOOM OUT
        - Thumb and index FAR apart = ZOOM IN
        """
        if len(lms) == 0:
            self._reset_pinch()
            return None

        # Handle cooldown
        if self.pinch_cooldown > 0:
            self.pinch_cooldown -= 1
            return None

        h, w, c = img.shape

        # Check if this is a valid pinch gesture pose
        if not self._is_pinch_pose(lms):
            self._reset_pinch()
            return None

        # Calculate distance between thumb and INDEX finger
        thumb_x, thumb_y = lms[THUMB][1], lms[THUMB][2]
        index_x, index_y = lms[INDEX_FINGER][1], lms[INDEX_FINGER][2]

        current_distance = math.sqrt((index_x - thumb_x) ** 2 + (index_y - thumb_y) ** 2) / w

        # Need stable frames before triggering
        self.pinch_stable_frames += 1
        if self.pinch_stable_frames < self.PINCH_STABLE_FRAMES:
            return None

        self.pinch_active = True

        # Determine zoom based on absolute distance
        if current_distance < self.PINCH_CLOSE_THRESHOLD:
            # Fingers close together = ZOOM OUT
            if self.last_pinch_action != "ZOOM OUT":
                self.last_pinch_action = "ZOOM OUT"
            return "ZOOM OUT"
        
        elif current_distance > self.PINCH_OPEN_THRESHOLD:
            # Fingers far apart = ZOOM IN
            if self.last_pinch_action != "ZOOM IN":
                self.last_pinch_action = "ZOOM IN"
            return "ZOOM IN"

        # In the middle zone - no zoom
        return None

    def _is_pinch_pose(self, lms):
        """
        Check if hand is in a valid pinch pose:
        - Thumb and INDEX finger are the active pinch fingers
        - Middle, Ring, and Pinky must be CLOSED/curled
        - Thumb and Index must be CLOSE ENOUGH to be a pinch (not pointing)
        """
        if len(lms) < 21:
            return False

        # Check that middle, ring, pinky are CLOSED (tip below/near MCP joint)
        middle_tip_y = lms[MIDDLE_FINGER][2]
        middle_mcp_y = lms[MIDDLE_POINT][2]
        ring_tip_y = lms[RING_FINGER][2]
        ring_mcp_y = lms[RING_POINT][2]
        pinky_tip_y = lms[PINKY_FINGER][2]
        pinky_mcp_y = lms[PINKY_POINT][2]

        # Fingers are closed if tip is below or near the MCP joint (not extended up)
        middle_closed = middle_tip_y > middle_mcp_y - 20
        ring_closed = ring_tip_y > ring_mcp_y - 20
        pinky_closed = pinky_tip_y > pinky_mcp_y - 20

        # All three must be closed
        if not (middle_closed and ring_closed and pinky_closed):
            return False

        # CRITICAL: Check that thumb and index are oriented toward each other (pinching)
        # Not just index pointing with thumb resting
        thumb_tip_x, thumb_tip_y = lms[THUMB][1], lms[THUMB][2]
        index_tip_x, index_tip_y = lms[INDEX_FINGER][1], lms[INDEX_FINGER][2]
        index_mcp_x, index_mcp_y = lms[INDEX_POINT][1], lms[INDEX_POINT][2]
        
        # Calculate distance between thumb and index tips
        thumb_index_dist = math.sqrt((thumb_tip_x - index_tip_x) ** 2 + (thumb_tip_y - index_tip_y) ** 2)
        
        # Calculate how extended the index finger is (distance from MCP to tip)
        index_extension = math.sqrt((index_tip_x - index_mcp_x) ** 2 + (index_tip_y - index_mcp_y) ** 2)
        
        # If index is very extended AND thumb is far from index tip, it's a POINTING gesture, not pinch
        # Pinch gesture: thumb and index tips should be within reasonable distance
        MAX_PINCH_DISTANCE = 150  # pixels - if further than this, it's pointing not pinching
        
        if thumb_index_dist > MAX_PINCH_DISTANCE:
            return False
        
        # Also check: for pointing, index points away from thumb
        # For pinch, thumb moves toward index
        # Check if thumb is extended toward index (not resting at side)
        thumb_mcp_x, thumb_mcp_y = lms[2][1], lms[2][2]
        thumb_extension = math.sqrt((thumb_tip_x - thumb_mcp_x) ** 2 + (thumb_tip_y - thumb_mcp_y) ** 2)
        
        # Thumb should be somewhat extended for a pinch gesture
        MIN_THUMB_EXTENSION = 30  # pixels
        if thumb_extension < MIN_THUMB_EXTENSION:
            return False

        return True

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

    def _reset_pinch(self):
        self.pinch_active = False
        self.pinch_stable_frames = 0
        self.last_pinch_action = None

    def reset(self):
        self._reset()
        self._reset_pinch()

    def is_active(self):
        return self.zoom_active or self.pinch_active

    def is_pinch_active(self):
        return self.pinch_active